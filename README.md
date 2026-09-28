<div align="center">

<br/>

<img src="./assets/logo.png" width="130" alt="DiscordMini Logo" />

# DiscordMini

### *"Real-time chat, built from scratch."*

[![Android](https://img.shields.io/badge/Android-Jetpack_Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Kotlin](https://img.shields.io/badge/Kotlin-JDK_17-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![SQLite](https://img.shields.io/badge/SQLite-Persistence-003B57?style=for-the-badge&logo=sqlite&logoColor=white)](https://www.sqlite.org/)
[![TCP Sockets](https://img.shields.io/badge/Protocol-TCP_Sockets-22C55E?style=for-the-badge)](https://en.wikipedia.org/wiki/Transmission_Control_Protocol)
[![License: MIT](https://img.shields.io/badge/License-MIT-F59E0B?style=for-the-badge)](LICENSE)

> A full-stack real-time chat application — **Android Client** + **Multi-threaded Java TCP Server** — built as a mini distributed systems project.

</div>

---

<br/>

<div align="center">

| <img src="./assets/chat.png" width="220"/> | <img src="./assets/chat_msg.png" width="220"/> | <img src="./assets/group.png" width="220"/> | <img src="./assets/group_msg.png" width="220"/> |
|:---:|:---:|:---:|:---:|
| **DM List** | **Direct Chat** | **Groups** | **Group Chat** |

</div>

<br/>

---

## 📖 Overview

**DiscordMini** is a lightweight clone of real-time messaging inspired by Discord, built entirely from scratch without Firebase or third-party messaging services. It uses a **custom JSON-over-TCP protocol** to deliver instant messages between users through a multi-threaded Java server.

The Android client is built with modern **Jetpack Compose + Material 3**, following clean MVVM architecture. The backend is a raw Java socket server with **SQLite persistence** — no frameworks, no shortcuts.

---

## 🏗️ System Architecture

```mermaid
graph TD
    subgraph Client["📱 Android Client (Discordappmin)"]
        UI["Jetpack Compose UI<br/>(Material 3)"] --> ViewModels["ViewModels & StateFlow<br/>(MVVM Architecture)"]
        ViewModels --> SocketClient["SocketClient<br/>(TCP Socket Worker)"]
    end

    SocketClient <== "Custom JSON-over-TCP<br/>(Port 5000 / ADB Reverse)" ==> ServerMain

    subgraph Server["🖥️ Backend Server (DiscordMiniServer)"]
        ServerMain["ServerMain<br/>(ServerSocket Listener)"] -->|Spawns Thread per Client| Handlers["ClientHandler Thread Pool"]
        Handlers --> Protocol["JSON Protocol Parser<br/>& Router"]
        Protocol --> DB["DatabaseManager<br/>(SQLite JDBC)"]
    end

    subgraph Storage["🗄️ Persistence"]
        DB --> SQLite[("SQLite DB<br/>discord_mini.db")]
    end

    classDef client fill:#1e1e2e,stroke:#89b4fa,stroke-width:2px,color:#cdd6f4;
    classDef server fill:#181825,stroke:#f9e2af,stroke-width:2px,color:#cdd6f4;
    classDef storage fill:#11111b,stroke:#a6e3a1,stroke-width:2px,color:#cdd6f4;
    class UI,ViewModels,SocketClient client;
    class ServerMain,Handlers,Protocol,DB server;
    class SQLite storage;
```

---

## ✨ Key Features

| Feature | Description |
|---------|-------------|
| 💬 **Direct Messaging** | One-to-one real-time chat between registered users. |
| 👥 **Group Channels** | Create and join group chats with multiple members instantly. |
| 🔐 **User Authentication** | Secure registration and login with server-side session management. |
| ⚡ **Custom TCP Protocol** | JSON-over-TCP for fast, lightweight real-time communication. |
| 🗄️ **SQLite Persistence** | All users, groups, and messages are persisted server-side. |
| 🎨 **Material 3 Dark UI** | Sleek dark-themed Android UI built fully in Jetpack Compose. |
| 🔀 **Multi-threaded Server** | Concurrent client connections handled with dedicated client handler threads. |

---

## 🏗️ Repository Structure

```
DiscordMini/
├── Discordappmin/              # 📱 Android Client (Kotlin + Jetpack Compose)
│   ├── app/
│   │   ├── ui/                 # Compose Screens & Components
│   │   ├── viewmodel/          # MVVM ViewModels + StateFlow
│   │   └── network/            # TCP Socket Client
│   └── build.gradle.kts
│
├── DiscordMiniServer/          # 🖥️ Backend Server (Java 17 + SQLite)
│   ├── src/
│   │   └── com/discordmini/
│   │       ├── server/         # ServerMain, ClientHandler
│   │       ├── db/             # SQLite Database layer
│   │       └── handlers/       # Message & protocol handlers
│   ├── lib/                    # json.jar, sqlite.jar
│   └── pom.xml
│
└── readme-images/              # 📸 App screenshots
```

---

## 🛠️ Tech Stack

### 📱 Android Client (`Discordappmin`)
- **Language**: Kotlin (JDK 17)
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + StateFlow
- **Networking**: Raw TCP Sockets (no Retrofit/Firebase)
- **Build**: Gradle Kotlin DSL

### 🖥️ Backend Server (`DiscordMiniServer`)
- **Language**: Java 17
- **Networking**: `ServerSocket` / `Socket` (raw TCP)
- **Protocol**: Custom JSON-over-TCP
- **Database**: SQLite via JDBC
- **Build**: Apache Maven

---

## 🚀 Getting Started

### Step 1 — Run the Backend Server

```bash
# Navigate to the server directory
cd DiscordMiniServer

# Compile (Maven)
mvn clean compile

# Run the server (logs output to /tmp/server.log)
nohup java -cp "target/classes:lib/json.jar:lib/sqlite.jar" \
  com.discordmini.server.ServerMain >> /tmp/server.log 2>&1 &
```

> The server listens on **port 5000** by default.

**Useful server commands:**

| Action | Command |
|--------|---------|
| ▶️ Start server | `nohup java -cp "target/classes:lib/json.jar:lib/sqlite.jar" com.discordmini.server.ServerMain >> /tmp/server.log 2>&1 &` |
| 📋 View logs | `cat /tmp/server.log` |
| 🔍 Check if running | `lsof -i :5000` |
| ⏹️ Stop server | `pkill -f ServerMain` |

---

### Step 2 — Connect Android Device via USB

To run the app on a **physical device** and connect it to your local server over USB:

```bash
# Forward the server port from your PC to the Android device
~/Android/Sdk/platform-tools/adb reverse tcp:5000 tcp:5000
```

> This maps `localhost:5000` on the Android device → your PC's server port `5000`.  
> Run this **after** plugging in your device and **before** launching the app.

**Then:**
1. Open `Discordappmin/` in **Android Studio**
2. Run `adb reverse` (command above)
3. Build & install the app on your device
4. The app will connect to your PC's server automatically via `127.0.0.1:5000`

---

### Step 3 — Login with a Test Account

> ⚠️ **New user registration is not available yet** — it will be added in a future update.  
> Use one of the pre-seeded accounts below to log in:

| 👤 Username | 🔑 Password |
|:-----------:|:-----------:|
| `alice` | `123` |
| `bob` | `456` |
| `carl` | `789` |
| `don` | `101` |

> 💡 You can run the app on **multiple devices / emulators** simultaneously using different accounts to test real-time messaging between users.

---

## 📸 App Screenshots

### Direct Messages

<div align="center">
  <img src="./assets/chat.png" alt="DM List Screen" width="40%" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="./assets/chat_msg.png" alt="Direct Chat Screen" width="40%" />
</div>

<br/>

### Group Chats

<div align="center">
  <img src="./assets/group.png" alt="Groups List Screen" width="40%" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="./assets/group_msg.png" alt="Group Chat Screen" width="40%" />
</div>

---

## 📄 License

This project is open-source under the **MIT License**.

---

<div align="center">

Built with ☕ Java & 💜 Kotlin &nbsp;·&nbsp; Distributed Systems Mini Project

</div>
