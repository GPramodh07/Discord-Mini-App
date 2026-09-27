# Troubleshooting: App Connects But Doesn't Receive Messages/Presence

**Symptom**: Login succeeds (LOGIN_OK received), but afterward the app never shows incoming `PRIVATE_MSG`, `GROUP_MSG`, or `PRESENCE` updates — even though two terminal-based test clients can talk to each other fine.

This means the one-shot request/response part of your protocol works, but the **long-lived, server-push part** does not. Below are the four most common causes, in order of likelihood, each with the fix.

---

## Cause 1: The read loop stops after login instead of running forever

### The bug
Code that looks like this reads exactly one line and then the function returns — it never goes back to check for more incoming data:
```kotlin
// WRONG — one-shot read, not a loop
fun login(username: String, password: String): JSONObject {
    writer.println(JSONObject().apply {
        put("type", "LOGIN"); put("username", username); put("password", password)
    })
    val responseLine = reader.readLine()   // reads LOGIN_OK ... and stops here forever
    return JSONObject(responseLine)
}
```
Anything the server sends *after* `LOGIN_OK` (a `PRESENCE` update, a `PRIVATE_MSG` from Bob) is sitting unread in the socket's input buffer, and nothing ever calls `readLine()` again to pick it up.

### The fix
Run **one continuous loop for the entire lifetime of the connection**, on a background thread/coroutine, dispatching every line by its `"type"` field:
```kotlin
suspend fun startReadLoop(onMessage: (JSONObject) -> Unit) = withContext(Dispatchers.IO) {
    while (isActive) {
        val line = reader.readLine() ?: break   // null = server closed the connection
        try {
            val json = JSONObject(line)
            withContext(Dispatchers.Main) { onMessage(json) }
        } catch (e: Exception) {
            Log.e("SocketClient", "Failed to parse line: $line", e)
        }
    }
    // loop exited -> connection is dead, trigger reconnect logic here
}
```
Login becomes: send the `LOGIN` json, then let this same loop's dispatcher route the `LOGIN_OK` (and every later message) to the right handler — it should **not** be a separate blocking call that returns a single value.

### How to confirm this was the bug
Add a log line at the very top of the loop body (`Log.d("SocketClient", "got line: $line")`). If you only ever see one log line (for `LOGIN_OK`) and nothing after, this is your problem.

---

## Cause 2: A new socket is opened per action instead of one persistent connection

### The bug
If "send message," "load contacts," or "check status" each do something like:
```kotlin
fun sendMessage(text: String) {
    val socket = Socket(HOST, PORT)   // <-- opens a brand-new connection every time
    ...
}
```
then:
- The server's `SessionRegistry` maps `userId → ClientHandler`, and it only knows about whichever socket connected **most recently** for that user. Every new socket triggers a new `ClientHandler`, and the old one (and its `userId` registration) is abandoned.
- Because you never stay connected, the server never has a live channel to push `PRESENCE`/`PRIVATE_MSG` to — there's nothing listening.
- Each new connection isn't logged in either (unless you re-send `LOGIN` every time, which would also spam duplicate `PRESENCE: ONLINE` broadcasts and confuse `SessionRegistry` with stale/duplicate entries).

### The fix
Open **exactly one `Socket`, once, right after successful login**, and keep it alive for as long as the user is logged in — owned by a single long-lived component (a Foreground Service, or a repository singleton), not re-created per screen or per button tap.

```kotlin
// SocketClient.kt — a singleton or service-owned instance, created ONCE
class SocketClient(private val host: String, private val port: Int) {
    private lateinit var socket: Socket
    private lateinit var reader: BufferedReader
    private lateinit var writer: PrintWriter
    private val outbound = Channel<String>(Channel.UNLIMITED)

    suspend fun connect() = withContext(Dispatchers.IO) {
        socket = Socket(host, port)
        reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        writer = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
    }

    fun send(json: JSONObject) { outbound.trySend(json.toString()) }

    suspend fun runWriteLoop() = withContext(Dispatchers.IO) {
        for (line in outbound) writer.println(line)
    }

    suspend fun runReadLoop(onMessage: (JSONObject) -> Unit) = withContext(Dispatchers.IO) {
        while (isActive) {
            val line = reader.readLine() ?: break
            withContext(Dispatchers.Main) { onMessage(JSONObject(line)) }
        }
    }
}
```
Every screen (`ChatViewModel`, `HomeViewModel`, etc.) calls `send(...)` on this **same shared instance** and observes incoming messages via a shared `StateFlow` fed by `runReadLoop`'s callback — nobody creates their own `Socket`.

