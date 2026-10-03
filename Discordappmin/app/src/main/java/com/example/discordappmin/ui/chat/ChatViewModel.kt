package com.example.discordappmin.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.discordappmin.model.Message
import com.example.discordappmin.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val _recipientUserId = MutableStateFlow("")

    val messages: StateFlow<List<Message>> = combine(ChatRepository.messages, _recipientUserId, ChatRepository.currentUser) { allMessages, recipient, currentUser ->
        val me = currentUser?.username?.lowercase() ?: ""
        val target = recipient.lowercase()
        if (target.isBlank()) return@combine emptyList()

        allMessages.filter { msg ->
            // Strictly exclude group messages from 1:1 DM view
            if (!msg.groupId.isNullOrEmpty()) return@filter false

            val sender = msg.fromUserId.lowercase()
            val rec = msg.recipientId?.lowercase() ?: ""

            // Message from recipient to me OR message from me to recipient
            (sender == target && (rec == me || rec.isEmpty())) ||
            (sender == me && (rec == target || rec.isEmpty()))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun loadMockMessages(userId: String) {
        _recipientUserId.value = userId
    }

    fun sendMessage(text: String) {
        val target = _recipientUserId.value
        if (text.isBlank() || target.isBlank()) return
        viewModelScope.launch {
            ChatRepository.sendPrivateMessage(target, text.trim())
        }
    }

    fun deleteMessage(message: Message) {
        viewModelScope.launch {
            ChatRepository.deleteMessage(message)
        }
    }
}
