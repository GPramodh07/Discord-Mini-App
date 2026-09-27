package com.example.discordappmin.ui.groupchat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.discordappmin.model.Message
import com.example.discordappmin.model.User
import com.example.discordappmin.ui.components.AppTextField
import com.example.discordappmin.ui.components.AppTopBar
import com.example.discordappmin.ui.components.MessageBubble
import com.example.discordappmin.ui.components.OnlineStatusDot
import com.example.discordappmin.ui.theme.DiscordBlurple
import com.example.discordappmin.ui.theme.DiscordDarkBackground
import com.example.discordappmin.ui.theme.DiscordDarkSurface
import com.example.discordappmin.ui.theme.DiscordMiniTheme

@Composable
fun GroupChatScreen(
    groupId: String,
    onBack: () -> Unit,
    viewModel: GroupChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val groupName by viewModel.groupName.collectAsState()
    val members by viewModel.groupMembers.collectAsState()

    LaunchedEffect(groupId) {
        viewModel.loadMockGroupMessages(groupId)
    }

    val displayTitle = if (groupName.isNotBlank()) groupName else groupId

    GroupChatScreenContent(
        groupName = displayTitle,
        members = members,
        messages = messages,
        onBack = onBack,
        onSendMessage = { viewModel.sendMessage(it) },
        onDeleteMessage = { viewModel.deleteMessage(it) }
    )
}

@Composable
fun GroupChatScreenContent(
    groupName: String,
    members: List<User> = emptyList(),
    messages: List<Message>,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onDeleteMessage: ((Message) -> Unit)? = null
) {
    var textInput by remember { mutableStateOf("") }
    var showMembersDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showMembersDialog) {
        AlertDialog(
            onDismissRequest = { showMembersDialog = false },
            containerColor = DiscordDarkSurface,
            title = {
                Text(
                    text = "Group Members (${members.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    if (members.isEmpty()) {
                        Text("No member info available", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        members.forEach { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(DiscordBlurple, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user.username.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "@${user.username}",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                OnlineStatusDot(isOnline = user.isOnline, size = 10.dp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMembersDialog = false }) {
                    Text("Close", color = DiscordBlurple, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "# $groupName",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showMembersDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Group Members Info",
                            tint = Color.White
                        )
                    }
                }
            )
        },
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
                    label = "Message to $groupName",
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
                        showSenderName = true,
                        onDeleteClick = onDeleteMessage
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GroupChatScreenPreview() {
    DiscordMiniTheme {
        GroupChatScreenContent(
            groupName = "CS-Distributed",
            members = listOf(
                User("alice", "alice", true),
                User("bob", "bob", false)
            ),
            messages = listOf(
                Message(id = "gm1", fromUserId = "u1", fromUsername = "alice", groupId = "g1", text = "Welcome to the group chat!", timestamp = System.currentTimeMillis() - 120000, isMine = false),
                Message(id = "gm2", fromUserId = "u2", fromUsername = "bob", groupId = "g1", text = "Hey everyone! Socket server ready?", timestamp = System.currentTimeMillis() - 60000, isMine = false),
                Message(id = "gm3", fromUserId = "me", fromUsername = "me", groupId = "g1", text = "Yep, Kotlin Compose UI is running!", timestamp = System.currentTimeMillis() - 20000, isMine = true)
            ),
            onBack = {},
            onSendMessage = {}
        )
    }
}
