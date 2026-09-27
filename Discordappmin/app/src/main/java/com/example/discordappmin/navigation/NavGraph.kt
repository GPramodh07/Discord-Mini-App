package com.example.discordappmin.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.discordappmin.ui.chat.ChatScreen
import com.example.discordappmin.ui.creategroup.CreateGroupScreen
import com.example.discordappmin.ui.groupchat.GroupChatScreen
import com.example.discordappmin.ui.home.HomeScreen
import com.example.discordappmin.ui.login.LoginScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object CreateGroup : Screen("create_group")
    object Chat : Screen("chat/{userId}") {
        fun createRoute(userId: String) = "chat/$userId"
    }
    object GroupChat : Screen("group_chat/{groupId}") {
        fun createRoute(groupId: String) = "group_chat/$groupId"
    }
}

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onContactClick = { userId -> navController.navigate(Screen.Chat.createRoute(userId)) },
                onGroupClick = { groupId -> navController.navigate(Screen.GroupChat.createRoute(groupId)) },
                onCreateGroupClick = { navController.navigate(Screen.CreateGroup.route) }
            )
        }
        composable(Screen.Chat.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ChatScreen(userId = userId, onBack = { navController.popBackStack() })
        }
        composable(Screen.GroupChat.route) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            GroupChatScreen(groupId = groupId, onBack = { navController.popBackStack() })
        }
        composable(Screen.CreateGroup.route) {
            CreateGroupScreen(
                onGroupCreated = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
