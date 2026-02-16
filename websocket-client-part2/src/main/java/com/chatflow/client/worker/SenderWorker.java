package com.chatflow.client.worker;

import com.chatflow.client.metrics.MetricsCollector;
import com.chatflow.client.model.ChatMessage;
import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SenderWorker implements Runnable {
    
    private static final int MAX_RETRIES = 5;
    private static final int INITIAL_BACKOFF_MS = 100;
    private static final int RESPONSE_TIMEOUT_MS = 5000;
    
    private final int workerId;
    private final String serverUrl;
    private final BlockingQueue<ChatMessage> messageQueue;
    private final MetricsCollector metricsCollector;
    private final int messagesToSend;
    private final CountDownLatch completionLatch;
    private final Gson gson;
    
    // Connection pool
    private final java.util.Map<String, ConnectionInfo> connectionPool;

    public SenderWorker(int workerId, String serverUrl, 
                       BlockingQueue<ChatMessage> messageQueue,
                       MetricsCollector metricsCollector,
                       int messagesToSend,
                       CountDownLatch completionLatch) {
        this.workerId = workerId;
        this.serverUrl = serverUrl;
        this.messageQueue = messageQueue;
        this.metricsCollector = metricsCollector;
        this.messagesToSend = messagesToSend;
        this.completionLatch = completionLatch;
        this.gson = new Gson();
        this.connectionPool = new java.util.concurrent.ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        int messagesSent = 0;
        
        try {
            while (messagesSent < messagesToSend) {
                ChatMessage message = messageQueue.take();
                
                boolean success = sendMessageWithRetry(message);
                
                if (success) {
                    messagesSent++;
                } else {
                    metricsCollector.recordFailedMessage();
                }
                
                // Progress reporting
                if (messagesSent > 0 && messagesSent % 1000 == 0) {
                    System.out.println("Worker-" + workerId + " progress: " + messagesSent + " messages sent");
                }
            }
            
            System.out.println("Worker-" + workerId + " completed: " + messagesSent + " messages sent");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Worker-" + workerId + " interrupted");
        } finally {
            // Close all connections in pool
            closeAllConnections();
            completionLatch.countDown();
        }
    }

    private boolean sendMessageWithRetry(ChatMessage message) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    // To handel Exponential backoff
                    long backoffMs = INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt - 1);
                    System.out.println("Worker-" + workerId + 
                        " [RETRY " + attempt + "] Waiting " + backoffMs + "ms before retry...");
                    Thread.sleep(backoffMs);
                }
                
                boolean success = sendMessage(message);
                if (success) {
                    if (attempt > 0) {
                        System.out.println("Worker-" + workerId + 
                            " [SUCCESS] After " + attempt + " retries");
                    }
                    return true;
                }
                
            } catch (Exception e) {
                System.err.println("Worker-" + workerId + 
                    " [ATTEMPT " + (attempt + 1) + "/" + MAX_RETRIES + "] " + e.getMessage());
            }
        }
        
        System.err.println("Worker-" + workerId + 
            " [FAILED] Message failed after " + MAX_RETRIES + " retries");
        return false;
    }

    private boolean sendMessage(ChatMessage message) throws Exception {
        String roomId = message.getRoomId();
        
        // Get connection info
        ConnectionInfo connInfo = connectionPool.get(roomId);
        
        
        // Get or create connection (only if doesn't exist or closed)
        if (connInfo == null || connInfo.client == null || !connInfo.client.isOpen()) {
            WebSocketClient client = createConnection(roomId);
            if (client == null) {
                return false;
            }
            connInfo = new ConnectionInfo(client);
            connectionPool.put(roomId, connInfo);
        }
        
        // Send message
        long sendTime = System.currentTimeMillis();
        
        // Create a queue for this specific message's response
        BlockingQueue<ResponseData> responseQueue = new LinkedBlockingQueue<>(1);
        
        // Store the response queue
        connInfo.client.setAttachment(responseQueue);
        
        // Send message with connection drop detection
        String json = gson.toJson(message);
        try {
            connInfo.client.send(json);
        } catch (Exception e) {
            // Connection dropped during send - gracefully handle
            System.err.println("Worker-" + workerId + " connection dropped during send to " + roomId + ": " + e.getMessage());
            connectionPool.remove(roomId);
            try {
                if (connInfo.client != null && connInfo.client.isOpen()) {
                    connInfo.client.close();
                }
            } catch (Exception closeEx) {
                // Ignore close errors
            }
            return false;
        }
        
        // Increment message count for this connection
        connInfo.messageCount++;
        
        // Wait for response
        ResponseData response = responseQueue.poll(RESPONSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        
        if (response != null) {
            long latency = response.receiveTime - sendTime;
            metricsCollector.recordMessage(sendTime, message.getMessageType(),
                                          latency, response.success, message.getRoomId());
            return response.success;
        } else {
            // Timeout - treat as connection drop, gracefully handle
            System.err.println("Worker-" + workerId + " timeout on room " + roomId + 
                             " - treating as connection drop (waited " + RESPONSE_TIMEOUT_MS + "ms)");
            connectionPool.remove(roomId);
            try {
                if (connInfo.client != null && connInfo.client.isOpen()) {
                    connInfo.client.close();
                }
            } catch (Exception e) {
            }
            return false;
        }
    }

    private WebSocketClient createConnection(String roomId) {
        try {
            String url = serverUrl + roomId;
            WebSocketClient client = new ReusableWebSocketClient(new URI(url));
            
            if (client.connectBlocking(5, TimeUnit.SECONDS)) {
                metricsCollector.recordConnection();
                return client;
            } else {
                return null;
            }
        } catch (Exception e) {
            System.err.println("Worker-" + workerId + " failed to connect to " + roomId + ": " + e.getMessage());
            return null;
        }
    }

    private void closeAllConnections() {
        for (ConnectionInfo connInfo : connectionPool.values()) {
            try {
                if (connInfo.client != null && connInfo.client.isOpen()) {
                    connInfo.client.close();
                }
            } catch (Exception e) {
            }
        }
        connectionPool.clear();
    }

    // Holder for connection and its message count
    private static class ConnectionInfo {
        WebSocketClient client;
        int messageCount;
        
        ConnectionInfo(WebSocketClient client) {
            this.client = client;
            this.messageCount = 0;
        }
    }

    // Inner class for reusable WebSocket client
    private class ReusableWebSocketClient extends WebSocketClient {
        
        public ReusableWebSocketClient(URI serverUri) {
            super(serverUri);
            setConnectionLostTimeout(0);
        }

        @Override
        public void onOpen(ServerHandshake handshake) {
            // Connection opened
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onMessage(String response) {
            long receiveTime = System.currentTimeMillis();
            
            // Get the response queue from attachment
            BlockingQueue<ResponseData> responseQueue = 
                (BlockingQueue<ResponseData>) this.getAttachment();
            
            if (responseQueue != null) {
                boolean success = response.contains("\"status\":\"success\"");
                ResponseData data = new ResponseData(receiveTime, success);
                
                try {
                    responseQueue.offer(data, 100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            // Connection closed - will reconnect if needed
        }

        @Override
        public void onError(Exception ex) {
            // Errors are handled at retry level
        }
    }

    // Helper class to hold response data
    private static class ResponseData {
        final long receiveTime;
        final boolean success;
        
        ResponseData(long receiveTime, boolean success) {
            this.receiveTime = receiveTime;
            this.success = success;
        }
    }
}