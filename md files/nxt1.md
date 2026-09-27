# Implementation Plan: `nxt1.md` — DiscordMini Server & Integration

This document outlines the detailed step-by-step technical plan to complete the `DiscordMiniServer` backend and prepare it for seamless integration with the `Discordappmin` Android client.

---

## Phase 1: Java Server Backend Implementation (`DiscordMiniServer`)

### Step 1.1: Dependency & Model Configuration
- **Update `pom.xml`**:
  - Add `org.json:json` for JSON message serialization/deserialization.
  - Add `org.xerial:sqlite-jdbc` for local persistent storage.
  - Configure Maven compiler plugin for Java 17 / 21 compatibility.
- **Implement Data Models (`com.discordmini.server.model`)**:
  - `ProtocolMessage.java`: Structural JSON wrapper for packet types (`LOGIN`, `REGISTER`, `PRIVATE_MSG`, `GROUP_MSG`, `PRESENCE`, `CREATE_GROUP`, `ERROR`, `ACK`).
  - `User.java`: User entity representation (`id`, `username`, `passwordHash`, `status`, `lastSeen`).
  - `Group.java`: Group channel entity (`groupId`, `name`, `ownerId`, `memberUsernames`).

### Step 1.2: Persistence Layer (`com.discordmini.server.db`)
- **Implement `DatabaseManager.java`**:
  - Initialize SQLite DB (`discord_mini.db`).
  - Auto-create tables on startup:
    - `users`: `(username TEXT PRIMARY KEY, password TEXT, status TEXT)`
    - `messages`: `(id INTEGER PRIMARY KEY AUTOINCREMENT, sender TEXT, receiver TEXT, is_group INTEGER, content TEXT, timestamp INTEGER, delivered INTEGER)`
    - `groups`: `(group_id TEXT PRIMARY KEY, name TEXT, owner TEXT)`
    - `group_members`: `(group_id TEXT, username TEXT, PRIMARY KEY (group_id, username))`
  - Add CRUD methods for user auth, message archiving, group management, and offline message queue retrieval.

### Step 1.3: Concurrency & State Registries (`com.discordmini.server.registry`)
- **Implement `SessionRegistry.java`**:
  - Maintain thread-safe `ConcurrentHashMap<String, ClientHandler>` mapping authenticated usernames to active socket handlers.
  - Expose thread-safe register/unregister methods and socket lookup helper functions.
- **Implement `GroupRegistry.java`**:
  - Maintain pub/sub maps for broadcasting group messages to online members dynamically.

### Step 1.4: Protocol Handler & Socket Server (`com.discordmini.server.handler` & `ServerMain`)
- **Implement `ClientHandler.java`**:
  - Implement `Runnable` for execution in `ExecutorService`.
  - Process line-by-line NDJSON protocol frames over input/output streams.
  - Handle packet dispatching:
    - `REGISTER` / `LOGIN`: Validate credentials against SQLite DB & update online presence.
    - `PRIVATE_MSG`: Route directly to recipient's `ClientHandler` or queue in SQLite if recipient is offline.
    - `CREATE_GROUP` / `GROUP_MSG`: Persist group channels and fan out messages to active group members.
    - `PRESENCE`: Broadcast online/offline status updates to connected clients.
- **Implement `ServerMain.java`**:
  - Open `ServerSocket` on port `5000`.
  - Initialize fixed/cached thread pool (`Executors.newCachedThreadPool()`).
  - Accept incoming socket connections and submit `ClientHandler` instances to the pool.

---

## Phase 2: Verification & Testing

### Step 2.1: Unit & Integration Verification
- Build server with Maven (`mvn clean package`).
- Run `ServerMain` locally.
- Test TCP connection handling and NDJSON protocol exchange using `netcat` / `telnet` or mock TCP scripts.

---

## Phase 3: Android Client Integration (`Discordappmin`)

### Step 3.1: Network & Repository Layer
- Create Kotlin background socket connection manager (`ChatConnectionService.kt` / `SocketClient.kt`).
- Map incoming NDJSON strings to domain objects via Kotlin `StateFlow`.
- Connect Jetpack Compose UI ViewModels to real live socket data feeds.
