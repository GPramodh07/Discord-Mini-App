# Socket Implementation & Verification Plan (`socket_plan.md`)

This plan details the step-by-step technical approach for building the **Java Socket Backend (`DiscordMiniServer`)** and **Android TCP Client Layer (`Discordappmin`)**, aligned with the distributed system properties defined in [`properties_and_algorithms.md`](file:///home/pramodh/AndroidStudioProjects/properties_and_algorithms.md).

---

## 🎯 Architecture & Algorithmic Foundation

Based on [`properties_and_algorithms.md`](file:///home/pramodh/AndroidStudioProjects/properties_and_algorithms.md):
1. **Low Latency & Real-Time**: Persistent TCP Sockets (`java.net.Socket`, `ServerSocket` port `5000`) with NDJSON streaming.
2. **Concurrency**: `ExecutorService` thread pool + thread-safe collections (`ConcurrentHashMap` for sessions, `CopyOnWriteArraySet` / synchronized sets for channels).
3. **Point-to-Point Routing**: O(1) session lookups via `SessionRegistry` for 1:1 private messages (`PRIVATE_MSG`).
4. **Pub/Sub Fan-Out**: Group messaging broadcasting (`GROUP_MSG`) across members via `GroupRegistry`.
5. **Event-Driven Dispatch**: NDJSON packet parsing (`LOGIN`, `REGISTER`, `PRIVATE_MSG`, `GROUP_MSG`, `CREATE_GROUP`, `PRESENCE`) in `ClientHandler`.

---

## 📅 Step-by-Step Implementation Roadmap

### Phase 1: Java Server Core Implementation (`DiscordMiniServer`)

#### Step 1.1: Maven Dependencies (`pom.xml`)
- Add `org.json:json` (JSON parsing).
- Add `org.xerial:sqlite-jdbc` (SQLite persistence).
- Configure Maven compiler plugin (Java 17/21).

#### Step 1.2: Core Data Models (`com.discordmini.server.model`)
- `ProtocolMessage.java`: JSON serialization/deserialization for network packets.
- `User.java`: `username`, `status`, `lastSeen`.
- `Group.java`: `groupId`, `name`, `owner`, `members`.

#### Step 1.3: SQLite Persistence (`com.discordmini.server.db.DatabaseManager`)
- Initialize `discord_mini.db` on server launch.
- Create tables: `users`, `messages`, `groups`, `group_members`.
- Add methods for user auth, message archiving, offline queue, and group membership.

#### Step 1.4: State Registries (`com.discordmini.server.registry`)
- `SessionRegistry.java`: `ConcurrentHashMap<String, ClientHandler>` for O(1) DM routing and active session tracking.
- `GroupRegistry.java`: Pub/sub subscriber maps for fanning out group channel messages.

#### Step 1.5: Client Connection Handler (`com.discordmini.server.handler.ClientHandler`)
- Implement NDJSON packet loop on socket input stream.
- Event dispatcher for:
  - `REGISTER` / `LOGIN`: Authenticate & broadcast presence.
  - `PRIVATE_MSG`: Route via `SessionRegistry` O(1) lookup or save to offline DB queue.
  - `GROUP_MSG`: Fan-out via `GroupRegistry` pub/sub algorithm.
  - `CREATE_GROUP`: Register group & populate member maps.

#### Step 1.6: Server Entry Point (`ServerMain.java`)
- Bind `ServerSocket` to port `5000`.
- Initialize `ExecutorService` thread pool.
- Infinite accept loop spawning `ClientHandler` runnables.

---

## 🧪 Phase 2: Server Verification (Netcat / Telnet / Python Script)

### Step 2.1: Server Build & Startup
- Run `mvn clean package` inside `DiscordMiniServer`.
- Launch `ServerMain` on port 5000.

### Step 2.2: Connection & Protocol Verification
- **Test 1: User Registration & Login**
  - Connect via `nc localhost 5000`.
  - Send `{"type":"REGISTER","username":"alice","password":"123"}`.
  - Send `{"type":"LOGIN","username":"alice","password":"123"}`.
- **Test 2: Point-to-Point (DM) Routing**
  - Open 2 separate netcat terminals (`alice` & `bob`).
  - Send `PRIVATE_MSG` from `alice` to `bob`. Verify instant deliverability.
- **Test 3: Offline Queue**
  - Send message to offline user `charlie`. Log in `charlie` and verify queued messages arrive upon connection.
- **Test 4: Pub/Sub Group Broadcast**
  - Create group `{"type":"CREATE_GROUP","groupName":"devs","members":["alice","bob"]}`.
  - Broadcast `GROUP_MSG` and verify fan-out delivery.

---

## 📱 Phase 3: Android Client Socket Integration (`Discordappmin`)

### Step 3.1: Network Service & Repository Layer
- Create `SocketClient.kt` in Android client handling persistent TCP connection on background coroutine (`Dispatchers.IO`).
- Implement `ChatRepository.kt` exposing incoming messages as Kotlin `StateFlow` / `SharedFlow`.

### Step 3.2: Connect Jetpack Compose UI
- Wire `ChatViewModel`, `GroupChatViewModel`, `HomeViewModel`, and `LoginViewModel` to `ChatRepository`.
- Test real-time message exchange between Android app instance and Server.
