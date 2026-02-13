package com.chatflow.server.handler;

import org.java_websocket.WebSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionManager {
    
    private final Map<WebSocket, String> connections;
    private final AtomicInteger connectionCounter;

    public ConnectionManager() {
        this.connections = new ConcurrentHashMap<>();
        this.connectionCounter = new AtomicInteger(0);
    }

    public void addConnection(WebSocket conn, String roomId) {
        connections.put(conn, roomId);
        connectionCounter.incrementAndGet();
        System.out.println("New connection added. Room: " + roomId + 
                         ", Total active: " + connections.size());
    }

    public void removeConnection(WebSocket conn) {
        String roomId = connections.remove(conn);
        if (roomId != null) {
            System.out.println("Connection removed. Room: " + roomId + 
                             ", Total active: " + connections.size());
        }
    }

    public String getRoomId(WebSocket conn) {
        return connections.get(conn);
    }

    public int getActiveConnectionCount() {
        return connections.size();
    }

    public int getTotalConnectionCount() {
        return connectionCounter.get();
    }

    public boolean hasConnection(WebSocket conn) {
        return connections.containsKey(conn);
    }
}