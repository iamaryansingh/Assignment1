package com.chatflow.server.validation;

import com.chatflow.server.model.ChatMessage;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

public class MessageValidator {
    
    private static final int MIN_USER_ID = 1;
    private static final int MAX_USER_ID = 100000;
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 20;
    private static final int MIN_MESSAGE_LENGTH = 1;
    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final List<String> VALID_MESSAGE_TYPES = Arrays.asList("TEXT", "JOIN", "LEAVE");

    public static ValidationResult validate(ChatMessage message) {
        ValidationResult result = new ValidationResult();

        if (message == null) {
            result.addError("Message cannot be null");
            return result;
        }

        validateUserId(message.getUserId(), result);
        validateUsername(message.getUsername(), result);
        validateMessage(message.getMessage(), result);
        validateTimestamp(message.getTimestamp(), result);
        validateMessageType(message.getMessageType(), result);

        return result;
    }

    private static void validateUserId(String userId, ValidationResult result) {
        if (userId == null || userId.trim().isEmpty()) {
            result.addError("userId is required");
            return;
        }

        try {
            int id = Integer.parseInt(userId.trim());
            if (id < MIN_USER_ID || id > MAX_USER_ID) {
                result.addError("userId must be between " + MIN_USER_ID + " and " + MAX_USER_ID);
            }
        } catch (NumberFormatException e) {
            result.addError("userId must be a valid number");
        }
    }

    private static void validateUsername(String username, ValidationResult result) {
        if (username == null || username.trim().isEmpty()) {
            result.addError("username is required");
            return;
        }

        String trimmed = username.trim();
        
        if (trimmed.length() < MIN_USERNAME_LENGTH || trimmed.length() > MAX_USERNAME_LENGTH) {
            result.addError("username must be " + MIN_USERNAME_LENGTH + "-" + 
                          MAX_USERNAME_LENGTH + " characters");
            return;
        }

        if (!trimmed.matches("^[a-zA-Z0-9]+$")) {
            result.addError("username must contain only alphanumeric characters");
        }
    }

    private static void validateMessage(String message, ValidationResult result) {
        if (message == null) {
            result.addError("message is required");
            return;
        }

        if (message.length() < MIN_MESSAGE_LENGTH || message.length() > MAX_MESSAGE_LENGTH) {
            result.addError("message must be " + MIN_MESSAGE_LENGTH + "-" + 
                          MAX_MESSAGE_LENGTH + " characters");
        }
    }

    private static void validateTimestamp(String timestamp, ValidationResult result) {
        if (timestamp == null || timestamp.trim().isEmpty()) {
            result.addError("timestamp is required");
            return;
        }

        try {
            DateTimeFormatter.ISO_DATE_TIME.parse(timestamp.trim());
        } catch (DateTimeParseException e) {
            result.addError("timestamp must be in ISO-8601 format (e.g., 2026-02-10T15:30:00Z)");
        }
    }

    private static void validateMessageType(String messageType, ValidationResult result) {
        if (messageType == null || messageType.trim().isEmpty()) {
            result.addError("messageType is required");
            return;
        }

        String trimmed = messageType.trim().toUpperCase();
        if (!VALID_MESSAGE_TYPES.contains(trimmed)) {
            result.addError("messageType must be one of: TEXT, JOIN, LEAVE");
        }
    }
}