package com.chatflow.client.metrics;

import java.util.concurrent.atomic.AtomicInteger;

public class MetricsCollector {
    
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger connectionCount = new AtomicInteger(0);
    
    public synchronized void recordSuccess() {
        successCount.incrementAndGet();
    }
    
    public synchronized void recordFailedMessage() {
        failureCount.incrementAndGet();
    }
    
    public synchronized void recordConnection() {
        connectionCount.incrementAndGet();
    }
    
    public int getSuccessCount() {
        return successCount.get();
    }
    
    public int getFailureCount() {
        return failureCount.get();
    }
    
    public int getConnectionCount() {
        return connectionCount.get();
    }
}