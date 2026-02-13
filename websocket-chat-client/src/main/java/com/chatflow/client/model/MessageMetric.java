package com.chatflow.client.model;

public class MessageMetric {
    private final long timestamp;
    private final String messageType;
    private final long latencyMs;
    private final boolean success;
    private final String roomId;

    public MessageMetric(long timestamp, String messageType, long latencyMs,
                        boolean success, String roomId) {
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.latencyMs = latencyMs;
        this.success = success;
        this.roomId = roomId;
    }

    public long getTimestamp() { return timestamp; }
    public String getMessageType() { return messageType; }
    public long getLatencyMs() { return latencyMs; }
    public boolean isSuccess() { return success; }
    public String getRoomId() { return roomId; }

    @Override
    public String toString() {
        return timestamp + "," + messageType + "," + latencyMs + "," +
               (success ? "200" : "500") + "," + roomId;
    }
}