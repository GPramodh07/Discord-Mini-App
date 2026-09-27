# Properties and Algorithms in Mini-Discord (Java Implementation)

This document provides a detailed technical breakdown of the distributed system properties, algorithmic paradigms, and how they map directly to the Java Socket mini-project implementation.

## Part 1: Distributed System Properties

### 1. Low Latency & Real-Time Processing
*   **Theory / Discord's Context:** Discord relies on instant, bidirectional message delivery to maintain seamless conversations. Real-time processing guarantees that event payloads are delivered across network streams with minimal delay, rather than waiting for batch processing or polling.
*   **Where It Is Used in the App Code:**
    *   **Persistent TCP Sockets:** Implemented using `java.net.Socket` and `java.net.ServerSocket`. Once established, the stream stays open, avoiding the overhead of repeatedly setting up HTTP connections.
    *   **Asynchronous Client I/O:** Inside `DiscordClient.java`, incoming server messages are read asynchronously on a separate background daemon thread (`receiveThread`). This prevents the user's typing interface from freezing while waiting for incoming messages.

### 2. Concurrency & High Throughput
*   **Theory / Discord's Context:** Systems like Discord handle millions of concurrent text, audio, and presence updates without blocking system threads or degrading network throughput. Concurrency allows the system to execute multiple tasks or process multiple user connections simultaneously by sharing CPU resources efficiently.
*   **Where It Is Used in the App Code:**
    *   **Thread-per-Client Concurrency Pattern:** `DiscordServer.java` spawns an independent worker thread (`ClientHandler`) for every newly accepted socket connection inside an infinite accept loop (`new Thread(handler).start();`).
    *   **Non-Blocking Thread-Safe Data Structures:** The app uses `ConcurrentHashMap` for user sessions and `CopyOnWriteArraySet` for channel memberships. These handle high-throughput concurrent reads and writes across active threads without requiring coarse-grained synchronization locks, preventing `ConcurrentModificationException`.

### 3. Fault Tolerance & Graceful Disconnection
*   **Theory / Discord's Context:** Client crashes, network drops, or client-side power failures should never trigger cascading failures on server nodes or affect neighboring channels. Fault tolerance guarantees that a system continues operating correctly even when individual components fail.
*   **Where It Is Used in the App Code:**
    *   **Exception Handling & Cleanup:** In `DiscordServer.java`, the `ClientHandler` thread wraps connection loops inside `try-catch-finally` blocks.
    *   When a client disconnects unexpectedly or encounters a socket read error (`IOException`), the server catches the failure, removes user references from the global thread-safe maps, notifies remaining channel members, and gracefully releases the socket resources in the `finally` block.

---

## Part 2: Distributed Algorithms & Paradigms

### 1. Publish-Subscribe (Pub/Sub) Fan-Out Algorithm
*   **Algorithm Concept:** Publishers send event payloads to logical "topic" channels without needing to know who or how many users are currently listening (individual subscribers). The system manages subscriber lists and "fans out" incoming payloads to all matching consumers.
*   **Where It Is Used in the App Code:**
    *   Used for multi-user group messaging (e.g., `#general`, `#random`, or custom channels).
    *   In the `broadcastToChannel()` method, the channel name acts as the topic. The server retrieves the set of subscriber usernames via `channels.get(channelName)` and iterates through it, fanning out the payload through each subscriber's output socket writer stream.

### 2. Point-to-Point Direct Message (DM) Routing via O(1) Hash Lookups
*   **Algorithm Concept:** Direct point-to-point routing uses unique key-value lookup functions to resolve host address identifiers in constant time—O(1)—avoiding network broadcasts or linear O(N) searches across the network.
*   **Where It Is Used in the App Code:**
    *   Used when processing private 1-on-1 messages (`/dm <username> <message>`).
    *   In the `sendDirectMessage()` method, the server executes `clients.get(targetUsername)` to perform an O(1) hash table lookup directly on the target user's socket stream handler, sending the message precisely to that user without broadcasting.

### 3. Event-Driven Message Parsing & Dispatching Algorithm
*   **Algorithm Concept:** An event loop sequentially evaluates stream inputs, inspects protocol prefixes (headers/commands), and dispatches control flow to specific handler methods based on defined parsing logic.
*   **Where It Is Used in the App Code:**
    *   Inside `ClientHandler.run()`, the server continuously listens to incoming lines from `BufferedReader.readLine()`. The incoming text acts as an event payload.
    *   The dispatcher routes logic based on prefixes:
        *   Prefix `/join` $\rightarrow$ Invokes Channel Switch & Subscription Event
        *   Prefix `/dm` $\rightarrow$ Invokes Direct Routing Event
        *   Equals `/users` $\rightarrow$ Invokes Presence State Inspection Event
        *   Equals `/exit` $\rightarrow$ Invokes Disconnection & Teardown Event
        *   Regular Text $\rightarrow$ Invokes Channel Broadcast (Pub/Sub) Event