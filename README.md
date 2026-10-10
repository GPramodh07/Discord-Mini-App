<div align="center">

<br/>

<img src="./assets/logo_no_bg.png" width="140" alt="DiscordMini Logo" />

# ⚡ DiscordMini

### *Real-Time Distributed Messaging Ecosystem — Built from Scratch*

<br/>

[![Download APK](https://img.shields.io/badge/⚡_Download_APK-GitHub_Release-5865F2?style=for-the-badge&logo=android&logoColor=white)](https://github.com/GPramodh07/Discord-Mini-App/releases/download/v2.0.0/DiscordMiniApp_V2.0.0.apk)
[![Download Google Drive](https://img.shields.io/badge/📁_Google_Drive_Mirror-v2.0.0-34A853?style=for-the-badge&logo=googledrive&logoColor=white)](https://drive.google.com/file/d/14AuAz-yuUiJYktQX4oqyUHer7FbeqRio/view?usp=sharing)

<br/>

[![Android](https://img.shields.io/badge/Android-Jetpack_Compose-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![SQLite](https://img.shields.io/badge/SQLite-Database-003B57?style=for-the-badge&logo=sqlite&logoColor=white)](https://www.sqlite.org/)
[![AWS EC2](https://img.shields.io/badge/AWS-EC2_Deployed-FF9900?style=for-the-badge&logo=amazon-aws&logoColor=white)](https://aws.amazon.com/)
[![TCP Sockets](https://img.shields.io/badge/Networking-Raw_TCP_Sockets-007ACC?style=for-the-badge)](https://en.wikipedia.org/wiki/Transmission_Control_Protocol)
[![License: MIT](https://img.shields.io/badge/License-MIT-F59E0B?style=for-the-badge)](LICENSE)

<br/>

> **DiscordMini** is a full-stack real-time messaging ecosystem featuring a modern **Jetpack Compose Android Client** powered by an **Android Foreground Service** connected to a multithreaded **Java TCP Socket Backend** deployed live on **AWS EC2**.

<br/>

<div align="center">

<a href="https://github.com/GPramodh07/Discord-Mini-App/releases/download/v2.0.0/DiscordMiniApp_V2.0.0.apk">
  <img src="https://img.shields.io/badge/⚡_DOWNLOAD_VIA_GITHUB-v2.0.0-5865F2?style=for-the-badge&logo=android&logoColor=white" height="44" alt="Download GitHub APK" />
</a>
&nbsp;&nbsp;
<a href="https://drive.google.com/file/d/14AuAz-yuUiJYktQX4oqyUHer7FbeqRio/view?usp=sharing">
  <img src="https://img.shields.io/badge/📁_DOWNLOAD_VIA_GOOGLE_DRIVE-Mirror-34A853?style=for-the-badge&logo=googledrive&logoColor=white" height="44" alt="Download Google Drive APK" />
</a>

</div>

</div>

---

## 📸 App Showcase

<div align="center">

| 🔐 **Authentication** | 💬 **Direct Messages** | 👥 **Group Channels** | 💬 **Group Chat** |
|:---:|:---:|:---:|:---:|
| <img src="assets/login_page.jpeg" width="220" alt="Login Screen"/> | <img src="assets/chat.jpeg" width="220" alt="DM List"/> | <img src="assets/groups.jpeg" width="220" alt="Groups List"/> | <img src="assets/group_chat.jpeg" width="220" alt="Group Chat"/> |

</div>

---

## ✨ Production Feature Highlights

- 🚀 **Production-Ready Android Release**: Pre-built Android APK ready for immediate deployment ([**Download v2.0.0 APK**](https://github.com/GPramodh07/Discord-Mini-App/releases/download/v2.0.0/DiscordMiniApp_V2.0.0.apk)).
- ☁️ **AWS Cloud VPS Backend**: Multithreaded TCP server deployed on AWS EC2 (`3.107.163.173:5000`) with automatic local fallback for USB ADB debugging (`127.0.0.1`).
- ⚠️ **Comprehensive Error Handling & Authentication**:
  - **Server Down Alert**: Displays `"Server is not running, contact admin"` when TCP connections fail.
  - **Invalid Password**: Surfaced as `"Incorrect password. Please try again."`.
  - **Account Verification**: Surfaced as `"User does not exist in database"`.
- 🔴 **WhatsApp & Discord Style Unread Badges**: Real-time unread count badges (`1`, `2`, `99+`) rendered on the right corner of DM contacts and group items, automatically clearing upon entering the conversation.
- 🔋 **Android Foreground Service & CPU WakeLocks**: `SocketForegroundService` uses a CPU `PARTIAL_WAKE_LOCK` to keep TCP socket connections alive when the app is minimized, screen-locked, or running in dual app clones.
- 💬 **Strict 1:1 DMs & Group Channels**: Complete separation between private direct messages and multi-user group chat channels.
- 🚪 **Explicit Disconnection & Logout**: Action bar logout with confirmation dialog sending `PRESENCE OFFLINE` packets, closing socket streams, stopping foreground services, and clearing navigation state.
- 🗑️ **Cross-Client Real-Time Message Deletion**: Synchronized message deletion across all connected clients backed by SQLite database persistence.
- 🟢 **Live Online Presence Indicators**: Real-time broadcast of online/offline status for contacts.
- 🗄️ **SQLite Sync & Persistence**: Automatic history restoration, group channel loading, and offline message queueing powered by SQLite JDBC.

---

## 🏗️ System Architecture & Protocol

### Architecture Diagram

```mermaid
graph TD
    subgraph Client["📱 Android Client (Discordappmin)"]
        UI["Jetpack Compose UI<br/>(Material 3 Theme)"] --> ViewModels["ViewModels & StateFlow<br/>(MVVM Architecture)"]
        ViewModels --> Repository["ChatRepository & SocketClient"]
        Repository --> Service["SocketForegroundService<br/>(WakeLock + KeepAlive)"]
    end

    Service <== "Custom NDJSON-over-TCP<br/>(Port 5000 / AWS EC2)" ==> ServerMain

    subgraph Server["🖥️ Backend Server (DiscordMiniServer - AWS EC2)"]
        ServerMain["ServerMain<br/>(ServerSocket Listener)"] -->|Spawns Thread per Client| Handlers["ClientHandler Thread Pool"]
        Handlers --> Registry["SessionRegistry & GroupRegistry"]
        Handlers --> DB["DatabaseManager<br/>(SQLite JDBC)"]
    end

    subgraph Storage["🗄️ Persistence"]
        DB --> SQLite[("SQLite DB<br/>discord_mini.db")]
    end

    classDef client fill:#1e1e2e,stroke:#89b4fa,stroke-width:2px,color:#cdd6f4;
    classDef server fill:#181825,stroke:#f9e2af,stroke-width:2px,color:#cdd6f4;
    classDef storage fill:#11111b,stroke:#a6e3a1,stroke-width:2px,color:#cdd6f4;
    class UI,ViewModels,Repository,Service client;
    class ServerMain,Handlers,Registry,DB server;
    class SQLite storage;
```

### Custom NDJSON Packet Protocol

The Android client and Java backend communicate over raw TCP using line-delimited JSON objects:

```json
{"type":"LOGIN","sender":"alice","password":"123"}
{"type":"PRIVATE_MSG","sender":"alice","recipient":"bob","content":"Hello Bob!","id":"msg-123"}
{"type":"GROUP_MSG","sender":"alice","groupId":"g1","content":"Hey everyone!","id":"msg-456"}
{"type":"DELETE_MSG","sender":"alice","content":"msg-123"}
{"type":"ERROR","sender":"SERVER","recipient":"alice","content":"Incorrect password. Please try again."}
```

---

## 📲 Download & Test Credentials

### 1️⃣ Download APK

<div align="center">

<a href="https://github.com/GPramodh07/Discord-Mini-App/releases/download/v2.0.0/DiscordMiniApp_V2.0.0.apk">
  <img src="https://img.shields.io/badge/⚡_DOWNLOAD_VIA_GITHUB-v2.0.0-5865F2?style=for-the-badge&logo=android&logoColor=white" height="44" alt="Download GitHub APK" />
</a>
&nbsp;&nbsp;
<a href="https://drive.google.com/file/d/14AuAz-yuUiJYktQX4oqyUHer7FbeqRio/view?usp=sharing">
  <img src="https://img.shields.io/badge/📁_DOWNLOAD_VIA_GOOGLE_DRIVE-Mirror-34A853?style=for-the-badge&logo=googledrive&logoColor=white" height="44" alt="Download Google Drive APK" />
</a>

</div>

### 2️⃣ Pre-Seeded Test Credentials

| 👤 Username | 🔑 Password | 📌 Roles / Notes |
|:-----------:|:-----------:|:-----------------|
| `alice` | `123` | Active User (DM & Groups) |
| `bob` | `456` | Active User (DM & Groups) |
| `carl` | `789` | Active User (DM & Groups) |
| `don` | `101` | Active User (DM & Groups) |

> 💡 **Testing Tip**: Log into two accounts simultaneously (using two physical phones or Android App Clones) to experience real-time 2-way messaging, unread counter pills, online status changes, and background socket connectivity.

---

## 🛠️ Repository Structure

```
DiscordMini/
├── assets/                     # 🎨 App Logo & Showcase Screenshots
├── Discordappmin/              # 📱 Android Client (Kotlin + Jetpack Compose)
│   ├── app/
│   │   ├── src/main/java/com/example/discordappmin/
│   │   │   ├── network/        # SocketClient & SocketPacket Protocol
│   │   │   ├── repository/     # ChatRepository (IP Config, StateFlow & Login)
│   │   │   ├── service/        # SocketForegroundService (Background Keep-Alive & WakeLock)
│   │   │   └── ui/             # Jetpack Compose Screens & ViewModels
│   └── build.gradle.kts
│
└── DiscordMiniServer/          # 🖥️ Java TCP Backend Server (Java 17 + SQLite JDBC)
    ├── src/main/java/com/discordmini/server/
    │   ├── db/                 # DatabaseManager (SQLite JDBC CRUD)
    │   ├── handler/            # ClientHandler (NDJSON Protocol Parser & Auth)
    │   └── registry/           # SessionRegistry & GroupRegistry
    └── lib/                    # json.jar, sqlite.jar
```

---

## 💻 Building & Running from Source

### 1️⃣ Java Server Backend

```bash
cd DiscordMiniServer

# Compile Java server source
javac -d bin -cp "lib/*" $(find src -name "*.java")

# Launch server on TCP port 5000
java -cp "bin:lib/*" com.discordmini.server.ServerMain
```

### 2️⃣ Android Client

Open `Discordappmin` in Android Studio and build the project, or build directly via Gradle wrapper:

```bash
cd Discordappmin

# Build debug APK
./gradlew assembleDebug
```

### 3️⃣ Local USB ADB Reverse (Optional)

If running the backend locally instead of AWS EC2:

```bash
# Reverse TCP port 5000 from mobile device to PC
~/Android/Sdk/platform-tools/adb reverse tcp:5000 tcp:5000
```

---

## 📄 License

Distributed under the **MIT License**. See `LICENSE` for more information.

<div align="center">

Crafted with 💜 **Kotlin** & ☕ **Java** &nbsp;•&nbsp; **DiscordMini Monorepo**

</div>
