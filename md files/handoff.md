# Handoff Documentation — DiscordMini Project

## 1. Project Overview
- **Project Name**: DiscordMini (Distributed Chat Client & Java Socket Server)
- **Client Tech Stack**: Kotlin, Jetpack Compose, Material 3, Android Studio (`Discordappmin`)
- **Server Tech Stack**: Java TCP Socket Server, Maven, SQLite, Thread Pool (`DiscordMiniServer`)
- **Master Plans**: Refer to [plan.md](file:///home/pramodh/AndroidStudioProjects/Discordappmin/plan.md) and [ui.md](file:///home/pramodh/AndroidStudioProjects/Discordappmin/ui.md).

---

## 2. Completed Milestones

### Android Client UI (`Discordappmin`)
- [x] Enabled Jetpack Compose + Material 3 dependencies and Compose Compiler plugin.
- [x] Defined theme system ([Color.kt](file:///home/pramodh/AndroidStudioProjects/Discordappmin/app/src/main/java/com/example/discordappmin/ui/theme/Color.kt), [Theme.kt](file:///home/pramodh/AndroidStudioProjects/Discordappmin/app/src/main/java/com/example/discordappmin/ui/theme/Theme.kt)).
- [x] Created reusable components: `AppTopBar`, `AppTextField`, `OnlineStatusDot`, `ContactListItem`, `MessageBubble`.
- [x] Built interactive screens with mock ViewModels:
  - `LoginScreen`
  - `HomeScreen` (Direct Messages & Groups tabs)
  - `ChatScreen` (1:1 messaging)
  - `GroupChatScreen` (Group messaging with sender labels)
  - `CreateGroupScreen` (Multi-select contact list)
- [x] Configured `NavGraph.kt` and verified debug build (`./gradlew assembleDebug` passed cleanly).

### Java Server Setup (`DiscordMiniServer`)
- [x] Created project folder structure under `/home/pramodh/AndroidStudioProjects/DiscordMiniServer`.
- [x] Generated empty skeleton files for:
  - `pom.xml`, `README.md`
  - `ServerMain.java`
  - `handler/ClientHandler.java`
  - `registry/SessionRegistry.java`, `registry/GroupRegistry.java`
  - `db/DatabaseManager.java`
  - `model/ProtocolMessage.java`, `model/User.java`, `model/Group.java`
  - `utils/Logger.java`

---

## 3. Next Steps (Immediate Action Plan)

### Step 1: Implement `DiscordMiniServer` Core Logic
1. Populate `pom.xml` with dependencies (`org.json:json`, `org.xerial:sqlite-jdbc`).
2. Write `ProtocolMessage.java`, `User.java`, and `Group.java` data models.
3. Build `DatabaseManager.java` for SQLite persistence (tables: `users`, `messages`, `groups`, `group_members`, undelivered offline messages queue).
4. Implement `SessionRegistry.java` (concurrent user-to-socket map) and `GroupRegistry.java` (pub/sub group broadcast map).
5. Complete `ClientHandler.java` to handle NDJSON packet parsing (`LOGIN`, `PRIVATE_MSG`, `GROUP_MSG`, `PING`, `PRESENCE`).
6. Finalize `ServerMain.java` with `ServerSocket` listening on port `5000` and thread pool (`ExecutorService`).

### Step 2: Connect Android Client to Real Sockets
1. Create `ChatConnectionService.kt` (Android Foreground Service) running a socket read coroutine on `Dispatchers.IO`.
2. Implement `ChatRepository.kt` to expose real incoming messages via `StateFlow`.
3. Replace mock ViewModels in Android client with live socket networking.
4. Verify end-to-end user-to-user private and group message routing.
