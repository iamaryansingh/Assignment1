package com.chatflow.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    
    private static final int WEBSOCKET_PORT = 8081;
    private static final int HEALTH_CHECK_PORT = 8081;

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("  ChatFlow WebSocket Server");
        System.out.println("===========================================");
        System.out.println();

        try {
            ChatWebSocketServer wsServer = new ChatWebSocketServer(WEBSOCKET_PORT);
            wsServer.start();
            
            Thread.sleep(1000);
            
            HealthCheckServer healthServer = new HealthCheckServer(HEALTH_CHECK_PORT, wsServer);
            healthServer.start();

            System.out.println();
            System.out.println("===========================================");
            System.out.println("  SERVER READY");
            System.out.println("===========================================");
            System.out.println("WebSocket endpoint: ws://localhost:" + WEBSOCKET_PORT + "/chat/{roomId}");
            System.out.println("Health check endpoint: http://localhost:" + HEALTH_CHECK_PORT + "/health");
            System.out.println();
            System.out.println("Press ENTER to stop the server...");
            System.out.println("===========================================");

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            reader.readLine();

            System.out.println("\nShutting down servers...");
            wsServer.stop(1000);
            healthServer.stop();
            
            System.out.println("Servers stopped. Goodbye!");

        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (InterruptedException e) {
            System.err.println("Server startup interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            System.exit(1);
        }
    }
}