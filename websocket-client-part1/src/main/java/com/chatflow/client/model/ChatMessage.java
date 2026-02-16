package com.chatflow.client.model;

public class ChatMessage {
    private String userId;
    private String username;
    private String message;
    private String timestamp;
    private String messageType;
    private String roomId;

    public ChatMessage(String userId, String username, String message,
                       String timestamp, String messageType, String roomId) {
        this.userId = userId;
        this.username = username;
        this.message = message;
        this.timestamp = timestamp;
        this.messageType = messageType;
        this.roomId = roomId;
    }

    // Getters
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getMessage() { return message; }
    public String getTimestamp() { return timestamp; }
    public String getMessageType() { return messageType; }
    public String getRoomId() { return roomId; }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", message='" + message + '\'' +
                ", roomId='" + roomId + '\'' +
                ", messageType='" + messageType + '\'' +
                '}';
    }
}