package com.example.discordappmin.ui.creategroup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.discordappmin.model.User
import com.example.discordappmin.ui.components.AppTextField
import com.example.discordappmin.ui.components.AppTopBar
import com.example.discordappmin.ui.components.ContactListItem
import com.example.discordappmin.ui.theme.DiscordBlurple
import com.example.discordappmin.ui.theme.DiscordDarkBackground
import com.example.discordappmin.ui.theme.DiscordDarkSurface
import com.example.discordappmin.ui.theme.DiscordMiniTheme

@Composable
fun CreateGroupScreen(
    onGroupCreated: () -> Unit,
    onBack: () -> Unit,
    viewModel: CreateGroupViewModel = viewModel()
) {
    val availableUsers by viewModel.availableUsers.collectAsState()
    val selectedUserIds by viewModel.selectedUserIds.collectAsState()

    CreateGroupScreenContent(
        groupName = viewModel.groupName,
        onGroupNameChange = { viewModel.onGroupNameChange(it) },
        availableUsers = availableUsers,
        selectedUserIds = selectedUserIds,
        onToggleUserSelection = { viewModel.toggleUserSelection(it) },
        onCreateGroup = { viewModel.createGroup(onGroupCreated) },
        onBack = onBack
    )
}

@Composable
fun CreateGroupScreenContent(
    groupName: String,
    onGroupNameChange: (String) -> Unit,
    availableUsers: List<User>,
    selectedUserIds: Set<String>,
    onToggleUserSelection: (String) -> Unit,
    onCreateGroup: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { AppTopBar(title = "Create Group Channel", onBack = onBack) },
        containerColor = DiscordDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            AppTextField(
                value = groupName,
                onValueChange = onGroupNameChange,
                label = "Group Name"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Select Members",
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableUsers) { user ->
                    val isSelected = selectedUserIds.contains(user.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DiscordDarkSurface, RoundedCornerShape(8.dp))
                            .clickable { onToggleUserSelection(user.id) }
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            ContactListItem(
                                name = user.username,
                                isOnline = user.isOnline,
                                onClick = { onToggleUserSelection(user.id) }
                            )
                        }
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleUserSelection(user.id) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = DiscordBlurple,
                                uncheckedColor = Color.Gray
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCreateGroup,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DiscordBlurple)
            ) {
                Text(
                    text = "Create Group",
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
fun CreateGroupScreenPreview() {
    DiscordMiniTheme {
        CreateGroupScreenContent(
            groupName = "Dev Team",
            onGroupNameChange = {},
            availableUsers = listOf(
                User(id = "u1", username = "alice", isOnline = true),
                User(id = "u2", username = "bob", isOnline = false),
                User(id = "u3", username = "carl", isOnline = true),
                User(id = "u4", username = "david", isOnline = false)
            ),
            selectedUserIds = setOf("u1", "u3"),
            onToggleUserSelection = {},
            onCreateGroup = {},
            onBack = {}
        )
    }
}
