package com.example.discordappmin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.discordappmin.ui.theme.DiscordBlurple
import com.example.discordappmin.ui.theme.DiscordDarkSurface

@Composable
fun ContactListItem(
    name: String,
    isOnline: Boolean? = null,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DiscordDarkSurface)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle with initial
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DiscordBlurple),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name & Subtitle
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }

        // Online status dot if present
        if (isOnline != null) {
            Spacer(modifier = Modifier.width(8.dp))
            OnlineStatusDot(isOnline = isOnline)
        }
    }
}
