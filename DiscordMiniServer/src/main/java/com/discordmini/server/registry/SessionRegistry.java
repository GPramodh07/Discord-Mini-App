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
        ClientHandler existing = activeSessions.putIfAbsent(username, handler);
        if (existing == null) {
            Logger.info("User '" + username + "' registered in active sessions.");
            return true;
        } else {
            Logger.warn("Session register conflict: User '" + username + "' is already logged in.");
            return false;
        }
    }

    public static void unregisterSession(String username) {
        if (username != null) {
            activeSessions.remove(username);
            Logger.info("User '" + username + "' removed from active sessions.");
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
