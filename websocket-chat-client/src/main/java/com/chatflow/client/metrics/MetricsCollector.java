package com.chatflow.client.metrics;

import com.chatflow.client.model.MessageMetric;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MetricsCollector {
    
    private final List<MessageMetric> metrics;
    private final AtomicInteger successCount;
    private final AtomicInteger failureCount;
    private final AtomicInteger totalConnections;
    private final AtomicLong totalLatency;
    private final ConcurrentHashMap<String, AtomicInteger> roomMessageCounts;
    private final ConcurrentHashMap<String, AtomicInteger> messageTypeCounts;

    public MetricsCollector() {
        this.metrics = new ArrayList<>();
        this.successCount = new AtomicInteger(0);
        this.failureCount = new AtomicInteger(0);
        this.totalConnections = new AtomicInteger(0);
        this.totalLatency = new AtomicLong(0);
        this.roomMessageCounts = new ConcurrentHashMap<>();
        this.messageTypeCounts = new ConcurrentHashMap<>();
    }

    public synchronized void recordMessage(long timestamp, String messageType,
                                          long latencyMs, boolean success, String roomId) {
        MessageMetric metric = new MessageMetric(timestamp, messageType, latencyMs, success, roomId);
        metrics.add(metric);
        
        if (success) {
            successCount.incrementAndGet();
            totalLatency.addAndGet(latencyMs);
            
            // Track per room
            roomMessageCounts.computeIfAbsent(roomId, k -> new AtomicInteger(0)).incrementAndGet();
            
            // Track per message type
            messageTypeCounts.computeIfAbsent(messageType, k -> new AtomicInteger(0)).incrementAndGet();
        }
    }

    public void recordFailedMessage() {
        failureCount.incrementAndGet();
    }

    public void recordConnection() {
        totalConnections.incrementAndGet();
    }

    public int getSuccessCount() {
        return successCount.get();
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    public int getTotalConnections() {
        return totalConnections.get();
    }

    public synchronized List<MessageMetric> getMetrics() {
        return new ArrayList<>(metrics);
    }

    public ConcurrentHashMap<String, AtomicInteger> getRoomMessageCounts() {
        return roomMessageCounts;
    }

    public ConcurrentHashMap<String, AtomicInteger> getMessageTypeCounts() {
        return messageTypeCounts;
    }

    public double getAverageLatency() {
        int count = successCount.get();
        if (count == 0) return 0;
        return (double) totalLatency.get() / count;
    }
}