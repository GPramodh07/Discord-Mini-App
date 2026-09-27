# Next Action Plan: Android Socket Integration (`android_socket_integration_plan.md`)

## 📌 Executive Summary
With the **Java Socket Backend (`DiscordMiniServer`)** 100% verified and operating with real-time NDJSON socket streams, $O(1)$ DM routing, pub/sub group messaging, and SQLite persistence, the next objective is to integrate the **Android Client UI (`Discordappmin`)** with the live socket network layer ([`ChatRepository.kt`](file:///home/pramodh/AndroidStudioProjects/Discordappmin/app/src/main/java/com/example/discordappmin/repository/ChatRepository.kt)).

---

## 🎯 Step-by-Step Execution Plan

### Step 1: Connect `LoginViewModel` to Live Network Authentication
- **Current State**: Uses mock delay and local navigation.
- **Action**: 
  - Update `LoginViewModel.kt` to trigger `ChatRepository.login(username, host, port)`.
  - Handle connection states (`CONNECTING`, `SUCCESS`, `ERROR`).
  - Navigate to `HomeScreen` upon receiving successful `LOGIN` socket ACK.

### Step 2: Connect `HomeViewModel` to Live Presence & Group Feeds
- **Current State**: Displays hardcoded list of direct messages and channels.
- **Action**:
  - Collect `ChatRepository.onlineUsers` `StateFlow` to update user presence indicators (`OnlineStatusDot`) dynamically.
  - Collect `ChatRepository.groups` `StateFlow` to display active group channels.

### Step 3: Connect `ChatViewModel` (1:1 Direct Messaging)
- **Current State**: Uses static in-memory list of mock messages.
- **Action**:
  - Filter `ChatRepository.messages` `StateFlow` for the selected recipient username.
  - Bind `onSendMessage` UI action to `ChatRepository.sendPrivateMessage(recipient, text)`.
  - Auto-scroll Compose `LazyColumn` on new incoming DM packets.

### Step 4: Connect `GroupChatViewModel` & `CreateGroupViewModel`
- **Current State**: Pre-populated mock group messages and static member selection.
- **Action**:
  - Update `CreateGroupViewModel.kt` to send `CREATE_GROUP` socket packet with selected member IDs.
  - Bind `GroupChatViewModel.kt` send action to `ChatRepository.sendGroupMessage(groupId, text)`.

### Step 5: End-to-End System Testing & Verification
- **Execution**:
  - Keep Java server (`ServerMain`) running on port `5000`.
  - Launch `Discordappmin` on Android Emulator or physical Android device.
  - Open a terminal session (`nc localhost 5000`) acting as a second user.
  - Verify bi-directional, real-time message delivery between the Android App UI and the terminal client!
