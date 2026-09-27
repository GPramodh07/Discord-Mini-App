package com.example.discordappmin.repository

import com.example.discordappmin.model.Group
import com.example.discordappmin.model.Message
import com.example.discordappmin.model.User
import com.example.discordappmin.network.SocketClient
import com.example.discordappmin.network.SocketPacket
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

object ChatRepository {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _onlineUsers = MutableStateFlow<List<User>>(emptyList())
    val onlineUsers: StateFlow<List<User>> = _onlineUsers.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    // Used to signal login result from packet handler back to login() coroutine
    private var loginDeferred: CompletableDeferred<Boolean>? = null

    init {
        _onlineUsers.value = listOf(
            User(id = "alice", username = "alice", isOnline = false),
            User(id = "bob", username = "bob", isOnline = false),
            User(id = "carl", username = "carl", isOnline = false),
            User(id = "don", username = "don", isOnline = false)
        )

        _groups.value = emptyList()

        scope.launch {
            SocketClient.incomingPackets.collect { packet ->
                handleIncomingPacket(packet)
            }
        }
    }

    suspend fun login(username: String, password: String = "123", host: String = "10.0.2.2", port: Int = 5000): Boolean {
        val cleanUser = username.trim().lowercase()
        val cleanPass = password.trim()

        // Attempt TCP socket connection: 
        // 1. 127.0.0.1 (ADB USB Reverse port forwarding)
        // 2. 10.8.139.239 (Real Wi-Fi network IP)
        // 3. 10.0.2.2 (Android Emulator loopback)
        var connected = SocketClient.connect("127.0.0.1", port)
        if (!connected) {
            connected = SocketClient.connect("10.8.139.239", port)
        }
        if (!connected) {
            connected = SocketClient.connect("10.0.2.2", port)
        }

        if (!connected) {
            // Offline fallback: basic local validation
            val isValid = when (cleanUser) {
                "alice" -> cleanPass == "123"
                "bob" -> cleanPass == "456"
                "carl" -> cleanPass == "789"
                "don" -> cleanPass == "101"
                else -> false
            }
            if (isValid) {
                _currentUser.value = User(id = cleanUser, username = cleanUser, isOnline = true)
                updatePresence(cleanUser, true)
            }
            return isValid
        }

        // Server is reachable — send LOGIN and wait for ACK
        val deferred = CompletableDeferred<Boolean>()
        loginDeferred = deferred

        val loginPacket = SocketPacket(
            type = "LOGIN",
            sender = cleanUser,
            content = cleanPass
        )
        SocketClient.sendPacket(loginPacket)

        // Wait up to 5 seconds for LOGIN_SUCCESS or LOGIN_FAILED/ERROR from server
        val success = withTimeoutOrNull(5000L) { deferred.await() } ?: false
        loginDeferred = null

        if (success) {
            _currentUser.value = User(id = cleanUser, username = cleanUser, isOnline = true)
            updatePresence(cleanUser, true)
        }

        return success
    }

    private fun updatePresence(username: String, isOnline: Boolean) {
        val list = _onlineUsers.value.toMutableList()
        val idx = list.indexOfFirst { it.username == username }
        if (idx != -1) {
            list[idx] = list[idx].copy(isOnline = isOnline)
        } else {
            list.add(User(id = username, username = username, isOnline = isOnline))
        }
        _onlineUsers.value = list
    }

    suspend fun createGroup(groupName: String, selectedUsernames: List<String>): Boolean {
        val me = _currentUser.value?.username ?: return false
        val allMembers = (selectedUsernames + me).distinct()

        val packet = SocketPacket(
            type = "CREATE_GROUP",
            sender = me,
            groupName = groupName,
            content = groupName,
            members = allMembers
        )

        return SocketClient.sendPacket(packet)
    }

    suspend fun sendPrivateMessage(recipient: String, text: String): Boolean {
        val me = _currentUser.value?.username ?: return false
        val packet = SocketPacket(
            type = "PRIVATE_MSG",
            sender = me,
            recipient = recipient,
            content = text
        )
        SocketClient.sendPacket(packet)

        val msg = Message(
            id = System.currentTimeMillis().toString(),
            fromUserId = me,
            fromUsername = me,
            recipientId = recipient,
            text = text,
            timestamp = System.currentTimeMillis(),
            isMine = true
        )
        _messages.value = _messages.value + msg
        return true
    }

