# DiscordMini — Distributed Chat App (Mini Project Plan)

**Course**: Distributed Systems / Distributed Computing
**Reference App**: Discord
**Simplified Target**: Android chat client + Java socket server, supporting 1:1 private chat and group chat
**Environment**: Linux laptop
**Time budget**: ~5–6 days (tight, so this plan is scoped deliberately small but "complete")

---

## 1. Why Discord, and what we're actually demonstrating

Discord's real architecture uses Erlang/Elixir gateway servers, WebSockets, Cassandra, Redis, etc. We are **not** rebuilding that — we're building a small system that demonstrates the *same distributed-systems concepts* Discord relies on, using raw sockets as the assignment requires.

### Concept → Book Mapping (Coulouris, 5th ed.)

| Distributed Concept | Where it shows up in our app | Coulouris Chapter |
|---|---|---|
| Client–Server architecture | Android clients ↔ Java server | Ch. 2 (System Models) |
| Inter-process communication (sockets) | Raw TCP sockets, custom protocol | Ch. 4 |
| Indirect communication (Pub/Sub) | Group chat = topic-based broadcast | Ch. 6 |
| Request-Reply (RPC-like) | Login, private message delivery | Ch. 5 |
| Naming / Discovery | In-memory user registry on server (online users, group membership) | Ch. 13 |
| Fault Tolerance | Heartbeat/ping-pong, reconnect logic, offline message queue | Ch. 15 |
| Consistency / Ordering | Per-conversation message sequence numbers (like Lamport-style ordering) | Ch. 14 |
| Concurrency control | Thread-per-client / thread pool on server | Ch. 2, 7 |
| Scalability | Discussed via thread-pool sizing + why real Discord shards by guild | Ch. 1 |

This mapping is exactly what you'll reuse in `ParadigmsandAlgorithms_RegNo.doc`.

---

## 2. Tech Stack Decision (client updated to Kotlin + Compose + Material 3; server stays Java)

| Layer | Choice | Why |
|---|---|---|
| Android language | **Kotlin** | Required for idiomatic Jetpack Compose. You know Java at intermediate level, so budget ~half a day to get comfortable with Kotlin syntax (`val`/`var`, data classes, lambdas, coroutines) before writing UI — see Day 0.5 below. |
| UI framework | **Jetpack Compose + Material 3** | Declarative UI, less boilerplate than XML+RecyclerView once you're past the learning curve, and Material 3 gives you a polished look with minimal styling work (dynamic color, `Scaffold`, `TopAppBar`, prebuilt components). |
| Networking (required) | **Raw TCP Sockets** (`java.net.Socket` / `ServerSocket`) | Assignment mandates Socket Programming — no Retrofit/gRPC/WebSocket libraries. Kotlin can call `java.net.*` directly, no extra dependency needed. |
| Async on client | **Kotlin Coroutines** (`viewModelScope`, `Dispatchers.IO`) | Natural fit for Compose; replaces manual `Thread`/`Handler`/`LocalBroadcastManager` plumbing. Socket read-loop runs as a coroutine on `Dispatchers.IO`, emits incoming messages via a `Flow`/`StateFlow` that Compose UI collects and recomposes on. |
| Wire protocol | **NDJSON** (newline-delimited JSON) over TCP | Simple to hand-parse, human-readable for screenshots/demo, matches your "JSON" node in the mind map. Use `kotlinx.serialization` (Kotlin-idiomatic, works cleanly with data classes) instead of `org.json`. |
| Server persistence | **SQLite** (via `sqlite-jdbc`) or **H2 embedded DB** | Zero setup, file-based, enough to persist users/messages/groups. Avoids installing Postgres/MySQL. |
| Client local cache | Keep it **in-memory** (`StateFlow<List<Message>>` held in a `ViewModel`) for v1 | Room DB is a nice-to-have but skip it if time is short — mention it as "future work" in your report. Compose + ViewModel already survives configuration changes, so you don't strictly need persistence for the demo. |
| Background connection handling | Android **Foreground Service** running the socket read-loop as a coroutine | Keeps the TCP connection alive independent of Activity/Compose lifecycle — good talking point for Minor III ("why not just open the socket inside a Composable?"). |
| Concurrency on server | `ExecutorService` thread pool (1 thread per client connection + shared broadcast logic) | Simple, textbook example of the "thread-per-client" server model discussed in Ch. 2/7. Server stays plain Java — no reason to touch it. |
| IDE | Android Studio (latest stable, has first-class Compose tooling: Live Edit, `@Preview`) | Standard, and previews save you emulator/device round-trips while designing screens. |
| Version control | Git + GitHub/GitLab repo | For your submission zip + to show commit history if asked. |

