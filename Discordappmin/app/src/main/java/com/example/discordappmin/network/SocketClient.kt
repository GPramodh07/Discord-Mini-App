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

    // Heartbeat timer to keep connection alive through NAT gateways on public internet
    // (Not needed for USB/localhost but harmless to keep running)
    private var heartbeatTimer: Timer? = null
    // Heartbeat interval in milliseconds (10 seconds)
    // Comment out or increase this value if reverting to USB config where heartbeat is unnecessary
    private const val HEARTBEAT_INTERVAL_MS = 10_000L

    private val _incomingPackets = MutableSharedFlow<SocketPacket>(extraBufferCapacity = 64)
    val incomingPackets: SharedFlow<SocketPacket> = _incomingPackets.asSharedFlow()

    suspend fun connect(host: String = "10.0.2.2", port: Int = 5000): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (isConnected) disconnect()
                
                val s = Socket()
                s.keepAlive = true
                s.tcpNoDelay = true
                s.connect(java.net.InetSocketAddress(host, port), 5000)
                socket = s
                writer = PrintWriter(s.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(s.getInputStream()))
                isConnected = true

                // Start listening thread
                Thread {
                    listenLoop()
                }.start()

                // Start heartbeat timer to prevent NAT idle timeout on public internet
                startHeartbeat()

                true
            } catch (e: Exception) {
                e.printStackTrace()
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
                                    android.util.Log.e("SocketClient", "Heartbeat failed, connection lost")
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
                        // Filter out PONG heartbeat responses — don't emit them as messages
                        if (packet.type == "PONG") {
                            android.util.Log.d("SocketClient", "Heartbeat PONG received")
                            continue
                        }
                        _incomingPackets.tryEmit(packet)
                    } catch (e: Exception) {
                        // Fallback for non-JSON lines (e.g. raw text from terminal)
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
            android.util.Log.e("SocketClient", "Listen loop error", e)
        } finally {
            android.util.Log.d("SocketClient", "Listening loop stopped")
            disconnect()
        }
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

    fun disconnect() {
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
    }
}
