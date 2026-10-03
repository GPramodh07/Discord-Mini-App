package com.example.discordappmin.ui.groupchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.discordappmin.model.Message
import com.example.discordappmin.repository.ChatRepository
import com.example.discordappmin.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GroupChatViewModel : ViewModel() {
    private val _activeGroupId = MutableStateFlow("")

    val groupName: StateFlow<String> = combine(ChatRepository.groups, _activeGroupId) { groups, gId ->
        groups.find { it.id == gId }?.name ?: gId
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    val groupMembers: StateFlow<List<User>> = combine(ChatRepository.groups, ChatRepository.onlineUsers, _activeGroupId) { groups, users, gId ->
        val group = groups.find { it.id == gId }
        val memberIds = group?.memberIds ?: emptyList()
        memberIds.map { memberId ->
            val found = users.find { it.username.equals(memberId, ignoreCase = true) || it.id.equals(memberId, ignoreCase = true) }
            found ?: User(id = memberId, username = memberId, isOnline = false)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val messages: StateFlow<List<Message>> = combine(ChatRepository.messages, _activeGroupId) { allMessages, gId ->
        if (gId.isBlank()) return@combine emptyList()
        allMessages.filter { msg ->
            !msg.groupId.isNullOrEmpty() && msg.groupId == gId
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun loadMockGroupMessages(groupId: String) {
        _activeGroupId.value = groupId
    }

    fun sendMessage(text: String) {
        val targetGroup = _activeGroupId.value
        if (text.isBlank() || targetGroup.isBlank()) return
        viewModelScope.launch {
            ChatRepository.sendGroupMessage(targetGroup, text.trim())
        }
    }

    fun deleteMessage(message: Message) {
        viewModelScope.launch {
            ChatRepository.deleteMessage(message)
        }
    }
}
