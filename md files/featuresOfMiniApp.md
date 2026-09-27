# 🚀 DiscordMini System — Detailed Feature Documentation (`featuresOfMiniApp.md`)

This document provides a comprehensive overview of all features and capabilities provided by the **DiscordMini** multi-platform system, comprising the **Android Client Application (`Discordappmin`)** and the **Java Backend Socket Server (`DiscordMiniServer`)**.

---

## 📱 1. Android Client Features (`Discordappmin`)

### 🎨 Modern Jetpack Compose Dark Theme UI
- **Discord-Inspired UI**: Built with Jetpack Compose using modern dark aesthetics (`#36393F` background, `#2F3136` surface cards, `#5865F2` Discord blurple primary accents).
- **Responsive Navigation**: Bottom-nav/tabbed architecture with smooth transitions between Direct Messages, Group Channels, and Group Creation.

### 👤 Multi-User Authentication & Self-Filtering
- **Socket Authentication**: Real-time socket authentication for pre-registered users (`alice`, `bob`, `carl`, `don`).
- **Self-Filtering Contact List**: Automatically filters out the currently logged-in user so users never see themselves in their own DM list or contact selection views.

### 💬 Live 1:1 Direct Messaging (DMs)
- **Real-Time Delivery**: Asynchronous TCP socket packet transmission for instant 1:1 messaging.
- **Sender/Receiver Alignment**: Sent bubbles aligned to the right in Discord Blurple (`#5865F2`), received bubbles aligned to the left in dark gray (`#4F545C`).
- **Auto-Scroll**: Automatic smooth scrolling to the newest message upon receipt or sending.

### 👥 WhatsApp / Discord Style Group Channel Creation
- **Multi-Select Contact List**: Dynamic user selection screen allowing users to pick multiple contacts and assign a custom channel name (e.g. `pokiri`, `CS-Distributed`).
- **Automatic Group Enrollment**: Creator and all selected members are automatically enrolled into the new channel.
- **Live Broadcast Notification**: Instantly broadcasts `CREATE_GROUP` socket packets to all online members so the new group pops up live on their app screens without requiring a refresh.

### 🏷️ Group Sender Labels & Info `(i)` Button
- **Sender Attribution**: Bold username headers (`alice`, `bob`, `carl`) displayed on top of every message bubble in group chats for clear message attribution.
- **Group Info Action `(i)`**: Top bar features an info `(i)` button that opens a **Group Members Dialog**.
- **Live Member Presence Status**: Displays all group channel members with real-time presence indicators (🟢 Green dot for online, 🔘 Gray dot for offline).

### 🕒 Message Timestamps
- **Formatted Time Display**: Every message bubble features a clean timestamp (`h:mm a`, e.g., `7:33 PM`) at the bottom right of the bubble.

### 🗑️ Message Deletion for Everyone (1:1 & Group Chat)
- **Long-Press Dialog**: Long-pressing any message bubble opens a confirmation modal (*"Delete Message for everyone?"*).
- **Real-Time Cross-Client Sync**: Deleting a message removes it immediately from local UI, sends a `DELETE_MSG` socket packet to the server, wipes the record from the SQLite database, and removes the message live from all active recipients' screens.

### 📜 Historical Chat & Channel Restoration
- **Past Conversation Sync**: Reopening the app or logging in automatically fetches and restores all past 1:1 direct messages and joined group channel history from the server SQLite DB.
- **Message De-duplication**: Smart de-duplication prevents duplicate bubbles when restoring history over socket connections.

### 🔌 Multi-IP & ADB USB Phone Connectivity
- **ADB USB Reverse Tunnel Support**: Connects over `127.0.0.1:5000` via ADB reverse port forwarding (`adb reverse tcp:5000 tcp:5000`) for seamless physical phone debugging.
- **Fallback Multi-IP Routing**: Automatically attempts connection via `127.0.0.1` (USB tunnel), `10.8.139.239` (Wi-Fi network IP), and `10.0.2.2` (Android emulator).

---

## 🖥️ 2. Java Backend Server Features (`DiscordMiniServer`)

### ⚡ Multithreaded TCP Socket Server (`ServerMain.java`)
- **NIO / Sockets Engine**: Multithreaded TCP socket server listening on port `5000` using Java `CachedThreadPool`.
- **NDJSON Protocol Handler**: Processes line-based JSON socket packets (`LOGIN`, `REGISTER`, `PRIVATE_MSG`, `GROUP_MSG`, `CREATE_GROUP`, `DELETE_MSG`, `PRESENCE`, `ACK`, `ERROR`).

### 🗄️ SQLite Database Persistence (`DatabaseManager.java`)
- **User Accounts Table**: Pre-seeded accounts (`alice`, `bob`, `carl`, `don`), hashed passwords, last-seen timestamps, online/offline status.
- **Groups & Members Tables**: Persistent channel mapping and member list management.
- **Messages Archive Table**: Complete chat history archiving with an `offline` queue flag (`delivered = 0/1`).

### 📢 Group Pub/Sub Fan-Out Engine (`GroupRegistry.java` & `SessionRegistry.java`)
- **Concurrent Session Tracking**: Thread-safe active user socket lookup tables.
- **Live Channel Fan-Out**: Instantly routes group messages and notifications to all active online channel members.

### 📬 Offline Message Queue
- **Offline Persistence**: Messages sent to disconnected contacts are saved to SQLite with `delivered = 0`.
- **Automatic Flush on Login**: When an offline user connects, all pending unread messages and joined group channels are automatically delivered and marked as `delivered = 1`.

### 🟢 Live Presence Monitoring
- **Real-Time Status Broadcasts**: Automatically detects client socket connects/disconnects and broadcasts `PRESENCE` packets (`ONLINE`/`OFFLINE`) to all active sessions.

### 💻 Multi-Terminal & App Cross-Platform Integration
- **CLI Terminal Compatibility**: Supports non-GUI terminal clients (e.g. `ncat` / `netcat` / custom TCP client) sending raw NDJSON packets to interact live with Android app users.

---

## 📊 Summary of System Capabilities

| Feature Category | Description | Status |
| :--- | :--- | :---: |
| **Authentication & User Profiles** | Socket login for `alice`, `bob`, `carl`, `don` with self-filtering DM list | ✅ Completed |
| **Direct Messaging (DMs)** | Live 1:1 TCP socket messaging with timestamp & auto-scroll | ✅ Completed |
| **WhatsApp/Discord Group Channels** | Dynamic multi-select group creation with live member enrollment | ✅ Completed |
| **Group Info `(i)` & Presence** | Top-bar info button displaying member list with live 🟢 / 🔘 status dots | ✅ Completed |
| **Sender Username Labels** | Bold sender name header above every message bubble in group chats | ✅ Completed |
| **Message Deletion** | Long-press delete for 1:1 and Group chats across UI, socket & SQLite DB | ✅ Completed |
| **Chat & Channel History Sync** | Auto-restoration of all past 1:1 and group conversations upon login | ✅ Completed |
| **SQLite Backend Engine** | Persistent storage for users, groups, members, and messages | ✅ Completed |
| **Offline Message Queue** | Stores messages for offline users and flushes upon re-login | ✅ Completed |
| **Physical Phone & Terminal Support**| Works live over ADB USB reverse tunnel and terminal netcat clients | ✅ Completed |
