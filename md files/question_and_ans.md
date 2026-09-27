# Comprehensive Troubleshooting & Solutions — `question_and_ans.md`

This document details all technical problems encountered during the development and integration of the **Android Client (`Discordappmin`)** and **Java Socket Server (`DiscordMiniServer`)**, along with their exact root causes and detailed solutions.

---

## ❓ 1. Protocol Field Mismatch (Server Rejects App Login)

### 🔴 Problem
When attempting to log in from the Android app, the server log failed to register the user, or returned an error.
- **Root Cause**: The Android client sent `LOGIN` packets using `SocketPacket` structure: `{"type":"LOGIN", "sender":"alice", "content":"123"}`. However, `ClientHandler.java` on the server expected `json.optString("username")` and `json.optString("password")`. Because `username` and `password` fields were absent in the app's JSON packet, the server evaluated them as empty strings and rejected authentication with `"Invalid username or password"`.

### 🛠️ Solution
Modified `ClientHandler.java` on the Java server to dynamically read both JSON formats:
```java
// Reads 'sender' as username, 'content' as password for Android client format,
// with fallback to 'username'/'password' for terminal (ncat) clients.
String username = json.has("username") ? json.optString("username") : json.optString("sender");
String password = json.has("password") ? json.optString("password") : json.optString("content");
```
Both the Android app and CLI terminal tools (`ncat`) now use the same unified login protocol.

---

## ❓ 2. Asynchronous Authentication & Dynamic Presence Sync

### 🔴 Problem
The Android client initially logged users in locally without waiting for server confirmation, leading to state desynchronization. Furthermore, online status indicators did not reflect real-time socket presence.

### 🛠️ Solution
1. **Async Server ACK**: Updated `ChatRepository.kt` to use Kotlin Coroutines `CompletableDeferred<Boolean>` with a 5-second timeout. When `login()` sends a `LOGIN` packet, it awaits the server's `ACK LOGIN_SUCCESS` or `ERROR` packet before granting login.
2. **Real-time PRESENCE Listener**: Added a `PRESENCE` handler in `handleIncomingPacket()` that dynamically updates `_onlineUsers` `StateFlow`. When `bob` connects or disconnects, `alice`'s app instantly updates Bob's presence indicator from gray to green 🟢 in real time.

---

## ❓ 3. Single-Session Enforcement & `USER_OFFLINE_QUEUED`

### 🔴 Problem
1. When attempting to log in as `alice` from the app while `alice` was already connected in a terminal session, the login failed with `"User is already logged in elsewhere"`.
2. When Bob messaged Alice while Alice was offline, Bob received `{"type":"ACK", "content":"USER_OFFLINE_QUEUED"}`.

### 🛠️ Solution
1. **Single-Session Handling**: Confirmed that `SessionRegistry.java` enforces one active TCP connection per user account. Closing the existing terminal session allowed the Android app to log in cleanly as `alice`.
2. **Offline Message Queue**: Verified SQLite message archiving in `DatabaseManager.java`. Messages sent to offline users are stored with `delivered = 0`. The moment `alice` logs in, `ClientHandler.java` automatically flushes and delivers all queued messages to her device.

---

## ❓ 4. Physical USB-Debugged Phone Connectivity (`adb reverse`)

### 🔴 Problem
When testing the app on a physical Android phone connected via USB debugging cable, the app could not connect to the server log, or connections failed.
- **Root Cause**:
  - `10.0.2.2` is a special loopback alias valid **only inside Android Emulators**. Physical phones cannot route to `10.0.2.2`.
  - Over USB debugging, TCP socket traffic over `127.0.0.1` stays internal to the phone's loopback interface instead of reaching the host PC.

### 🛠️ Solution
1. **ADB USB Reverse Port Forwarding**: Executed ADB reverse port mapping on the host PC:
   ```bash
   ~/Android/Sdk/platform-tools/adb reverse tcp:5000 tcp:5000
   ```
   This creates a direct hardware USB tunnel mapping port `5000` on the USB-connected phone straight to port `5000` on the PC server.
2. **Multi-IP Connection Fallback**: Updated [`ChatRepository.kt`](file:///home/pramodh/AndroidStudioProjects/Discordappmin/app/src/main/java/com/example/discordappmin/repository/ChatRepository.kt) login connection logic:
   ```kotlin
   // 1. 127.0.0.1 (ADB USB Reverse Tunnel for Physical Phone)
   // 2. 10.8.139.239 (Host Local Wi-Fi IP)
   // 3. 10.0.2.2 (Android Emulator Loopback)
   var connected = SocketClient.connect("127.0.0.1", port)
   if (!connected) connected = SocketClient.connect("10.8.139.239", port)
   if (!connected) connected = SocketClient.connect("10.0.2.2", port)
   ```

---

## ❓ 5. Server Port Conflict (`java.net.BindException: Address already in use`)

### 🔴 Problem
Attempting to start the Java server produced `java.net.BindException: Address already in use` error in `/tmp/server.log`.
- **Root Cause**: An existing `ServerMain` process was already running in the background listening on port 5000. Launching another instance failed due to TCP port binding limits.

### 🛠️ Solution
Cleaned up background processes before restarting:
```bash
pkill -f "com.discordmini.server.ServerMain"
nohup java -cp "target/classes:lib/json.jar:lib/sqlite.jar" com.discordmini.server.ServerMain > /tmp/server.log 2>&1 &
```

---

## ✅ Summary of Verified Working System

- **Terminal ↔ Terminal Chat**: Fully functional (`alice` & `bob` CLI sessions).
- **Physical Phone (USB) ↔ Terminal Chat**: Fully functional live 2-way real-time communication.
- **Dynamic Online Presence**: Real-time green/gray status updates across all connected clients.
- **Offline Message Persistence**: Automatic SQLite queueing & flush delivery upon user login.
