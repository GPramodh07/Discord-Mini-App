package com.example.discordappmin.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.discordappmin.ui.theme.DiscordBlurple
import com.example.discordappmin.ui.theme.DiscordDarkSurfaceVariant

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = visualTransformation,
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DiscordDarkSurfaceVariant,
            unfocusedContainerColor = DiscordDarkSurfaceVariant,
            disabledContainerColor = DiscordDarkSurfaceVariant,
            focusedBorderColor = DiscordBlurple,
            unfocusedBorderColor = Color.Transparent,
            focusedLabelColor = DiscordBlurple,
            unfocusedLabelColor = Color.Gray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        modifier = modifier.fillMaxWidth()
    )
}