    suspend fun sendGroupMessage(groupId: String, text: String): Boolean {
        val me = _currentUser.value?.username ?: return false
        val packet = SocketPacket(
            type = "GROUP_MSG",
            sender = me,
            groupId = groupId,
            content = text
        )
        SocketClient.sendPacket(packet)

        val msg = Message(
            id = System.currentTimeMillis().toString(),
            fromUserId = me,
            fromUsername = me,
            groupId = groupId,
            text = text,
            timestamp = System.currentTimeMillis(),
            isMine = true
        )
        _messages.value = _messages.value + msg
        return true
    }

    suspend fun deleteMessage(message: Message): Boolean {
        val me = _currentUser.value?.username ?: return false
        val packet = SocketPacket(
            type = "DELETE_MSG",
            sender = me,
            recipient = message.recipientId,
            groupId = message.groupId,
            content = message.id
        )
        SocketClient.sendPacket(packet)

        _messages.value = _messages.value.filterNot { 
            it.id == message.id || it.timestamp.toString() == message.id || (it.text == message.text && it.fromUserId == me)
        }
        return true
    }

    private fun handleIncomingPacket(packet: SocketPacket) {
        val me = _currentUser.value?.username ?: ""
        when (packet.type) {
            "ACK" -> {
                val content = packet.content ?: ""
                when {
                    content == "LOGIN_SUCCESS" -> loginDeferred?.complete(true)
                    content == "LOGIN_FAILED" || content.startsWith("Invalid") -> loginDeferred?.complete(false)
                }
            }
            "ERROR" -> {
                loginDeferred?.complete(false)
            }
            "DELETE_MSG" -> {
                val msgId = packet.content ?: return
                _messages.value = _messages.value.filterNot { 
                    it.id == msgId || it.timestamp.toString() == msgId 
                }
            }
            "PRIVATE_MSG" -> {
                val sender = packet.sender ?: "Unknown"
                val recipient = packet.recipient
                val text = packet.content ?: ""
                val msgId = packet.timestamp.toString()
                val msg = Message(
                    id = msgId,
                    fromUserId = sender,
                    fromUsername = sender,
                    recipientId = recipient,
                    text = text,
                    timestamp = packet.timestamp,
                    isMine = (sender == me)
                )
                val current = _messages.value
                val isDuplicate = current.any { 
                    it.id == msgId || (it.fromUserId == sender && it.text == text && kotlin.math.abs(it.timestamp - packet.timestamp) < 1000)
                }
                if (!isDuplicate) {
                    _messages.value = current + msg
                }
            }
            "GROUP_MSG" -> {
                val sender = packet.sender ?: "Unknown"
                val groupId = packet.groupId ?: ""
                val text = packet.content ?: ""
                val msgId = packet.timestamp.toString()
                val msg = Message(
                    id = msgId,
                    fromUserId = sender,
                    fromUsername = sender,
                    groupId = groupId,
                    text = text,
                    timestamp = packet.timestamp,
                    isMine = (sender == me)
                )
                val current = _messages.value
                val isDuplicate = current.any { 
                    it.id == msgId || (it.fromUserId == sender && it.groupId == groupId && it.text == text && kotlin.math.abs(it.timestamp - packet.timestamp) < 1000)
                }
                if (!isDuplicate) {
                    _messages.value = current + msg
                }
            }
            "CREATE_GROUP" -> {
                val gId = packet.groupId ?: return
                val gName = packet.content ?: packet.groupName ?: "New Channel"
                val members = packet.members ?: emptyList()
                val currentGroups = _groups.value.toMutableList()
                if (currentGroups.none { it.id == gId }) {
                    currentGroups.add(Group(id = gId, name = gName, memberIds = members))
                    _groups.value = currentGroups
                }
            }
            "PRESENCE" -> {
                val user = packet.sender ?: return
                val isOnline = packet.content == "ONLINE"
                updatePresence(user, isOnline)
            }
        }
    }
}
