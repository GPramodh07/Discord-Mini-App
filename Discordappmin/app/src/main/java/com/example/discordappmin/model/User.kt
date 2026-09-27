package com.example.discordappmin.model

data class User(
    val id: String,
    val username: String,
    val isOnline: Boolean = false
)
