package com.example.discordappmin.network

import org.json.JSONArray
import org.json.JSONObject

data class SocketPacket(
    val type: String,
    val sender: String? = null,
    val recipient: String? = null,
    val groupId: String? = null,
    val groupName: String? = null,
    val content: String? = null,
    val members: List<String>? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("type", type)
        sender?.let { json.put("sender", it) }
        recipient?.let { json.put("recipient", it) }
        groupId?.let { json.put("groupId", it) }
        groupName?.let { json.put("groupName", it) }
        content?.let { json.put("content", it) }
        members?.let { list ->
            val arr = JSONArray()
            list.forEach { arr.put(it) }
            json.put("members", arr)
        }
        json.put("timestamp", timestamp)
        return json.toString()
    }

    companion object {
        fun fromJsonString(jsonStr: String): SocketPacket {
            val json = JSONObject(jsonStr)
            val membersList = if (json.has("members")) {
                val arr = json.getJSONArray("members")
                val l = mutableListOf<String>()
                for (i in 0 until arr.length()) l.add(arr.getString(i))
                l
            } else null

            return SocketPacket(
                type = json.optString("type"),
                sender = if (json.has("sender")) json.getString("sender") else null,
                recipient = if (json.has("recipient")) json.getString("recipient") else null,
                groupId = if (json.has("groupId")) json.getString("groupId") else null,
                groupName = if (json.has("groupName")) json.getString("groupName") else null,
                content = if (json.has("content")) json.getString("content") else null,
                members = membersList,
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}
