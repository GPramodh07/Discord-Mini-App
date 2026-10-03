package com.discordmini.server.registry;

import com.discordmini.server.handler.ClientHandler;
import com.discordmini.server.utils.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionRegistry {
    private static final Map<String, ClientHandler> activeSessions = new ConcurrentHashMap<>();

    public static boolean registerSession(String username, ClientHandler handler) {
        if (username == null || handler == null) return false;
        ClientHandler oldSession = activeSessions.put(username, handler);
        if (oldSession != null && oldSession != handler) {
            Logger.info("User '" + username + "' session replaced with new connection.");
        } else {
            Logger.info("User '" + username + "' registered in active sessions.");
        }
        return true;
    }

    public static void unregisterSession(String username, ClientHandler handler) {
        if (username != null && handler != null) {
            boolean removed = activeSessions.remove(username, handler);
            if (removed) {
                Logger.info("User '" + username + "' removed from active sessions.");
            } else {
                Logger.info("Stale connection closed for '" + username + "', active session retained.");
            }
        }
    }

    public static ClientHandler getSession(String username) {
        return username != null ? activeSessions.get(username) : null;
    }

    public static boolean isUserOnline(String username) {
        return username != null && activeSessions.containsKey(username);
    }

    public static Collection<ClientHandler> getAllActiveSessions() {
        return activeSessions.values();
    }
}
