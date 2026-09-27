package com.example.discordappmin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.discordappmin.ui.theme.OfflineGray
import com.example.discordappmin.ui.theme.OnlineGreen

@Composable
fun OnlineStatusDot(
    isOnline: Boolean,
    size: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isOnline) OnlineGreen else OfflineGray)
    )
}
