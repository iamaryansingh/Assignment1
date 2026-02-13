package com.chatflow.server.model;

public class ServerResponse {
    private String status;
    private ChatMessage originalMessage;
    private String serverTimestamp;
    private String roomId;

    public ServerResponse(String status, ChatMessage originalMessage, 
                         String serverTimestamp, String roomId) {
        this.status = status;
        this.originalMessage = originalMessage;
        this.serverTimestamp = serverTimestamp;
        this.roomId = roomId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ChatMessage getOriginalMessage() {
        return originalMessage;
    }

    public void setOriginalMessage(ChatMessage originalMessage) {
        this.originalMessage = originalMessage;
    }

    public String getServerTimestamp() {
        return serverTimestamp;
    }

    public void setServerTimestamp(String serverTimestamp) {
        this.serverTimestamp = serverTimestamp;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}