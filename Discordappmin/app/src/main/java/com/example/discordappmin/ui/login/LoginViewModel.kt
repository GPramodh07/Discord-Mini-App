package com.example.discordappmin.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.discordappmin.repository.ChatRepository
import com.example.discordappmin.repository.LoginResult
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    var username by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun onUsernameChange(value: String) {
        username = value
        errorMessage = null
    }

    fun onPasswordChange(value: String) {
        password = value
        errorMessage = null
    }

    fun login(onSuccess: () -> Unit) {
        if (username.isBlank()) {
            errorMessage = "Username cannot be empty"
            return
        }

        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            when (val result = ChatRepository.login(username.trim(), password.trim())) {
                is LoginResult.Success -> {
                    isLoading = false
                    onSuccess()
                }
                is LoginResult.Error -> {
                    isLoading = false
                    errorMessage = result.message
                }
            }
        }
    }
}
