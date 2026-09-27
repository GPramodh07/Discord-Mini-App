package com.example.discordappmin.ui.creategroup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.discordappmin.model.User
import com.example.discordappmin.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CreateGroupViewModel : ViewModel() {
    var groupName by mutableStateOf("")
        private set

    val availableUsers: StateFlow<List<User>> = combine(ChatRepository.onlineUsers, ChatRepository.currentUser) { onlineUsers, currentUser ->
        val me = currentUser?.username?.lowercase() ?: ""
        onlineUsers.filter { it.username.lowercase() != me }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedUserIds: StateFlow<Set<String>> = _selectedUserIds.asStateFlow()

    fun onGroupNameChange(value: String) {
        groupName = value
    }

    fun toggleUserSelection(userId: String) {
        val current = _selectedUserIds.value
        _selectedUserIds.value = if (current.contains(userId)) {
            current - userId
        } else {
            current + userId
        }
    }

    fun createGroup(onGroupCreated: () -> Unit) {
        if (groupName.isBlank() || _selectedUserIds.value.isEmpty()) return
        viewModelScope.launch {
            val success = ChatRepository.createGroup(groupName.trim(), _selectedUserIds.value.toList())
            if (success) {
                onGroupCreated()
            }
        }
    }
}
