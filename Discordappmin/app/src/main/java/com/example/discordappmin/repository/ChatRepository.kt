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

sealed class LoginResult {
    object Success : LoginResult()
    data class Error(val message: String) : LoginResult()
}

object ChatRepository {
    private val scope = CoroutineScope(Dispatchers.IO)

    // Server Host & Port Configuration
    // ----------------------------------------------------
    // Deployed Cloud VPS Server (Active)
    var SERVER_HOST: String = "3.107.163.173" 
    var SERVER_PORT: Int = 5000

    // USB ADB Reverse / Localhost (Uncomment to revert to USB config)
    // var SERVER_HOST: String = "127.0.0.1" 

    // Additional local fallbacks
    // var SERVER_HOST: String = "10.0.2.2"  // Android Emulator
    // var SERVER_HOST: String = "10.8.139.239" // Local Wi-Fi
    // ----------------------------------------------------

    // Store password for automatic reconnection (set during login)
    private var storedPassword: String = ""

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _onlineUsers = MutableStateFlow<List<User>>(emptyList())
    val onlineUsers: StateFlow<List<User>> = _onlineUsers.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    // Unread message counters for WhatsApp / Discord style unread badges
    private val _unreadDms = MutableStateFlow<Map<String, Int>>(emptyMap())
    val unreadDms: StateFlow<Map<String, Int>> = _unreadDms.asStateFlow()

    private val _unreadGroups = MutableStateFlow<Map<String, Int>>(emptyMap())
    val unreadGroups: StateFlow<Map<String, Int>> = _unreadGroups.asStateFlow()

    var activeChatUserId = MutableStateFlow("")
    var activeGroupId = MutableStateFlow("")

    // Used to signal login result from packet handler back to login() coroutine
    private var loginDeferred: CompletableDeferred<String>? = null

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

    fun setActiveChatUser(userId: String?) {
        val clean = userId?.lowercase() ?: ""
        activeChatUserId.value = clean
        if (clean.isNotEmpty()) {
            clearUnreadDm(clean)
        }
    }

    fun setActiveGroup(groupId: String?) {
        val clean = groupId ?: ""
        activeGroupId.value = clean
        if (clean.isNotEmpty()) {
            clearUnreadGroup(clean)
        }
    }

    fun clearActiveGroup() {
        activeGroupId.value = ""
    }

    fun clearUnreadDm(userId: String) {
        val clean = userId.lowercase()
        val current = _unreadDms.value.toMutableMap()
        if (current.containsKey(clean)) {
            current.remove(clean)
            _unreadDms.value = current
        }
    }

    fun clearUnreadGroup(groupId: String) {
        val current = _unreadGroups.value.toMutableMap()
        if (current.containsKey(groupId)) {
            current.remove(groupId)
            _unreadGroups.value = current
        }
    }

