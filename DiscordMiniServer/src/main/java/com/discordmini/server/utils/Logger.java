package com.discordmini.server.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void info(String message) {
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] [INFO] " + message);
    }

    public static void warn(String message) {
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] [WARN] " + message);
    }

    public static void error(String message) {
        System.err.println("[" + LocalDateTime.now().format(formatter) + "] [ERROR] " + message);
    }

    public static void error(String message, Throwable t) {
        System.err.println("[" + LocalDateTime.now().format(formatter) + "] [ERROR] " + message);
        t.printStackTrace();
    }
}
