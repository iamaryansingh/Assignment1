package com.chatflow.server;

import com.chatflow.server.handler.ConnectionManager;
import com.chatflow.server.model.ChatMessage;
import com.chatflow.server.model.ErrorResponse;
import com.chatflow.server.model.ServerResponse;
import com.chatflow.server.validation.MessageValidator;
import com.chatflow.server.validation.ValidationResult;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Collections;

public class ChatWebSocketServer extends WebSocketServer {

    private final Gson gson;
    private final ConnectionManager connectionManager;

    public ChatWebSocketServer(int port) {
        super(new InetSocketAddress(port));
        this.gson = new Gson();
        this.connectionManager = new ConnectionManager();
        System.out.println("WebSocket server initialized on port " + port);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String uri = handshake.getResourceDescriptor();
        String roomId = extractRoomId(uri);
        
        if (roomId == null) {
            System.err.println("Invalid connection attempt - no room ID in URI: " + uri);
            conn.close(1003, "Invalid URI - expected /chat/{roomId}");
            return;
        }

        connectionManager.addConnection(conn, roomId);
        System.out.println("WebSocket opened: " + conn.getRemoteSocketAddress() + 
                         " | Room: " + roomId);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connectionManager.removeConnection(conn);
        System.out.println("WebSocket closed: " + conn.getRemoteSocketAddress() + 
                         " | Code: " + code + " | Reason: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        String roomId = connectionManager.getRoomId(conn);
        System.out.println("Message received from room " + roomId + ": " + message);

        try {
            ChatMessage chatMessage = gson.fromJson(message, ChatMessage.class);
            ValidationResult validation = MessageValidator.validate(chatMessage);
            
            if (validation.isValid()) {
                sendSuccessResponse(conn, chatMessage, roomId);
            } else {
                sendErrorResponse(conn, validation);
            }
            
        } catch (JsonSyntaxException e) {
            System.err.println("Invalid JSON received: " + e.getMessage());
            sendErrorResponse(conn, "Invalid JSON format: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(conn, "Internal server error");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
        ex.printStackTrace();
        
        if (conn != null) {
            connectionManager.removeConnection(conn);
        }
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started successfully!");
        System.out.println("Listening for connections on port " + getPort());
        setConnectionLostTimeout(0);  // ✅ Disables the buggy ping/pong mechanism
        System.out.println("Connection lost timeout disabled (prevents library bug)");
    }

    private String extractRoomId(String uri) {
        if (uri == null || !uri.startsWith("/chat/")) {
            return null;
        }
        
        String[] parts = uri.split("/");
        if (parts.length >= 3) {
            return parts[2];
        }
        
        return null;
    }

    private void sendSuccessResponse(WebSocket conn, ChatMessage message, String roomId) {
        String serverTimestamp = Instant.now().toString();
        ServerResponse response = new ServerResponse("success", message, serverTimestamp, roomId);
        String jsonResponse = gson.toJson(response);
        
        conn.send(jsonResponse);
        System.out.println("Success response sent to room " + roomId);
    }

    private void sendErrorResponse(WebSocket conn, ValidationResult validation) {
        String serverTimestamp = Instant.now().toString();
        ErrorResponse response = new ErrorResponse("error", validation.getErrors(), serverTimestamp);
        String jsonResponse = gson.toJson(response);
        
        conn.send(jsonResponse);
        System.out.println("Error response sent: " + validation.getErrors());
    }

    private void sendErrorResponse(WebSocket conn, String errorMessage) {
        String serverTimestamp = Instant.now().toString();
        ErrorResponse response = new ErrorResponse("error", 
                                                   Collections.singletonList(errorMessage), 
                                                   serverTimestamp);
        String jsonResponse = gson.toJson(response);
        
        conn.send(jsonResponse);
        System.out.println("Error response sent: " + errorMessage);
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }
}