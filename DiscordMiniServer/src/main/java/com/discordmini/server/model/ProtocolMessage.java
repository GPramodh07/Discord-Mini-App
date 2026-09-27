package com.discordmini.server.model;

import java.util.List;
import org.json.JSONObject;

public class ProtocolMessage {
    private String type;       // LOGIN, REGISTER, PRIVATE_MSG, GROUP_MSG, CREATE_GROUP, PRESENCE, ERROR, ACK
    private String sender;
    private String recipient;
    private String groupId;
    private String content;
    private long timestamp;

    private List<String> members;

    public ProtocolMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public ProtocolMessage(String type, String sender, String recipient, String content) {
        this.type = type;
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String toJsonString() {
        JSONObject json = new JSONObject();
        if (type != null) json.put("type", type);
        if (sender != null) json.put("sender", sender);
        if (recipient != null) json.put("recipient", recipient);
        if (groupId != null) json.put("groupId", groupId);
        if (content != null) json.put("content", content);
        if (members != null && !members.isEmpty()) {
            org.json.JSONArray arr = new org.json.JSONArray();
            for (String m : members) arr.put(m);
            json.put("members", arr);
        }
        json.put("timestamp", timestamp);
        return json.toString();
    }

    public static ProtocolMessage fromJsonString(String jsonStr) {
        JSONObject json = new JSONObject(jsonStr);
        ProtocolMessage msg = new ProtocolMessage();
        if (json.has("type")) msg.setType(json.getString("type"));
        if (json.has("sender")) msg.setSender(json.getString("sender"));
        if (json.has("recipient")) msg.setRecipient(json.getString("recipient"));
        if (json.has("groupId")) msg.setGroupId(json.getString("groupId"));
        if (json.has("content")) msg.setContent(json.getString("content"));
        if (json.has("members")) {
            org.json.JSONArray arr = json.getJSONArray("members");
            java.util.List<String> mList = new java.util.ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                mList.add(arr.getString(i));
            }
            msg.setMembers(mList);
        }
        if (json.has("timestamp")) msg.setTimestamp(json.getLong("timestamp"));
        return msg;
    }
}
