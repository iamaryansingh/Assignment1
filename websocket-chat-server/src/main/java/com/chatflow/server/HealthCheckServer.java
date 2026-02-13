package com.chatflow.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class HealthCheckServer {
    
    private final HttpServer server;
    private final Gson gson;
    private final ChatWebSocketServer wsServer;

    public HealthCheckServer(int port, ChatWebSocketServer wsServer) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.gson = new Gson();
        this.wsServer = wsServer;
        
        server.createContext("/health", new HealthHandler());
        server.setExecutor(null);
        
        System.out.println("Health check server initialized on port " + port);
    }

    public void start() {
        server.start();
        System.out.println("Health check server started - accessible at /health");
    }

    public void stop() {
        server.stop(0);
        System.out.println("Health check server stopped");
    }

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "healthy");
                response.put("timestamp", Instant.now().toString());
                response.put("activeConnections", wsServer.getConnectionManager().getActiveConnectionCount());
                response.put("totalConnections", wsServer.getConnectionManager().getTotalConnectionCount());

                String jsonResponse = gson.toJson(response);

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();

                System.out.println("Health check performed - status: healthy");
            } else {
                String response = "Method not allowed. Use GET.";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }
}