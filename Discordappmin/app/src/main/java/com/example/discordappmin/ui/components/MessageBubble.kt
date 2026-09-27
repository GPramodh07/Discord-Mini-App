package com.example.discordappmin.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.discordappmin.model.Message
import com.example.discordappmin.ui.theme.DiscordBubbleReceived
import com.example.discordappmin.ui.theme.DiscordBubbleSent
import com.example.discordappmin.ui.theme.DiscordDarkSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    showSenderName: Boolean = false,
    onDeleteClick: ((Message) -> Unit)? = null
) {
    val alignment = if (message.isMine) Arrangement.End else Arrangement.Start
    val bubbleColor = if (message.isMine) DiscordBubbleSent else DiscordBubbleReceived
    var showDeleteDialog by remember { mutableStateOf(false) }

    val timeFormatted = remember(message.timestamp) {
        try {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    if (showDeleteDialog && onDeleteClick != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Message?", color = Color.White) },
            text = { Text("Are you sure you want to delete this message for everyone?", color = Color.LightGray) },
            containerColor = DiscordDarkSurface,
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick(message)
                    }
                ) {
                    Text("Delete", color = Color(0xFFED4245), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = alignment
    ) {
        Column(
            horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start
        ) {
            if (showSenderName) {
                Text(
                    text = message.fromUsername,
                    color = if (message.isMine) Color(0xFF5865F2) else Color(0xFF5865F2),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 2.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isMine) 16.dp else 4.dp,
                    bottomEnd = if (message.isMine) 4.dp else 16.dp
                ),
                color = bubbleColor,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            if (onDeleteClick != null) {
                                showDeleteDialog = true
                            }
                        }
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Text(
                        text = timeFormatted,
                        color = Color(0xFFAAAAAA),
                        fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