### How to confirm this was the bug
On the server console, watch the connect log (`[connect] ...`) — if you see a new connection line every time you send a message from the app, sockets are being recreated instead of reused.

---

## Cause 3: Data is read correctly but never reaches the UI

### The bug
The read loop works and parses JSON fine, but the result is stored somewhere the UI doesn't observe — e.g. a plain `var` instead of a `StateFlow`, or a `StateFlow` that's created but never `collect`ed:
```kotlin
// WRONG — Compose has no way to know this changed
class HomeViewModel : ViewModel() {
    var onlineUsers: List<User> = emptyList()   // plain var, no observation possible
}
```
or the flow exists but the Composable takes a one-time snapshot instead of subscribing:
```kotlin
// WRONG — .value read once, not observed
Text("Online: ${viewModel.onlineUsersFlow.value.size}")
```

### The fix
Use `MutableStateFlow` in the ViewModel/repository, updated from the read loop's callback, and `collectAsState()` in the Composable so recomposition happens automatically on every update:
```kotlin
// Repository or ViewModel
private val _onlineUsers = MutableStateFlow<List<User>>(emptyList())
val onlineUsers: StateFlow<List<User>> = _onlineUsers

fun onPresenceMessage(json: JSONObject) {
    val userId = json.getString("userId")
    val status = json.getString("status")
    _onlineUsers.update { list ->
        list.map { if (it.id == userId) it.copy(isOnline = status == "ONLINE") else it }
    }
}
```
```kotlin
// Composable
val onlineUsers by homeViewModel.onlineUsers.collectAsState()
LazyColumn {
    items(onlineUsers) { user -> ContactListItem(user) }   // recomposes automatically when the flow updates
}
```

### How to confirm this was the bug
If your logs from Cause 1's fix *do* show `PRESENCE` lines arriving, but the screen still shows Bob as offline, the read path is fine and the break is purely in how state is stored/observed — go straight to this fix.

---

## Cause 4: Foreground Service isn't running, so the connection dies in the background

### The bug
If the socket + read loop live inside an Activity-scoped or plain `ViewModel`-scoped coroutine (`viewModelScope.launch { ... }`) instead of a Service, the coroutine — and the socket with it — gets cancelled or killed whenever:
- The Activity is destroyed/recreated (e.g. rotation, or Android reclaiming memory)
- The app is backgrounded for more than a few seconds (Android's OS-level battery/memory management can kill background work with no Foreground Service protecting it)

This can look exactly like Cause 1 or 2 from the outside (no messages arrive), but the actual root cause is that the whole connection got torn down, not that the loop logic is wrong.

### The fix
Move the socket lifecycle into a **Foreground Service** with a persistent notification, so Android treats it as user-visible ongoing work rather than background work it can kill freely:
```kotlin
class ChatConnectionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var socketClient: SocketClient

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildPersistentNotification())
        socketClient = SocketClient(HOST, PORT)
        scope.launch {
            socketClient.connect()
            launch { socketClient.runWriteLoop() }
            socketClient.runReadLoop { json -> ChatRepository.handleIncoming(json) }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```
Start it right after login (`startForegroundService(Intent(context, ChatConnectionService::class.java))`), and route all sends/receives through a singleton (`ChatRepository`) that the service and every ViewModel both reference — this is also what fixes Cause 2 at the same time, since a Foreground Service naturally gives you one long-lived socket owner instead of one per screen.

### How to confirm this was the bug
Log in, then switch away from the app (press Home) for 10–15 seconds, then send a message from the terminal client to the app's user. If messages work fine while the app is in the foreground but stop the moment you background it, this is your cause.

---

## Fastest way to diagnose which one you actually have

1. Add `Log.d("SocketClient", "RX: $line")` at the very top of your read loop body.
2. Log in as Alice in the app, Bob in a terminal.
3. From the terminal, send Alice a private message.
4. Check **both** logs:
   - **Server console**: did it log receiving the message and attempting to forward to Alice's `ClientHandler`?
     - No → the bug is in login/registration on the server side (not one of the 4 above — check `SessionRegistry.register()` is actually being called with Alice's real connection).
     - Yes → continue to next check.
   - **Android Logcat**: did `"RX: ..."` print at all for the forwarded message?
     - No log line at all → **Cause 1** (loop isn't running) or **Cause 4** (service/coroutine got killed) — check if the app was backgrounded.
     - Log line appears, but UI doesn't update → **Cause 3** (state not observed).
     - Multiple different sockets connecting per action in the server's connect log → **Cause 2**.

Fix them in this order (1 → 2 → 4 → 3) since 1 and 2 are the most common root causes and fixing either can make 3/4 symptoms disappear on their own.
