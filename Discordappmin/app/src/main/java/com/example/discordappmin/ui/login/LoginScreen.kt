package com.example.discordappmin.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.discordappmin.ui.components.AppTextField
import com.example.discordappmin.ui.theme.DiscordBlurple
import com.example.discordappmin.ui.theme.DiscordDarkBackground
import com.example.discordappmin.ui.theme.DiscordMiniTheme
import com.example.discordappmin.ui.theme.DiscordRed

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    LoginScreenContent(
        username = viewModel.username,
        onUsernameChange = { viewModel.onUsernameChange(it) },
        password = viewModel.password,
        onPasswordChange = { viewModel.onPasswordChange(it) },
        errorMessage = viewModel.errorMessage,
        onLoginClick = { viewModel.login(onLoginSuccess) }
    )
}

@Composable
fun LoginScreenContent(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    errorMessage: String?,
    onLoginClick: () -> Unit
) {
    Scaffold(
        containerColor = DiscordDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome Back!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "We're so excited to see you again!",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            AppTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = "Username"
            )

            Spacer(modifier = Modifier.height(16.dp))

            AppTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Password",
                visualTransformation = PasswordVisualTransformation()
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = DiscordRed,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
            ) {
                Text(
                    text = "Log In",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    DiscordMiniTheme {
        LoginScreenContent(
            username = "",
            onUsernameChange = {},
            password = "",
            onPasswordChange = {},
            errorMessage = null,
            onLoginClick = {}
        )
    }
}
