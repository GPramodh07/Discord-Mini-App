package com.example.discordappmin.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.Timer
import java.util.TimerTask

object SocketClient {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    var isConnected = false
        private set

    // Store connection details for auto-reconnection
    private var lastHost: String = ""
    private var lastPort: Int = 5000

    // Store login credentials for auto re-login after reconnection
    var lastUsername: String = ""
        private set
    var lastPassword: String = ""
        private set

    // Heartbeat timer to keep connection alive through NAT gateways
    private var heartbeatTimer: Timer? = null
    private const val HEARTBEAT_INTERVAL_MS = 10_000L // 10 seconds

    // Auto-reconnection settings
    private var autoReconnectEnabled = true
    private var reconnecting = false
    private const val RECONNECT_DELAY_MS = 3_000L // 3 seconds between reconnect attempts
    private const val MAX_RECONNECT_ATTEMPTS = 10

    private val _incomingPackets = MutableSharedFlow<SocketPacket>(extraBufferCapacity = 64)
    val incomingPackets: SharedFlow<SocketPacket> = _incomingPackets.asSharedFlow()

    fun storeCredentials(username: String, password: String) {
        lastUsername = username
        lastPassword = password
    }

    suspend fun connect(host: String = "10.0.2.2", port: Int = 5000): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (isConnected) disconnect(skipReconnect = true)

                lastHost = host
                lastPort = port

                val s = Socket()
                s.keepAlive = true
                s.tcpNoDelay = true
                s.connect(java.net.InetSocketAddress(host, port), 5000)
                socket = s
                writer = PrintWriter(s.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(s.getInputStream()))
                isConnected = true
                reconnecting = false

                // Start listening thread
                Thread {
                    listenLoop()
                }.start()

                // Start heartbeat timer to prevent NAT idle timeout
                startHeartbeat()

                android.util.Log.d("SocketClient", "Connected to $host:$port")
                true
            } catch (e: Exception) {
                android.util.Log.e("SocketClient", "Connect failed to $host:$port", e)
                false
            }
        }
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatTimer = Timer("SocketHeartbeat", true).apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    try {
                        if (isConnected) {
                            val pingPacket = SocketPacket(type = "PING", sender = "client", content = "PING")
                            writer?.let {
                                it.println(pingPacket.toJsonString())
                                if (it.checkError()) {
                                    android.util.Log.e("SocketClient", "Heartbeat failed, triggering reconnect")
                                    disconnect()
                                }
                            }
                        } else {
                            stopHeartbeat()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SocketClient", "Heartbeat error", e)
                    }
                }
            }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS)
        }
    }

    private fun stopHeartbeat() {
        heartbeatTimer?.cancel()
        heartbeatTimer = null
    }

    private fun listenLoop() {
        try {
            android.util.Log.d("SocketClient", "Listening loop started")
            while (isConnected) {
                val line = reader?.readLine() ?: break
                if (line.isNotBlank()) {
                    android.util.Log.d("SocketClient", "RX: $line")
                    try {
                        val packet = SocketPacket.fromJsonString(line)
                        // Filter out PONG heartbeat responses
                        if (packet.type == "PONG") {
                            android.util.Log.d("SocketClient", "Heartbeat PONG received")
                            continue
                        }
                        _incomingPackets.tryEmit(packet)
                    } catch (e: Exception) {
                        val packet = SocketPacket(
                            type = "PRIVATE_MSG",
                            sender = "Server",
                            content = line
                        )
                        _incomingPackets.tryEmit(packet)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SocketClient", "Listen loop error: ${e.message}")
        } finally {
            android.util.Log.d("SocketClient", "Listening loop stopped")
            disconnect()
        }
    }

    /**
     * Auto-reconnection: runs in a background thread.
     * Attempts to reconnect to the last known host/port and re-login with stored credentials.
     */
    private fun attemptAutoReconnect() {
        if (!autoReconnectEnabled || reconnecting || lastHost.isBlank() || lastUsername.isBlank()) return
        reconnecting = true

        Thread {
            android.util.Log.d("SocketClient", "Auto-reconnect starting for user '$lastUsername' to $lastHost:$lastPort")
            var attempt = 0
            while (attempt < MAX_RECONNECT_ATTEMPTS && !isConnected && reconnecting) {
                attempt++
                android.util.Log.d("SocketClient", "Reconnect attempt $attempt/$MAX_RECONNECT_ATTEMPTS")
                try {
                    Thread.sleep(RECONNECT_DELAY_MS)

                    val s = Socket()
                    s.keepAlive = true
                    s.tcpNoDelay = true
                    s.connect(java.net.InetSocketAddress(lastHost, lastPort), 5000)
                    socket = s
                    writer = PrintWriter(s.getOutputStream(), true)
                    reader = BufferedReader(InputStreamReader(s.getInputStream()))
                    isConnected = true
                    reconnecting = false

                    android.util.Log.d("SocketClient", "Reconnected to $lastHost:$lastPort")

                    // Re-send LOGIN with stored credentials
                    val loginPacket = SocketPacket(type = "LOGIN", sender = lastUsername, content = lastPassword)
                    writer?.println(loginPacket.toJsonString())
                    android.util.Log.d("SocketClient", "Re-login sent for '$lastUsername'")

                    // Restart listening thread
                    Thread {
                        listenLoop()
                    }.start()

                    // Restart heartbeat
                    startHeartbeat()

                    // Ensure Foreground Service is active
                    try {
                        com.example.discordappmin.service.SocketForegroundService.startService(
                            com.example.discordappmin.DiscordApplication.instance,
                            lastUsername
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("SocketClient", "Error starting foreground service on reconnect", e)
                    }

                } catch (e: Exception) {
                    android.util.Log.e("SocketClient", "Reconnect attempt $attempt failed: ${e.message}")
                }
            }
            if (!isConnected) {
                android.util.Log.e("SocketClient", "Auto-reconnect gave up after $MAX_RECONNECT_ATTEMPTS attempts")
            }
            reconnecting = false
        }.start()
    }

    suspend fun sendPacket(packet: SocketPacket): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                writer?.let {
                    it.println(packet.toJsonString())
                    if (it.checkError()) {
                        android.util.Log.e("SocketClient", "PrintWriter error detected while sending packet")
                        disconnect()
                        false
                    } else {
                        true
                    }
                } ?: false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun disconnect(skipReconnect: Boolean = false) {
        val wasConnected = isConnected
        isConnected = false
        stopHeartbeat()
        try {
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        socket = null
        writer = null
        reader = null

        if (skipReconnect) {
            try {
                com.example.discordappmin.service.SocketForegroundService.stopService(
                    com.example.discordappmin.DiscordApplication.instance
                )
            } catch (e: Exception) {
                android.util.Log.e("SocketClient", "Error stopping foreground service", e)
            }
        }

        // Trigger auto-reconnect if this was an unexpected disconnect (not a manual disconnect)
        if (wasConnected && !skipReconnect) {
            android.util.Log.d("SocketClient", "Unexpected disconnect detected, attempting auto-reconnect...")
            attemptAutoReconnect()
        }
    }
}