You will end up with **two separate projects**:
1. `DiscordMiniServer` — plain Java (Maven or just `javac`), runs on your laptop, exposes a `ServerSocket`. **Unchanged.**
2. `DiscordMiniClient` — Android Studio project, **Kotlin + Jetpack Compose + Material 3**, connects to server's IP:port.

---

## 3. System Architecture (put this diagram in ArchitectureOfApp doc)

```
                     ┌─────────────────────────────┐
                     │        SERVER (Java)         │
                     │                              │
   TCP:5000  ─────►  │  ServerSocket.accept()       │
                     │        │                     │
                     │  ┌─────▼──────┐   Thread Pool│
                     │  │ ClientHandler (per user)│  │
                     │  └─────┬──────┘             │
                     │        │                     │
                     │  ┌─────▼───────────────┐     │
                     │  │ Session Registry     │     │  (Naming/Discovery)
                     │  │ (userId -> socket)    │     │
                     │  ├───────────────────────┤     │
                     │  │ Group Registry        │     │  (Pub/Sub topics)
                     │  │ (groupId -> [userIds]) │     │
                     │  ├───────────────────────┤     │
                     │  │ SQLite DB             │     │  (Persistence)
                     │  │ users, messages, groups│     │
                     │  └───────────────────────┘     │
                     └──────────────┬───────────────┘
                                    │ TCP sockets (NDJSON)
              ┌─────────────────────┼─────────────────────┐
              ▼                     ▼                     ▼
     ┌────────────────┐   ┌────────────────┐    ┌────────────────┐
     │ Android Client A│   │ Android Client B│    │ Android Client C│
     │ (Foreground Svc │   │ (Foreground Svc │    │ (Foreground Svc │
     │  holds socket)  │   │  holds socket)  │    │  holds socket)  │
     └────────────────┘   └────────────────┘    └────────────────┘
```

### Message Protocol (define this precisely — it's your "RPC contract")

Every message is one JSON object + `\n` delimiter. Minimal message types:

```json
{"type":"LOGIN","user":"alice","pass":"..."}
{"type":"LOGIN_OK","userId":"u1","onlineUsers":["bob","carl"]}
{"type":"PRIVATE_MSG","from":"alice","to":"bob","text":"hi","ts":1732000000,"seq":1}
{"type":"GROUP_CREATE","name":"CS-Distributed","members":["alice","bob"]}
{"type":"GROUP_MSG","group":"CS-Distributed","from":"alice","text":"hello all","ts":..., "seq":5}
{"type":"PRESENCE","user":"bob","status":"ONLINE"}
{"type":"PING"} / {"type":"PONG"}
{"type":"ERROR","reason":"USER_OFFLINE"}
```

- `seq` per-conversation gives you ordering guarantees to discuss (FIFO ordering, Ch. 14).
- `PING`/`PONG` every ~15s = your fault-detection mechanism.
- If a `PRIVATE_MSG`/`GROUP_MSG` target is offline, server stores it in SQLite and delivers on next login → this is your "eventual consistency / offline queue" talking point.

---

## 4. Day-by-Day Plan

