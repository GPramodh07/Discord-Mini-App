# DiscordMini 🚀

A real-time messaging application featuring a modern **Android Client App** built with Jetpack Compose and a high-performance **Multi-Threaded Socket Server** backend in Java with SQLite persistence.

---

## 📁 Repository Structure

```
DiscordMini/
├── Discordappmin/         # Android Mobile App (Kotlin + Jetpack Compose)
│   ├── app/               # UI components, ViewModels, and TCP Socket Client
│   └── build.gradle.kts   # Android Gradle configuration
│
└── DiscordMiniServer/     # Backend Server (Java 17 + Maven + SQLite)
    ├── src/               # Multi-threaded TCP Server & Handlers
    ├── pom.xml            # Maven configuration
    └── README.md          # Server documentation
```

---

## ✨ Features

- **Real-Time Communication**: Custom JSON-over-TCP protocol with instant message delivery.
- **Modern Android UI**: Built entirely using Android Jetpack Compose & Material 3 design elements.
- **User Authentication**: Register & Login with secure password handling.
- **Multi-threaded Server**: Handles concurrent client connections seamlessly using client handlers.
- **Message & Channel Persistence**: Uses SQLite database for server-side state persistence.

---

## 🛠️ Tech Stack

### Client (`Discordappmin`)
- **Language**: Kotlin (JDK 17)
- **UI Framework**: Android Jetpack Compose + Material 3
- **Architecture**: MVVM (Model-View-ViewModel) + StateFlow
- **Build System**: Gradle Kotlin DSL

### Server (`DiscordMiniServer`)
- **Language**: Java 17
- **Networking**: Core Java Sockets (`ServerSocket`, `Socket`)
- **Database**: SQLite JDBC
- **Build System**: Apache Maven

---

## 🚀 Getting Started

### 1. Run the Backend Server
Navigating to the server directory and starting the server:

```bash
cd DiscordMiniServer
mvn clean compile exec:java
```
*The server will start listening for socket connections on the configured port (default: `8080`).*

### 2. Run the Android App
1. Open the `Discordappmin` folder in **Android Studio**.
2. Update the target IP address in the client configuration to match your backend host (e.g. `10.0.2.2` for Android Emulator or your server's local IP).
3. Build & Run on an Emulator or physical device:
```bash
cd Discordappmin
./gradlew installDebug
```

---

## 📄 License
This project is open-source under the MIT License.
