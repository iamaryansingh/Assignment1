package com.chatflow.client.generator;

import com.chatflow.client.model.ChatMessage;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class MessageGenerator implements Runnable {
    
    private static final String[] MESSAGES = {
        "Hello everyone!", "How are you doing?", "Great to be here",
        "Anyone online?", "Let's discuss the project", "What's up?",
        "Good morning!", "Have a great day!", "See you later",
        "Thanks for the help", "Interesting point", "I agree",
        "That makes sense", "Can you clarify?", "Great idea!",
        "Let me think about it", "Sounds good", "Perfect!",
        "I'm working on it", "Almost done", "Just finished",
        "Need some help here", "Can someone assist?", "Thanks!",
        "Appreciate it", "No problem", "You're welcome",
        "Sure thing", "Definitely", "Absolutely",
        "I understand", "Got it", "Makes sense",
        "Cool", "Awesome", "Nice",
        "Excellent work", "Well done", "Keep it up",
        "Looking forward", "See you soon", "Take care",
        "Bye for now", "Catch you later", "Peace out",
        "Have a good one", "Until next time", "Cheers",
        "All the best", "Good luck", "You too" 
    };

    private final BlockingQueue<ChatMessage> messageQueue;
    private final int totalMessages;
    private final Random random;

    public MessageGenerator(BlockingQueue<ChatMessage> messageQueue, int totalMessages) {
        this.messageQueue = messageQueue;
        this.totalMessages = totalMessages;
        this.random = new Random();
    }

    @Override
    public void run() {
        System.out.println("MessageGenerator started - generating " + totalMessages + " messages...");
        
        try {
            for (int i = 0; i < totalMessages; i++) {
                ChatMessage message = generateMessage();
                messageQueue.put(message);
                
                // Log progress every 50,000 messages
                if ((i + 1) % 50000 == 0) {
                    System.out.println("Generated " + (i + 1) + " messages...");
                }
            }
            
            System.out.println("MessageGenerator completed - " + totalMessages + " messages generated");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("MessageGenerator interrupted");
        }
    }

    private ChatMessage generateMessage() {
        // Generate random userId (1-100000)
        int userId = random.nextInt(100000) + 1;
        
        // Generate username from userId
        String username = "user" + userId;
        
        // Pick random message
        String message = MESSAGES[random.nextInt(MESSAGES.length)];
        
        // Generate timestamp
        String timestamp = Instant.now().toString();
        
        // Generate messageType (90% TEXT, 5% JOIN, 5% LEAVE)
        String messageType = generateMessageType();
        
        // Generate random roomId (1-20)
        String roomId = "room" + ((userId % 20) + 1);        
        return new ChatMessage(String.valueOf(userId), username, message,
                             timestamp, messageType, roomId);
    }

    private String generateMessageType() {
        int rand = random.nextInt(100);
        if (rand < 90) {
            return "TEXT";
        } else if (rand < 95) {
            return "JOIN";
        } else {
            return "LEAVE";
        }
    }
}