### Day 0 — Environment Setup (Linux)
- [ ] Install JDK 17 (`sudo apt install openjdk-17-jdk`), verify `java -version`
- [ ] Install Android Studio (download from official site, extract, run `studio.sh`, or `sudo snap install android-studio --classic`) — use a recent stable release (Compose tooling improves every release, so don't use an old cached version)
- [ ] In Android Studio SDK Manager: install SDK Platform (API 34), Android Emulator, an AVD (Pixel 6, API 34) — or use a physical Android phone via USB debugging (faster & more reliable than emulator for socket networking demos)
- [ ] Create new Android project `DiscordMiniClient` using the **"Empty Activity" (Compose)** template — Android Studio scaffolds Kotlin + Compose + Material 3 automatically, min SDK 24
- [ ] Create plain Java project `DiscordMiniServer` (use Maven or IntelliJ/VS Code — doesn't need Android Studio)
- [ ] Add `sqlite-jdbc` dependency to server (Maven `pom.xml`)
- [ ] Add `kotlinx.serialization` plugin + dependency to the client's `build.gradle.kts`
- [ ] Set up Git repo, `.gitignore` for both projects
- [ ] **Test connectivity early**: find your laptop's local IP (`ip a`), make sure phone/emulator and laptop are on same Wi-Fi/hotspot, and confirm you can `telnet <laptop-ip> 5000` from adb shell or phone. Do this on Day 0 — network/firewall issues eat the most time if left till later.

### Day 0.5 — Kotlin + Compose Crash Course (since you're coming from Java)
Don't skip this — an hour or two here saves a day of confusion later.
- [ ] Kotlin syntax delta from Java: `val`/`var`, no semicolons, data classes (`data class Message(val from: String, val text: String)`), null-safety (`?`, `?:`, `!!`), trailing lambdas, `when` instead of `switch`
- [ ] Coroutines basics: `suspend fun`, `viewModelScope.launch { }`, `Dispatchers.IO` for socket work, `Flow`/`StateFlow` for streaming incoming messages into the UI
- [ ] Compose basics: a `@Composable` function is a UI description, not a widget — it re-runs ("recomposes") when the `State` it reads changes. Learn `remember`, `mutableStateOf`, `collectAsState()`, `LazyColumn` (this replaces RecyclerView), `Scaffold`, `TopAppBar`
- [ ] Skim the official Jetpack Compose "Basics" codelab (developer.android.com) — it's short and hands-on, better than reading theory
- [ ] Architecture pattern: one `ViewModel` per screen holding a `StateFlow<UiState>`; Composables just observe and render it. This keeps your socket/networking code out of the UI layer entirely.

### Day 1 — Server Core
- [ ] `ServerSocket` accepting connections, `ExecutorService` thread pool
- [ ] `ClientHandler` (Runnable) per connection: read/write NDJSON lines
- [ ] In-memory `SessionRegistry` (ConcurrentHashMap<userId, Socket/Writer>)
- [ ] LOGIN / LOGIN_OK flow (no security yet — plaintext is fine for a class project, but *mention* in your report that you'd use TLS + hashed passwords in production)
- [ ] SQLite schema: `users(id, username, password_hash)`, `messages(id, from, to_user, to_group, text, ts, seq, delivered)`, `groups(id, name)`, `group_members(group_id, user_id)`
- [ ] PRIVATE_MSG routing (deliver if online, else persist)
- [ ] Basic console logging of all events (you'll screenshot this for the report)

### Day 2 — Server: Groups + Fault Tolerance
- [ ] GROUP_CREATE / GROUP_MSG (fan-out to all online members = pub/sub broadcast)
- [ ] PING/PONG heartbeat thread per client; if no PONG within timeout → mark OFFLINE, close socket, broadcast PRESENCE update
- [ ] Offline message queue: on LOGIN_OK, flush any undelivered messages from SQLite
- [ ] Graceful disconnect handling (client closes app / loses network)
- [ ] Write a tiny **command-line test client** (plain Java, no Android) to test server logic fast without waiting on Android builds

### Day 3 — Android Client: UI (Compose + Material 3)
- [ ] Set up navigation: `NavHost` with routes for Login, Home, Chat, Group Chat (use `androidx.navigation:navigation-compose`)
- [ ] Login/Register screen: `OutlinedTextField` for username/password, `Button`, wrapped in a `Scaffold`
- [ ] Home screen: `LazyColumn` of contacts (each row = online status dot + name, using Material 3 `ListItem`), a `TopAppBar` action or `TabRow` to switch to "Groups"
- [ ] Chat screen: `LazyColumn` of message bubbles (two `Composable`s — `SentBubble`/`ReceivedBubble` — aligned left/right via `Arrangement`), bottom `Row` with `TextField` + `IconButton` (send icon) pinned via `Scaffold`'s `bottomBar`
- [ ] Group creation: `AlertDialog` or a separate Compose screen with checkboxes (`Checkbox` per contact) to pick members
- [ ] Use Material 3 theming (`MaterialTheme`, `ColorScheme`) — the default generated theme is already presentable; tweak seed color if you want it to look distinct from a stock template
- [ ] Use `@Preview` annotations while building each screen so you're not redeploying to a device/emulator for every tweak — this is one of the big time-savers Compose gives you over XML

### Day 4 — Android Client: Networking (Coroutines + Foreground Service)
- [ ] `ChatConnectionService` (Foreground Service): on start, launches a coroutine on `Dispatchers.IO` that opens the `Socket` and loops reading NDJSON lines
- [ ] Expose incoming messages as a `SharedFlow`/`StateFlow` from a singleton `ChatRepository` (or via a bound-service interface) — ViewModels collect from this, so UI updates automatically without manual broadcasts
- [ ] Sending: a `Channel<String>` (coroutine-based queue) as the outbound buffer, drained by one dedicated writer coroutine — same producer-consumer pattern as before, just coroutine-native instead of `BlockingQueue`+`Thread`
- [ ] Wire up Login screen's ViewModel → service connect → LOGIN message → update `UiState` on `LOGIN_OK`
- [ ] Wire up Chat screen's ViewModel → send PRIVATE_MSG / GROUP_MSG, collect incoming ones into `StateFlow<List<Message>>` which `LazyColumn` renders reactively
- [ ] Handle reconnection: if socket drops, retry with exponential backoff inside the coroutine loop (`delay()` between attempts) — another fault-tolerance mechanism you can name explicitly
- [ ] Serialize/deserialize messages with `kotlinx.serialization` `Json.encodeToString()` / `decodeFromString()` against your `@Serializable data class` message models (mirror the JSON schema in Section 3)

### Day 5 — Integration, Testing, Polish
- [ ] End-to-end test: 2 phones/emulators + 1 laptop server — private chat both directions
- [ ] Test group chat with 3 clients
- [ ] Test fault tolerance: kill one client mid-chat, confirm others get PRESENCE=OFFLINE, confirm queued message delivered on reconnect
- [ ] Take all screenshots now (login, chat, group chat, server console logs showing multiple threads/clients, a screenshot of a disconnect being detected)
- [ ] Write installation/usage tutorial (README.md → also becomes your tutorial doc)
- [ ] Record a short demo or prepare a live-demo script for Minor III

### Day 6 (buffer) — Documentation + Packaging
- [ ] `ArchitectureOfApp_RegNo.doc` — how real Discord works (gateway, sharding, Elixir/Erlang actor model, Cassandra, Redis presence) — **this needs real research**, see Section 5 below
- [ ] `ParadigmsandAlgorithms_RegNo.doc` — use the table in Section 1, expand each row into a paragraph tying it to both Discord's real design and your implementation
- [ ] `RequirementSpecificationSimpleApp_RegNo.doc` — functional/non-functional requirements of your simplified app (use Section 2/3 of this plan as the base)
- [ ] Presentation (8–10 slides): Problem → Discord's real architecture → Distributed concepts identified → Your simplified design → Demo screenshots → Learnings
- [ ] Zip everything: `/src` (both projects), `/docs` (4 Word docs + tutorial), `/screenshots`, `/presentation.pptx`

---

## 5. Research To-Do (for the ArchitectureOfApp doc — needs actual sources)

Don't invent details about Discord's real backend from memory — pull from Discord's own engineering blog before writing that document. I can search these for you if you want:
- Discord engineering blog (discord.com/blog, "how discord..." posts) — they've written publicly about their Elixir gateway, moving from MongoDB→Cassandra→ScyllaDB, Rust services, Redis for presence
- Focus your summary on: (a) how a message travels client→gateway→fan-out to other clients, (b) how they handle millions of concurrent WebSocket connections (actor model / Erlang processes), (c) how they achieved horizontal scalability (sharding by guild/server ID)

---

## 6. Scope Guardrails (say no to these if time gets tight)

Cut these first if you're behind schedule — they're not needed to demonstrate the core distributed concepts:
- Voice/video chat (Discord's actual complex part — skip entirely)
- End-to-end encryption
- Rich media (images/files) — text-only chat is enough
- Push notifications when app is killed
- Multiple server instances / actual horizontal scaling (just *discuss* this conceptually in your docs — you don't need to implement multi-node sharding)

## 7. What to say if asked "why sockets and not gRPC/WebSocket libraries?"
Your assignment explicitly requires **Socket Programming** — so implementing the framing, serialization, and connection-management yourself (rather than a library abstracting it away) is the point: it's what makes the IPC/RPC/pub-sub chapters of the textbook tangible instead of theoretical.
