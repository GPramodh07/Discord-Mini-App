package com.example.discordappmin.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.discordappmin.model.Message
import com.example.discordappmin.ui.components.AppTextField
import com.example.discordappmin.ui.components.AppTopBar
import com.example.discordappmin.ui.components.MessageBubble
import com.example.discordappmin.ui.theme.DiscordBlurple
import com.example.discordappmin.ui.theme.DiscordDarkBackground
import com.example.discordappmin.ui.theme.DiscordDarkSurface
import com.example.discordappmin.ui.theme.DiscordMiniTheme

@Composable
fun ChatScreen(
    userId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadMockMessages(userId)
    }

    ChatScreenContent(
        title = "@$userId",
        messages = messages,
        onBack = onBack,
        onSendMessage = { viewModel.sendMessage(it) },
        onDeleteMessage = { viewModel.deleteMessage(it) }
    )
}

@Composable
fun ChatScreenContent(
    title: String,
    messages: List<Message>,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onDeleteMessage: ((Message) -> Unit)? = null
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = title, onBack = onBack) },
        containerColor = DiscordDarkBackground,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DiscordDarkSurface)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = "Message $title",
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onSendMessage(textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(DiscordBlurple, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        showSenderName = false,
                        onDeleteClick = onDeleteMessage
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    DiscordMiniTheme {
        ChatScreenContent(
            title = "@alice",
            messages = listOf(
                Message(id = "m1", fromUserId = "u1", fromUsername = "alice", text = "Hey there! How is the project going?", timestamp = System.currentTimeMillis() - 60000, isMine = false),
                Message(id = "m2", fromUserId = "me", fromUsername = "me", text = "Hey! Working on the Compose UI right now.", timestamp = System.currentTimeMillis() - 30000, isMine = true),
                Message(id = "m3", fromUserId = "u1", fromUsername = "alice", text = "Awesome! Looks super smooth!", timestamp = System.currentTimeMillis() - 10000, isMine = false)
            ),
            onBack = {},
            onSendMessage = {}
        )
    }
}
