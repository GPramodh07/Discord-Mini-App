package com.example.discordappmin.model

data class Message(
    val id: String,
    val fromUserId: String,
    val fromUsername: String,
    val recipientId: String? = null,
    val groupId: String? = null,
    val text: String,
    val timestamp: Long,
    val isMine: Boolean
)
