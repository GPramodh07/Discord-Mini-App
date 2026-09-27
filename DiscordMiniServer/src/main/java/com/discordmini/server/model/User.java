package com.discordmini.server.model;

public class User {
    private String username;
    private String password;
    private String status; // ONLINE, OFFLINE
    private long lastSeen;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.status = "OFFLINE";
        this.lastSeen = System.currentTimeMillis();
    }

    public User(String username, String password, String status, long lastSeen) {
        this.username = username;
        this.password = password;
        this.status = status;
        this.lastSeen = lastSeen;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
}
