<div align="center">

<br/>

<img src="./assets/logo_no_bg.png" width="140" alt="DiscordMini Logo" />

# ⚡ DiscordMini

### *Real-Time Distributed Messaging App — Built from Scratch*

<br/>

[![Download APK](https://img.shields.io/badge/🚀_Download_APK-v1.0.0-22C55E?style=for-the-badge&logo=android&logoColor=white)](https://github.com/GPramodh07/Discord-Mini-App/releases/download/v1.0.0/DiscordMini_v1.0.apk)

<br/>

[![Android](https://img.shields.io/badge/Android-Jetpack_Compose-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![SQLite](https://img.shields.io/badge/SQLite-Database-003B57?style=for-the-badge&logo=sqlite&logoColor=white)](https://www.sqlite.org/)
[![AWS EC2](https://img.shields.io/badge/AWS-EC2_Deployed-FF9900?style=for-the-badge&logo=amazon-aws&logoColor=white)](https://aws.amazon.com/)
[![TCP Sockets](https://img.shields.io/badge/Networking-Raw_TCP_Sockets-007ACC?style=for-the-badge)](https://en.wikipedia.org/wiki/Transmission_Control_Protocol)
[![License: MIT](https://img.shields.io/badge/License-MIT-F59E0B?style=for-the-badge)](LICENSE)

<br/>

> **DiscordMini** is a full-stack real-time messaging ecosystem featuring a modern **Jetpack Compose Android Client** powered by an **Android Foreground Service** connected to a multithreaded **Java TCP Socket Backend** deployed on **AWS EC2**.

<br/>

<a href="https://github.com/GPramodh07/Discord-Mini-App/releases/download/v1.0.0/DiscordMini_v1.0.apk">
  <img src="https://img.shields.io/badge/⬇️_DOWNLOAD_LATEST_ANDROID_APK-Click_Here-007ACC?style=for-the-badge&logo=android&logoColor=white" width="380" height="50" alt="Download DiscordMini APK" />
</a>

</div>

---

## 📸 App Showcase

<div align="center">

| 💬 **Direct Messages** | 📩 **Private Chat** | 👥 **Group Channels** | 💬 **Group Chat** |
|:---:|:---:|:---:|:---:|
| <img src="./assets/chat.png" width="210" alt="DM List"/> | <img src="./assets/chat_msg.png" width="210" alt="Direct Chat"/> | <img src="./assets/group.png" width="210" alt="Groups List"/> | <img src="./assets/group_msg.png" width="210" alt="Group Chat"/> |

</div>

---

## ✨ Features Highlight

- 🚀 **Production Ready APK Release**: Download and run instantly on any Android device ([**Download v1.0.0 APK**](https://github.com/GPramodh07/Discord-Mini-App/releases/download/v1.0.0/DiscordMini_v1.0.apk)).
- ☁️ **AWS Cloud VPS Deployment**: Backend running live on AWS EC2 (`3.106.255.165:5000`) with fallback support for local USB ADB reverse debugging (`127.0.0.1`).
- 🔋 **Android Foreground Service & Battery Optimization**: Includes `SocketForegroundService` with a CPU `PARTIAL_WAKE_LOCK` to keep TCP sockets connected when minimized, locked, or running across Android App Clones.
- 💬 **Strict 1:1 DMs & Group Channels**: Direct messaging isolated cleanly from multi-user group chats.
- 🗑️ **Cross-Client Real-Time Delete**: Delete messages for everyone across all connected clients with SQLite DB synchronization.
- 🟢 **Dynamic Presence Broadcasts**: Live green/gray online status indicators for connected contacts.
- 🗄️ **SQLite Message & Group History Sync**: Automatically restores past conversations, group channels, and offline queued messages on login.
- 🔀 **Multithreaded Concurrent Server**: Built with custom NDJSON protocol and concurrent session/group registries (`SessionRegistry`, `GroupRegistry`).

---

## 🏗️ System Architecture

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

---

## 📲 Quick Download & Test Credentials

### 1️⃣ Download APK

Click the button below to download the compiled Android APK directly to your phone:

<div align="center">

<a href="https://github.com/GPramodh07/Discord-Mini-App/releases/download/v1.0.0/DiscordMini_v1.0.apk">
  <img src="https://img.shields.io/badge/⚡_DOWNLOAD_DISCORDMINI_APK_v1.0.0-22C55E?style=for-the-badge&logo=android&logoColor=white" height="48" alt="Download APK" />
</a>

</div>

### 2️⃣ Pre-Seeded Test Credentials

Log in with any of the pre-configured accounts:

| 👤 Username | 🔑 Password | 📌 Roles / Notes |
|:-----------:|:-----------:|:-----------------|
| `alice` | `123` | Active User (DM & Groups) |
| `bob` | `456` | Active User (DM & Groups) |
| `carl` | `789` | Active User (DM & Groups) |
| `don` | `101` | Active User (DM & Groups) |

> 💡 **Tip**: Log into two accounts simultaneously (using two phones or Phone App Clones) to test live 2-way real-time messaging, group creation, online presence, and background message delivery!

---

## 🛠️ Repository Structure

```
DiscordMini/
├── assets/                     # 🎨 App Logo & Showcase Screenshots
├── Discordappmin/              # 📱 Android Client (Kotlin + Jetpack Compose)
│   ├── app/
│   │   ├── src/main/java/com/example/discordappmin/
│   │   │   ├── network/        # SocketClient & SocketPacket
│   │   │   ├── repository/     # ChatRepository (IP Config & State)
│   │   │   ├── service/        # SocketForegroundService (Background Keep-Alive)
│   │   │   └── ui/             # Jetpack Compose Screens & ViewModels
│   └── build.gradle.kts
│
└── DiscordMiniServer/          # 🖥️ Backend Server (Java 17 + SQLite)
    ├── src/main/java/com/discordmini/server/
    │   ├── db/                 # DatabaseManager (SQLite JDBC)
    │   ├── handler/            # ClientHandler (NDJSON Protocol Parser)
    │   └── registry/           # SessionRegistry & GroupRegistry
    └── lib/                    # json.jar, sqlite.jar
```

---

## 💻 Building from Source

### Running the Java Server

```bash
cd DiscordMiniServer

# Compile server code
javac -d bin -cp "lib/*" $(find src -name "*.java")

# Run the server on port 5000
java -cp "bin:lib/*" com.discordmini.server.ServerMain
```

### Local USB ADB Debugging (Optional)

If running the backend locally instead of AWS EC2:

```bash
# Reverse TCP port 5000 from phone to PC
~/Android/Sdk/platform-tools/adb reverse tcp:5000 tcp:5000
```

---

## 📄 License

Distributed under the **MIT License**. See `LICENSE` for more information.

<div align="center">

Crafted with 💜 **Kotlin** & ☕ **Java** &nbsp;•&nbsp; **DiscordMini Monorepo**

</div>
