package com.discordmini.server.handler;

import com.discordmini.server.db.DatabaseManager;
import com.discordmini.server.model.Group;
import com.discordmini.server.model.ProtocolMessage;
import com.discordmini.server.registry.GroupRegistry;
import com.discordmini.server.registry.SessionRegistry;
import com.discordmini.server.utils.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String currentUsername;
    private boolean isRunning = true;
    private final DatabaseManager dbManager;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            Logger.info("Client connected from: " + socket.getRemoteSocketAddress());

            String line;
            while (isRunning && (line = in.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                processMessage(line);
            }
        } catch (IOException e) {
            Logger.warn("Connection error with client " + (currentUsername != null ? currentUsername : socket.getRemoteSocketAddress()) + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void processMessage(String rawJson) {
        try {
            Logger.info("RAW INCOMING (" + (currentUsername != null ? currentUsername : "Anonymous") + "): " + rawJson);
            ProtocolMessage message = ProtocolMessage.fromJsonString(rawJson);
            if (message.getType() == null) return;

            switch (message.getType()) {
                case "PING":
                    ProtocolMessage pong = new ProtocolMessage("PONG", "SERVER", currentUsername, "PONG");
                    sendMessage(pong);
                    break;
                case "REGISTER":
                    handleRegister(rawJson);
                    break;
                case "LOGIN":
                    handleLogin(rawJson);
                    break;
                case "PRIVATE_MSG":
                    handlePrivateMessage(message);
                    break;
                case "GROUP_MSG":
                    handleGroupMessage(message);
                    break;
                case "CREATE_GROUP":
                    handleCreateGroup(rawJson);
                    break;
                case "PRESENCE":
                    handlePresenceBroadcast(message);
                    break;
                case "DELETE_MSG":
                    handleDeleteMessage(message);
                    break;
                default:
                    sendError("Unknown packet type: " + message.getType());
            }
        } catch (Exception e) {
            Logger.error("Failed to parse incoming payload: " + rawJson, e);
            sendError("Invalid payload format");
        }
    }

    private void handleRegister(String rawJson) {
        JSONObject json = new JSONObject(rawJson);
        String username = json.optString("username");
        String password = json.optString("password");

        if (username.isEmpty() || password.isEmpty()) {
            sendError("Username and password required for registration");
            return;
        }

        boolean success = dbManager.registerUser(username, password);
        ProtocolMessage resp = new ProtocolMessage("ACK", "SERVER", username, success ? "REGISTER_SUCCESS" : "REGISTER_FAILED");
        sendMessage(resp);
    }

    private void handleLogin(String rawJson) {
        JSONObject json = new JSONObject(rawJson);
        // Android client sends LOGIN with 'sender' = username, 'content' = password
        // Fall back to 'username'/'password' fields for terminal-based clients
        String username = json.has("username") ? json.optString("username") : json.optString("sender");
        String password = json.has("password") ? json.optString("password") : json.optString("content");

        if (!dbManager.authenticateUser(username, password)) {
            sendError("Invalid username or password");
            return;
        }

        // OLD STRICT LOGIN CHECK (Commented out for AWS EC2 / Network Reconnect stability):
        /*
        if (SessionRegistry.isUserOnline(username)) {
            sendError("User is already logged in elsewhere");
            return;
        }
        */

        // NEW LOGIC: If user reconnects while an old stale socket exists, disconnect the old handler and accept the new one
        ClientHandler oldHandler = SessionRegistry.getSession(username);
        if (oldHandler != null && oldHandler != this) {
            Logger.info("Reconnecting user '" + username + "': replacing stale session.");
            oldHandler.currentUsername = null; // Prevent old handler cleanup from unregistering current session or broadcasting OFFLINE
            try { oldHandler.socket.close(); } catch (IOException ignored) {}
        }

        this.currentUsername = username;
        SessionRegistry.registerSession(username, this);
        dbManager.updateUserStatus(username, "ONLINE");

        ProtocolMessage resp = new ProtocolMessage("ACK", "SERVER", username, "LOGIN_SUCCESS");
        sendMessage(resp);

        // 1. Sync User's Joined Groups upon login
        List<Group> userGroups = dbManager.getUserGroups(username);
        for (Group g : userGroups) {
            GroupRegistry.registerGroup(g.getGroupId(), g.getMembers());
            ProtocolMessage groupNotif = new ProtocolMessage("CREATE_GROUP", g.getOwner(), null, g.getName());
            groupNotif.setGroupId(g.getGroupId());
            groupNotif.setMembers(new java.util.ArrayList<>(g.getMembers()));
            sendMessage(groupNotif);
        }

        // 2. Sync Full Past Chat History (1:1 & Group conversations)
        List<ProtocolMessage> history = dbManager.getChatHistoryForUser(username);
        for (ProtocolMessage msg : history) {
            sendMessage(msg);
        }
        dbManager.markMessagesDelivered(username);

        // Broadcast Presence to others
        broadcastPresence("ONLINE");

        // Send existing online users presence to newly logged in user
        for (ClientHandler activeHandler : SessionRegistry.getAllActiveSessions()) {
            if (activeHandler.currentUsername != null && !activeHandler.currentUsername.equals(username)) {
                ProtocolMessage existingPresence = new ProtocolMessage("PRESENCE", activeHandler.currentUsername, username, "ONLINE");
                sendMessage(existingPresence);
            }
        }
    }

    private void handlePrivateMessage(ProtocolMessage message) {
        if (currentUsername == null) {
            sendError("Must be logged in to send messages");
            return;
        }

        message.setSender(currentUsername);
        String recipient = message.getRecipient();

        ClientHandler recipientHandler = SessionRegistry.getSession(recipient);
        if (recipientHandler != null) {
            recipientHandler.sendMessage(message);
            dbManager.saveMessage(message, true);
        } else {
            // Save to offline message queue
            dbManager.saveMessage(message, false);
            ProtocolMessage infoMsg = new ProtocolMessage("ACK", "SERVER", currentUsername, "USER_OFFLINE_QUEUED");
            sendMessage(infoMsg);
        }
    }

    private void handleGroupMessage(ProtocolMessage message) {
        if (currentUsername == null) {
            sendError("Must be logged in to send group messages");
            return;
        }

        message.setSender(currentUsername);
        String groupId = message.getGroupId();

        Set<String> members = GroupRegistry.getGroupMembers(groupId);
        if (members.isEmpty()) {
            Group group = dbManager.getGroup(groupId);
            if (group != null) {
                GroupRegistry.registerGroup(groupId, group.getMembers());
                members = group.getMembers();
            }
        }

        dbManager.saveMessage(message, true);

        // Fan out to online members
        for (String member : members) {
            if (!member.equals(currentUsername)) {
                ClientHandler memberHandler = SessionRegistry.getSession(member);
                if (memberHandler != null) {
                    memberHandler.sendMessage(message);
                }
            }
        }
    }

    private void handleCreateGroup(String rawJson) {
        if (currentUsername == null) {
            sendError("Must be logged in to create group");
            return;
        }

        JSONObject json = new JSONObject(rawJson);
        String groupName = json.optString("groupName");
        JSONArray membersJson = json.optJSONArray("members");

        Set<String> members = new HashSet<>();
        members.add(currentUsername);
        if (membersJson != null) {
            for (int i = 0; i < membersJson.length(); i++) {
                members.add(membersJson.getString(i));
            }
        }

        String groupId = "group_" + System.currentTimeMillis();
        Group group = new Group(groupId, groupName, currentUsername, members);

        if (dbManager.createGroup(group)) {
            GroupRegistry.registerGroup(groupId, members);

            ProtocolMessage resp = new ProtocolMessage("ACK", "SERVER", currentUsername, "CREATE_GROUP_SUCCESS");
            resp.setGroupId(groupId);
            sendMessage(resp);

            // Broadcast CREATE_GROUP notification to all online members
            ProtocolMessage groupNotif = new ProtocolMessage("CREATE_GROUP", currentUsername, null, groupName);
            groupNotif.setGroupId(groupId);
            groupNotif.setMembers(new java.util.ArrayList<>(members));

            for (String member : members) {
                ClientHandler memberHandler = SessionRegistry.getSession(member);
                if (memberHandler != null) {
                    memberHandler.sendMessage(groupNotif);
                }
            }
        } else {
            sendError("Failed to create group");
        }
    }

    private void handlePresenceBroadcast(ProtocolMessage message) {
        broadcastPresence(message.getContent());
    }

    private void broadcastPresence(String status) {
        if (currentUsername == null) return;
        ProtocolMessage presenceMsg = new ProtocolMessage("PRESENCE", currentUsername, null, status);
        for (ClientHandler handler : SessionRegistry.getAllActiveSessions()) {
            if (!handler.currentUsername.equals(currentUsername)) {
                handler.sendMessage(presenceMsg);
            }
        }
    }

    public void sendMessage(ProtocolMessage message) {
        if (out != null) {
            out.println(message.toJsonString());
        }
    }

    private void sendError(String errorDetails) {
        ProtocolMessage err = new ProtocolMessage("ERROR", "SERVER", currentUsername, errorDetails);
        sendMessage(err);
    }

    private void handleDeleteMessage(ProtocolMessage message) {
        if (currentUsername == null) return;

        String msgId = message.getContent();
        dbManager.deleteMessage(msgId);

        message.setSender(currentUsername);

        if (message.getGroupId() != null && !message.getGroupId().isEmpty()) {
            Set<String> members = GroupRegistry.getGroupMembers(message.getGroupId());
            for (String member : members) {
                if (!member.equals(currentUsername)) {
                    ClientHandler memberHandler = SessionRegistry.getSession(member);
                    if (memberHandler != null) {
                        memberHandler.sendMessage(message);
                    }
                }
            }
        } else if (message.getRecipient() != null && !message.getRecipient().isEmpty()) {
            ClientHandler recipientHandler = SessionRegistry.getSession(message.getRecipient());
            if (recipientHandler != null) {
                recipientHandler.sendMessage(message);
            }
        }
    }

    private void cleanup() {
        isRunning = false;
        if (currentUsername != null) {
            ClientHandler activeHandler = SessionRegistry.getSession(currentUsername);
            if (activeHandler == this) {
                SessionRegistry.unregisterSession(currentUsername, this);
                dbManager.updateUserStatus(currentUsername, "OFFLINE");
                broadcastPresence("OFFLINE");
            } else {
                Logger.info("Stale socket closed for: " + currentUsername + " (active session preserved)");
            }
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            Logger.error("Error closing socket for: " + currentUsername, e);
        }
        Logger.info("Client disconnected and cleaned up: " + (currentUsername != null ? currentUsername : "Anonymous"));
    }
}
