# UI Build Plan — DiscordMiniClient (Today's Session)

Goal for today: **all screens built and navigable end-to-end with mock/fake data**, no real networking yet (that's Day 4). By end of today you should be able to tap through Login → Home → Private Chat → Group Chat → back, on a device/emulator, with dummy messages showing.

---

## 0. Package Structure (create these first)

```
app/src/main/java/com/yourpkg/discordmini/
├── MainActivity.kt
├── navigation/
│   └── NavGraph.kt
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   └── Theme.kt
│   ├── components/
│   │   ├── MessageBubble.kt
│   │   ├── ContactListItem.kt
│   │   ├── OnlineStatusDot.kt
│   │   ├── AppTopBar.kt
│   │   └── AppTextField.kt
│   ├── login/
│   │   ├── LoginScreen.kt
│   │   └── LoginViewModel.kt
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── chat/
│   │   ├── ChatScreen.kt
│   │   └── ChatViewModel.kt
│   ├── groupchat/
│   │   ├── GroupChatScreen.kt
│   │   └── GroupChatViewModel.kt
│   └── creategroup/
│       ├── CreateGroupScreen.kt
│       └── CreateGroupViewModel.kt
└── model/
    ├── User.kt
    ├── Message.kt
    └── Group.kt
```

Create empty files for all of these first (2 minutes), so you're filling in blanks rather than context-switching between creating files and writing code.

---

## 1. Data Models (`model/`) — plain Kotlin data classes, no networking logic yet

```kotlin
// User.kt
data class User(
    val id: String,
    val username: String,
    val isOnline: Boolean = false
)

// Message.kt
data class Message(
    val id: String,
    val fromUserId: String,
    val fromUsername: String,
    val text: String,
    val timestamp: Long,
    val isMine: Boolean   // true if sent by the logged-in user -> controls bubble alignment
)

// Group.kt
data class Group(
    val id: String,
    val name: String,
    val memberIds: List<String>
)
```

These are your UI-layer models. When you wire networking on Day 4, you'll map server JSON → these models (or reuse them directly if the shape matches — decide then, don't overthink now).

---

## 2. Theme Setup (`ui/theme/`) — do this before any screen

- [ ] `Color.kt`: define a small palette — primary, secondary, background, surface, bubble-sent, bubble-received. Keep it to ~6 colors, don't over-engineer.
- [ ] `Type.kt`: default `Typography()` is fine — only customize if you have time later.
- [ ] `Theme.kt`: wrap in `MaterialTheme(colorScheme = ..., typography = ..., content = ...)`. Android Studio's Compose template already generates a working version of this — just tweak the seed colors so it doesn't look like a stock template.
- [ ] Confirm `MainActivity.kt` calls `setContent { DiscordMiniTheme { NavGraph() } }`

Get this compiling and previewing once before moving on — it's the foundation every screen sits on.

---

## 3. Navigation Graph (`navigation/NavGraph.kt`)

Define routes as a sealed class or object early so screens can reference them without typos:

```kotlin
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
```

```kotlin
@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(onLoginSuccess = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } })
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
            CreateGroupScreen(onGroupCreated = { navController.popBackStack() })
        }
    }
}
```

- [ ] Add `androidx.navigation:navigation-compose` to `build.gradle.kts` if the template didn't already include it
- [ ] Wire `NavGraph()` into `MainActivity`
- [ ] **Checkpoint**: run the app — you should see the Login screen (even if empty/placeholder) with no crash. Do this before building out screen internals.

---

## 4. Reusable Components (`ui/components/`) — build these before the screens that use them

| Component | Purpose | Key Compose bits |
|---|---|---|
| `AppTopBar` | Consistent top bar across screens (title + optional back button) | `TopAppBar`, `IconButton` with `Icons.AutoMirrored.Filled.ArrowBack` |
| `AppTextField` | Styled `OutlinedTextField` wrapper for username/password/message input | `OutlinedTextField`, optional `visualTransformation` for password |
| `OnlineStatusDot` | Small colored circle (green=online, gray=offline) | `Canvas` or a `Box` with `Modifier.clip(CircleShape).background(color)` |
| `ContactListItem` | One row in the contact/group list: avatar placeholder + name + status dot | `Row`, `ListItem` (Material3), takes `User` or `Group` as param |
| `MessageBubble` | Chat bubble, aligned left (received) or right (sent), different background color | `Row` with `Arrangement.End`/`Start`, `Surface` with `RoundedCornerShape`, takes `Message` as param |

Build each with a `@Preview` so you can see it in isolation before wiring it into a full screen — this is much faster than deploying to device every time.

---

## 5. Screens — build in this order (each depends on the previous ones' components)