    suspend fun login(username: String, password: String = "123", host: String = "10.0.2.2", port: Int = 5000): LoginResult {
        val cleanUser = username.trim().lowercase()
        val cleanPass = password.trim()

        if (cleanUser.isEmpty()) {
            return LoginResult.Error("Username cannot be empty")
        }

        var connected = false
        if (SERVER_HOST.isNotBlank()) {
            connected = SocketClient.connect(SERVER_HOST, SERVER_PORT)
        }

        if (!connected) {
            return LoginResult.Error("Server is not running, contact admin")
        }

        // Server is reachable — store credentials for auto-reconnection and send LOGIN
        SocketClient.storeCredentials(cleanUser, cleanPass)

        val deferred = CompletableDeferred<String>()
        loginDeferred = deferred

        val loginPacket = SocketPacket(
            type = "LOGIN",
            sender = cleanUser,
            content = cleanPass
        )
        SocketClient.sendPacket(loginPacket)

        // Wait up to 5 seconds for LOGIN_SUCCESS or ERROR from server
        val resultStr = withTimeoutOrNull(5000L) { deferred.await() }
        loginDeferred = null

        return when {
            resultStr == "SUCCESS" -> {
                _currentUser.value = User(id = cleanUser, username = cleanUser, isOnline = true)
                storedPassword = cleanPass
                updatePresence(cleanUser, true)

                // Start Foreground Service to keep TCP connection alive in background
                try {
                    com.example.discordappmin.service.SocketForegroundService.startService(
                        com.example.discordappmin.DiscordApplication.instance,
                        cleanUser
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ChatRepository", "Failed to start SocketForegroundService", e)
                }
                LoginResult.Success
            }
            resultStr != null -> LoginResult.Error(resultStr)
            else -> LoginResult.Error("Server response timed out. Please try again.")
        }
    }

    suspend fun logout() {
        val me = _currentUser.value?.username
        if (me != null && SocketClient.isConnected) {
            val presencePacket = SocketPacket(
                type = "PRESENCE",
                sender = me,
                content = "OFFLINE"
            )
            try {
                SocketClient.sendPacket(presencePacket)
            } catch (e: Exception) {
                android.util.Log.e("ChatRepository", "Error sending OFFLINE packet on logout", e)
            }
        }

        SocketClient.storeCredentials("", "")
        SocketClient.disconnect(skipReconnect = true)

        _currentUser.value = null
        storedPassword = ""
        _messages.value = emptyList()
        _groups.value = emptyList()
        _unreadDms.value = emptyMap()
        _unreadGroups.value = emptyMap()
        activeChatUserId.value = ""
        activeGroupId.value = ""

        _onlineUsers.value = listOf(
            User(id = "alice", username = "alice", isOnline = false),
            User(id = "bob", username = "bob", isOnline = false),
            User(id = "carl", username = "carl", isOnline = false),
            User(id = "don", username = "don", isOnline = false)
        )
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

    private suspend fun ensureConnected(): Boolean {
        if (SocketClient.isConnected) return true
        val me = _currentUser.value?.username ?: return false
        var connected = false
        if (SERVER_HOST.isNotBlank()) {
            connected = SocketClient.connect(SERVER_HOST, SERVER_PORT)
        }
        // Local Fallbacks (Uncomment if needed for local debugging)
        // if (!connected) connected = SocketClient.connect("127.0.0.1", 5000)
        // if (!connected) connected = SocketClient.connect("10.8.139.239", 5000)
        // if (!connected) connected = SocketClient.connect("10.0.2.2", 5000)
        if (connected) {
            val loginPacket = SocketPacket(type = "LOGIN", sender = me, content = storedPassword)
            SocketClient.sendPacket(loginPacket)
        }
        return connected
    }

    suspend fun sendPrivateMessage(recipient: String, text: String): Boolean {
        val me = _currentUser.value?.username ?: return false
        ensureConnected()
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
        ensureConnected()
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
        ensureConnected()
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
                    content == "LOGIN_SUCCESS" -> loginDeferred?.complete("SUCCESS")
                    content == "LOGIN_FAILED" -> loginDeferred?.complete("Invalid username or password")
                    content.startsWith("LOGIN_FAILED_") -> loginDeferred?.complete(content.replace("LOGIN_FAILED_", ""))
                    else -> loginDeferred?.complete(content)
                }
            }
            "ERROR" -> {
                val errorMsg = packet.content ?: "Authentication failed"
                loginDeferred?.complete(errorMsg)
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
                    isMine = (sender.lowercase() == me.lowercase())
                )
                val current = _messages.value
                val isDuplicate = current.any { 
                    it.id == msgId || (it.fromUserId == sender && it.text == text && kotlin.math.abs(it.timestamp - packet.timestamp) < 1000)
                }
                if (!isDuplicate) {
                    _messages.value = current + msg
                    // Increment unread count if message is received from contact when not currently viewing that DM
                    if (sender.lowercase() != me.lowercase() && activeChatUserId.value.lowercase() != sender.lowercase()) {
                        val unreads = _unreadDms.value.toMutableMap()
                        val key = sender.lowercase()
                        unreads[key] = (unreads[key] ?: 0) + 1
                        _unreadDms.value = unreads
                    }
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
                    isMine = (sender.lowercase() == me.lowercase())
                )
                val current = _messages.value
                val isDuplicate = current.any { 
                    it.id == msgId || (it.fromUserId == sender && it.groupId == groupId && it.text == text && kotlin.math.abs(it.timestamp - packet.timestamp) < 1000)
                }
                if (!isDuplicate) {
                    _messages.value = current + msg
                    // Increment unread count for group if message is from another member and group is not currently open
                    if (sender.lowercase() != me.lowercase() && activeGroupId.value != groupId) {
                        val unreads = _unreadGroups.value.toMutableMap()
                        unreads[groupId] = (unreads[groupId] ?: 0) + 1
                        _unreadGroups.value = unreads
                    }
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
