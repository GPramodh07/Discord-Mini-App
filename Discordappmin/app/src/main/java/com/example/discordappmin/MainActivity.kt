package com.example.discordappmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.discordappmin.navigation.NavGraph
import com.example.discordappmin.ui.theme.DiscordMiniTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiscordMiniTheme {
                NavGraph()
            }
        }
    }
}

//{"type":"LOGIN","sender":"alice","content":"123"}