### 5.1 Login Screen
- [ ] `Scaffold` with app title, two `AppTextField`s (username, password), one primary `Button` ("Login")
- [ ] Optional: a "Register instead" `TextButton` — can be a no-op today, or a second identical form; don't over-build this, registration flow can reuse the same screen with a toggle
- [ ] `LoginViewModel`: holds `username`/`password` as `mutableStateOf` (or a small `UiState` data class exposed via `StateFlow`), exposes `onLoginClick` — **for today, just validate non-empty fields and call `onLoginSuccess()` directly** (fake success, no real auth yet — that's Day 4)
- [ ] `@Preview` for the screen with sample state

### 5.2 Home Screen
- [ ] `Scaffold` with `AppTopBar` (title "DiscordMini"), a `FloatingActionButton` for "Create Group"
- [ ] `TabRow` with two tabs: "Contacts" and "Groups" (simple `var selectedTab by remember { mutableStateOf(0) }`, no need for full `HorizontalPager` unless you want swipe — button tabs are enough for today)
- [ ] Contacts tab: `LazyColumn` of `ContactListItem`, using **mock data** — a hardcoded `List<User>` of 4–5 fake users in `HomeViewModel` (e.g., `User("u1","alice",true)`, `User("u2","bob",false)`, ...)
- [ ] Groups tab: `LazyColumn` of group rows (reuse `ContactListItem` styling or a near-identical `GroupListItem`), mock `List<Group>` with 2 fake groups
- [ ] Tapping a contact → `onContactClick(user.id)`; tapping a group → `onGroupClick(group.id)`
- [ ] `@Preview` with mock data

### 5.3 Chat Screen (private chat)
- [ ] `Scaffold` with `AppTopBar` showing the other user's name + back arrow, `bottomBar` containing a `Row` (message `AppTextField` + send `IconButton`)
- [ ] Body: `LazyColumn` (reverse layout or just scroll-to-bottom on new item) of `MessageBubble`s, from **mock data**: a hardcoded `List<Message>` in `ChatViewModel` alternating `isMine = true/false`
- [ ] Send button (today): just appends a new `Message(isMine = true, ...)` to the local list so you can see the bubble render and the list scroll — no actual sending yet
- [ ] `@Preview` with a handful of mock messages

### 5.4 Group Chat Screen
- [ ] Nearly identical to Chat Screen — reuse `MessageBubble`, but also show the **sender's name** above/inside received bubbles (private chat doesn't need this since you already know who you're talking to; group chat does)
- [ ] Top bar shows group name instead of a person's name
- [ ] Mock data: reuse the same `Message` model, a few messages from different `fromUsername`s

### 5.5 Create Group Screen
- [ ] Text field for group name
- [ ] `LazyColumn` of contacts with a `Checkbox` next to each (multi-select) — reuse mock contact list from Home
- [ ] "Create" button → for today, just calls `onGroupCreated()` to pop back to Home (no real group persisted yet)

---

## 6. Order of Execution for Today (suggested time-boxing)

1. **Package skeleton + theme** — ~20 min
2. **Navigation graph wired with placeholder empty screens** (just a `Text("Login Screen")` etc. in each) — ~20 min, confirms nav works before you invest in real UI
3. **Reusable components** (`MessageBubble`, `ContactListItem`, `OnlineStatusDot`, `AppTopBar`, `AppTextField`) with previews — ~1 hr
4. **Login screen** — ~30 min
5. **Home screen** (contacts + groups tabs, mock data) — ~1 hr
6. **Chat screen** — ~45 min
7. **Group chat screen** — ~30 min (mostly reuse)
8. **Create group screen** — ~30 min
9. **Full click-through test on device/emulator**: Login → Home → tap a contact → send a mock message → back → tap a group → back → tap FAB → create group → back to Home — ~20–30 min

That's roughly a full focused day. If you run short on time, cut Create Group's checkbox multi-select down to just tapping names to toggle selection (skip actual `Checkbox` widget styling) — functionally identical, less fiddly UI work.

---

## 7. What NOT to do today (save for Day 4 — networking)

- Don't wire any real `Socket`/service code into ViewModels yet — keep every ViewModel's data source as a hardcoded mock list. Swapping mock data for real `StateFlow` from a repository later is a small, contained change *if* your screens already observe state via `ViewModel` + `StateFlow`/`collectAsState()` instead of holding data directly in the Composable.
- Don't build the Foreground Service yet.
- Don't add `kotlinx.serialization` model annotations yet — today's `model/` classes are plain UI models; you'll decide on Day 4 whether to reuse them as `@Serializable` or map between two sets of models.
- Don't polish animations/transitions — get every screen reachable and functional first, polish only if Day 5 buffer time allows.

---

## 8. End-of-Day Checklist

- [ ] App builds and runs with no crashes
- [ ] Can navigate: Login → Home → Chat → back → Home → GroupChat → back → Home → CreateGroup → back
- [ ] Contacts show an online/offline dot with two different colors
- [ ] Chat and Group Chat show sent messages right-aligned, received left-aligned, with visibly different bubble colors
- [ ] Sending a message in Chat/GroupChat screen appends it to the visible list immediately
- [ ] All screens use `MaterialTheme` colors/typography (nothing hardcoded outside the theme file)
- [ ] Every screen/component has at least one `@Preview` that renders without error
