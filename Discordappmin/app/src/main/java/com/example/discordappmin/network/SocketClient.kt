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

object SocketClient {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    var isConnected = false
        private set

    private val _incomingPackets = MutableSharedFlow<SocketPacket>(extraBufferCapacity = 64)
    val incomingPackets: SharedFlow<SocketPacket> = _incomingPackets.asSharedFlow()

    suspend fun connect(host: String = "10.0.2.2", port: Int = 5000): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (isConnected) disconnect()
                
                val s = Socket()
                s.connect(java.net.InetSocketAddress(host, port), 3000)
                socket = s
                writer = PrintWriter(s.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(s.getInputStream()))
                isConnected = true

                // Start listening thread
                Thread {
                    listenLoop()
                }.start()

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
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
                    it.flush()
                    true
                } ?: false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun disconnect() {
        isConnected = false
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
