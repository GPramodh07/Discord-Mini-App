package com.example.discordappmin.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.discordappmin.model.Group
import com.example.discordappmin.model.User
import com.example.discordappmin.repository.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HomeViewModel : ViewModel() {
    val currentUser: StateFlow<User?> = ChatRepository.currentUser

    val users: StateFlow<List<User>> = combine(ChatRepository.onlineUsers, ChatRepository.currentUser) { onlineUsers, currentUser ->
        val me = currentUser?.username?.lowercase() ?: ""
        onlineUsers.filter { it.username.lowercase() != me }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val groups: StateFlow<List<Group>> = ChatRepository.groups
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
