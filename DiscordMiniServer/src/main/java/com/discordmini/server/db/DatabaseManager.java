package com.discordmini.server.db;

import com.discordmini.server.model.Group;
import com.discordmini.server.model.ProtocolMessage;
import com.discordmini.server.model.User;
import com.discordmini.server.utils.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:discord_mini.db";
    private static DatabaseManager instance;

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            Logger.error("SQLite JDBC Driver not found", e);
        }
        initDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void initDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Users Table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "username TEXT PRIMARY KEY, " +
                    "password TEXT NOT NULL, " +
                    "status TEXT DEFAULT 'OFFLINE', " +
                    "last_seen INTEGER)");

            // Messages Table (For archiving and offline storage)
            stmt.execute("CREATE TABLE IF NOT EXISTS messages (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "type TEXT NOT NULL, " +
                    "sender TEXT NOT NULL, " +
                    "recipient TEXT, " +
                    "group_id TEXT, " +
                    "content TEXT NOT NULL, " +
                    "timestamp INTEGER NOT NULL, " +
                    "delivered INTEGER DEFAULT 0)");

            // Groups Table
            stmt.execute("CREATE TABLE IF NOT EXISTS groups (" +
                    "group_id TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "owner TEXT NOT NULL)");

            // Group Members Table
            stmt.execute("CREATE TABLE IF NOT EXISTS group_members (" +
                    "group_id TEXT NOT NULL, " +
                    "username TEXT NOT NULL, " +
                    "PRIMARY KEY (group_id, username))");

            // Seed default users (alice, bob, carl, don) if not existing
            stmt.execute("INSERT OR IGNORE INTO users(username, password, status, last_seen) VALUES('alice', '123', 'OFFLINE', 0)");
            stmt.execute("INSERT OR IGNORE INTO users(username, password, status, last_seen) VALUES('bob', '456', 'OFFLINE', 0)");
            stmt.execute("INSERT OR IGNORE INTO users(username, password, status, last_seen) VALUES('carl', '789', 'OFFLINE', 0)");
            stmt.execute("INSERT OR IGNORE INTO users(username, password, status, last_seen) VALUES('don', '101', 'OFFLINE', 0)");

            Logger.info("SQLite Database initialized successfully.");
        } catch (SQLException e) {
            Logger.error("Failed to initialize SQLite Database", e);
        }
    }

    // --- User Auth & Management ---

    public boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users(username, password, status, last_seen) VALUES(?, ?, 'OFFLINE', ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setLong(3, System.currentTimeMillis());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            Logger.warn("User registration failed for username: " + username + " (" + e.getMessage() + ")");
            return false;
        }
    }

    public boolean authenticateUser(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("password").equals(password);
            }
        } catch (SQLException e) {
            Logger.error("Authentication error for user: " + username, e);
        }
        return false;
    }

    public void updateUserStatus(String username, String status) {
        String sql = "UPDATE users SET status = ?, last_seen = ? WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.setString(3, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Logger.error("Error updating status for user: " + username, e);
        }
    }

    // --- Message Archiving & Offline Queue ---

    public void saveMessage(ProtocolMessage msg, boolean delivered) {
        String sql = "INSERT INTO messages(type, sender, recipient, group_id, content, timestamp, delivered) VALUES(?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, msg.getType());
            pstmt.setString(2, msg.getSender());
            pstmt.setString(3, msg.getRecipient());
            pstmt.setString(4, msg.getGroupId());
            pstmt.setString(5, msg.getContent());
            pstmt.setLong(6, msg.getTimestamp());
            pstmt.setInt(7, delivered ? 1 : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Logger.error("Error saving message", e);
        }
    }

    public List<ProtocolMessage> getUndeliveredMessages(String username) {
        List<ProtocolMessage> list = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE recipient = ? AND delivered = 0 ORDER BY timestamp ASC";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ProtocolMessage msg = new ProtocolMessage();
                msg.setType(rs.getString("type"));
                msg.setSender(rs.getString("sender"));
                msg.setRecipient(rs.getString("recipient"));
                msg.setGroupId(rs.getString("group_id"));
                msg.setContent(rs.getString("content"));
                msg.setTimestamp(rs.getLong("timestamp"));
                list.add(msg);
            }
        } catch (SQLException e) {
            Logger.error("Error retrieving undelivered messages for: " + username, e);
        }
        return list;
    }

    public void markMessagesDelivered(String username) {
        String sql = "UPDATE messages SET delivered = 1 WHERE recipient = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Logger.error("Error marking messages delivered for: " + username, e);
        }
    }

    // --- Group Management ---

    public boolean createGroup(Group group) {
        String sqlGroup = "INSERT INTO groups(group_id, name, owner) VALUES(?, ?, ?)";
        String sqlMember = "INSERT INTO group_members(group_id, username) VALUES(?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmtG = conn.prepareStatement(sqlGroup);
                 PreparedStatement pstmtM = conn.prepareStatement(sqlMember)) {

                pstmtG.setString(1, group.getGroupId());
                pstmtG.setString(2, group.getName());
                pstmtG.setString(3, group.getOwner());
                pstmtG.executeUpdate();

                for (String member : group.getMembers()) {
                    pstmtM.setString(1, group.getGroupId());
                    pstmtM.setString(2, member);
                    pstmtM.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                Logger.error("Failed to create group: " + group.getName(), e);
            }
        } catch (SQLException e) {
            Logger.error("DB Connection error during group creation", e);
        }
        return false;
    }

    public Group getGroup(String groupId) {
        String sqlGroup = "SELECT * FROM groups WHERE group_id = ?";
        String sqlMembers = "SELECT username FROM group_members WHERE group_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmtG = conn.prepareStatement(sqlGroup);
             PreparedStatement pstmtM = conn.prepareStatement(sqlMembers)) {

            pstmtG.setString(1, groupId);
            ResultSet rsG = pstmtG.executeQuery();
            if (rsG.next()) {
                String name = rsG.getString("name");
                String owner = rsG.getString("owner");

                pstmtM.setString(1, groupId);
                ResultSet rsM = pstmtM.executeQuery();
                Set<String> members = new HashSet<>();
                while (rsM.next()) {
                    members.add(rsM.getString("username"));
                }
                return new Group(groupId, name, owner, members);
            }
        } catch (SQLException e) {
            Logger.error("Error retrieving group: " + groupId, e);
        }
        return null;
    }

    public List<Group> getUserGroups(String username) {
        List<Group> list = new ArrayList<>();
        String sql = "SELECT g.group_id, g.name, g.owner FROM groups g " +
                     "JOIN group_members gm ON g.group_id = gm.group_id WHERE gm.username = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String gId = rs.getString("group_id");
                String name = rs.getString("name");
                String owner = rs.getString("owner");
                Group g = getGroup(gId);
                if (g != null) {
                    list.add(g);
                }
            }
        } catch (SQLException e) {
            Logger.error("Error retrieving user groups for: " + username, e);
        }
        return list;
    }

    public List<ProtocolMessage> getChatHistoryForUser(String username) {
        List<ProtocolMessage> list = new ArrayList<>();
        // OLD UNFILTERED SQL QUERY (Commented out):
        /*
        String sql = "SELECT * FROM messages WHERE " +
                     "sender = ? OR recipient = ? OR " +
                     "group_id IN (SELECT group_id FROM group_members WHERE username = ?) " +
                     "ORDER BY timestamp ASC";
        */

        // NEW FILTERED SQL QUERY (Prevents 1:1 messages from mixing with group messages or leaking into unrelated chats):
        String sql = "SELECT * FROM messages WHERE " +
                     "(recipient = ? OR (sender = ? AND group_id IS NULL)) OR " +
                     "(group_id IS NOT NULL AND group_id IN (SELECT group_id FROM group_members WHERE username = ?)) " +
                     "ORDER BY timestamp ASC";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, username);
            pstmt.setString(3, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ProtocolMessage msg = new ProtocolMessage();
                msg.setType(rs.getString("type"));
                msg.setSender(rs.getString("sender"));
                msg.setRecipient(rs.getString("recipient"));
                msg.setGroupId(rs.getString("group_id"));
                msg.setContent(rs.getString("content"));
                msg.setTimestamp(rs.getLong("timestamp"));
                list.add(msg);
            }
        } catch (SQLException e) {
            Logger.error("Error retrieving chat history for: " + username, e);
        }
        return list;
    }

    public void deleteMessage(String msgIdOrTimestamp) {
        String sql = "DELETE FROM messages WHERE timestamp = ? OR content = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            long ts = 0;
            try { ts = Long.parseLong(msgIdOrTimestamp); } catch (NumberFormatException ignored) {}
            pstmt.setLong(1, ts);
            pstmt.setString(2, msgIdOrTimestamp);
            int count = pstmt.executeUpdate();
            Logger.info("Deleted " + count + " message(s) from DB matching: " + msgIdOrTimestamp);
        } catch (SQLException e) {
            Logger.error("Error deleting message: " + msgIdOrTimestamp, e);
        }
    }
}
