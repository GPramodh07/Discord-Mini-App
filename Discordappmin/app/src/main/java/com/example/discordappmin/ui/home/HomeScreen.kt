package com.example.discordappmin.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.discordappmin.model.Group
import com.example.discordappmin.model.User
import com.example.discordappmin.ui.components.AppTopBar
import com.example.discordappmin.ui.components.ContactListItem
import com.example.discordappmin.ui.theme.DiscordBlurple
import com.example.discordappmin.ui.theme.DiscordDarkBackground
import com.example.discordappmin.ui.theme.DiscordDarkSurface
import com.example.discordappmin.ui.theme.DiscordMiniTheme

@Composable
fun HomeScreen(
    onContactClick: (String) -> Unit,
    onGroupClick: (String) -> Unit,
    onCreateGroupClick: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val titleText = currentUser?.username?.let { "DiscordMini (@$it)" } ?: "DiscordMini"

    HomeScreenContent(
        title = titleText,
        users = users,
        groups = groups,
        onContactClick = onContactClick,
        onGroupClick = onGroupClick,
        onCreateGroupClick = onCreateGroupClick
    )
}

@Composable
fun HomeScreenContent(
    title: String = "DiscordMini",
    users: List<User>,
    groups: List<Group>,
    onContactClick: (String) -> Unit,
    onGroupClick: (String) -> Unit,
    onCreateGroupClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Direct Messages", "Groups")

    Scaffold(
        topBar = { AppTopBar(title = title) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateGroupClick,
                containerColor = DiscordBlurple,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Group")
            }
        },
        containerColor = DiscordDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DiscordDarkSurface,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = DiscordBlurple
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(text = title, color = if (selectedTab == index) Color.White else Color.Gray) }
                    )
                }
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(users) { user ->
                        ContactListItem(
                            name = user.username,
                            isOnline = user.isOnline,
                            onClick = { onContactClick(user.id) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groups) { group ->
                        ContactListItem(
                            name = group.name,
                            subtitle = "${group.memberIds.size} members",
                            onClick = { onGroupClick(group.id) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    DiscordMiniTheme {
        HomeScreenContent(
            users = listOf(
                User(id = "u1", username = "alice", isOnline = true),
                User(id = "u2", username = "bob", isOnline = false)
            ),
            groups = listOf(
                Group(id = "g1", name = "CS-Distributed", memberIds = listOf("u1", "u2"))
            ),
            onContactClick = {},
            onGroupClick = {},
            onCreateGroupClick = {}
        )
    }
}
