package com.discordmini.server;

import com.discordmini.server.db.DatabaseManager;
import com.discordmini.server.handler.ClientHandler;
import com.discordmini.server.utils.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static final int PORT = 5000;

    public static void main(String[] args) {
        Logger.info("Starting DiscordMini TCP Socket Server...");

        // Initialize SQLite Database
        DatabaseManager.getInstance();

        // Executor service for handling client connection threads
        ExecutorService threadPool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            Logger.info("DiscordMini Server listening on port " + PORT + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                threadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            Logger.error("Server Socket Exception", e);
        } finally {
            threadPool.shutdown();
            Logger.info("Server stopped.");
        }
    }
}
