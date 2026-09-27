package com.example.discordappmin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DiscordBlurple,
    secondary = DiscordGreen,
    tertiary = DiscordYellow,
    background = DiscordDarkBackground,
    surface = DiscordDarkSurface,
    surfaceVariant = DiscordDarkSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFFF2F3F5),
    onSurface = Color(0xFFF2F3F5),
    onSurfaceVariant = Color(0xFFDBDEE1)
)

@Composable
fun DiscordMiniTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
