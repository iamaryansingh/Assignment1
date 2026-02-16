package com.chatflow.client;

import com.chatflow.client.generator.MessageGenerator;
import com.chatflow.client.metrics.MetricsCollector;
import com.chatflow.client.model.ChatMessage;
import com.chatflow.client.worker.SenderWorker;

import java.util.concurrent.*;

public class LoadTestClient {
    
    private static final String SERVER_URL = "ws://localhost:8080/chat/";
    private static final int TOTAL_MESSAGES = 500000;
    
    // WARMUP
    private static final int WARMUP_THREADS = 32; 
    private static final int WARMUP_MESSAGES_PER_THREAD = 1000; 
    
    // MAIN
    private static final int MAIN_PHASE_THREADS = 45; 
    private static final int QUEUE_CAPACITY = 50000;

    public static void main(String[] args) {
        System.out.println("============================================");
        System.out.println("  WEBSOCKET LOAD TEST CLIENT - PART 1");
        System.out.println("============================================");
        System.out.println("Configuration:");
        System.out.println("  Server: " + SERVER_URL);
        System.out.println("  Total messages: " + TOTAL_MESSAGES);
        System.out.println("  Warmup threads: " + WARMUP_THREADS);
        System.out.println("  Main phase threads: " + MAIN_PHASE_THREADS);
        System.out.println("============================================\n");
        
        LoadTestClient client = new LoadTestClient();
        client.runTest();
    }

    public void runTest() {
        long overallStartTime = System.currentTimeMillis();
        
        MetricsCollector metricsCollector = new MetricsCollector();
        BlockingQueue<ChatMessage> messageQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        
        try {
            // Phase 1: Warmup
            System.out.println("=== WARMUP PHASE ===");
            runWarmupPhase(messageQueue, metricsCollector);
            
            // Phase 2: Main Load Test
            System.out.println("\n=== MAIN PHASE ===");
            runMainPhase(messageQueue, metricsCollector);
            
            long overallEndTime = System.currentTimeMillis();
            
            // Display results
            displayResults(metricsCollector, overallStartTime, overallEndTime);
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void runWarmupPhase(BlockingQueue<ChatMessage> messageQueue, 
                                MetricsCollector metricsCollector) 
            throws InterruptedException {
        
        int warmupMessages = WARMUP_THREADS * WARMUP_MESSAGES_PER_THREAD;
        
        System.out.println("Starting warmup with " + WARMUP_THREADS + " threads...");
        System.out.println("Each thread will send " + WARMUP_MESSAGES_PER_THREAD + " messages");
        System.out.println("Total warmup messages: " + warmupMessages + "\n");
        
        long startTime = System.currentTimeMillis();
        
        // Start message generator
        Thread generatorThread = new Thread(
            new MessageGenerator(messageQueue, warmupMessages)
        );
        generatorThread.start();
        
        // Wait for queue to fill
        System.out.println("Waiting for message queue to fill...");
        Thread.sleep(2000);
        
        // Start worker threads
        ExecutorService executorService = Executors.newFixedThreadPool(WARMUP_THREADS);
        CountDownLatch latch = new CountDownLatch(WARMUP_THREADS);
        
        for (int i = 0; i < WARMUP_THREADS; i++) {
            if (i > 0 && i % 5 == 0) {
                Thread.sleep(100);
            }
            
            SenderWorker worker = new SenderWorker(
                i, SERVER_URL, messageQueue, metricsCollector,
                WARMUP_MESSAGES_PER_THREAD, latch
            );
            executorService.submit(worker);
        }
        
        System.out.println("All warmup threads started, waiting for completion...\n");
        
        latch.await();
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.MINUTES);
        generatorThread.join();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double throughput = (warmupMessages * 1000.0) / duration;
        
        System.out.println("\n--- Warmup Phase Completed ---");
        System.out.println("Threads: " + WARMUP_THREADS);
        System.out.println("Messages: " + warmupMessages);
        System.out.println("Successful: " + metricsCollector.getSuccessCount());
        System.out.println("Failed: " + metricsCollector.getFailureCount());
        System.out.println("Duration: " + String.format("%.3f", duration / 1000.0) + " seconds");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " msg/sec");
    }

    private void runMainPhase(BlockingQueue<ChatMessage> messageQueue,
                             MetricsCollector metricsCollector) 
            throws InterruptedException {
        
        int warmupMessages = WARMUP_THREADS * WARMUP_MESSAGES_PER_THREAD;
        int mainPhaseMessages = TOTAL_MESSAGES - warmupMessages;
        
        System.out.println("Starting main phase with " + MAIN_PHASE_THREADS + " threads...");
        System.out.println("Remaining messages to send: " + mainPhaseMessages + "\n");
        
        long startTime = System.currentTimeMillis();
        
        // Start message generator
        Thread generatorThread = new Thread(
            new MessageGenerator(messageQueue, mainPhaseMessages)
        );
        generatorThread.start();
        
        // Wait for queue to fill
        System.out.println("Waiting for message queue to fill...");
        Thread.sleep(2000);
        
        // Calculate messages per thread
        int messagesPerThread = mainPhaseMessages / MAIN_PHASE_THREADS;
        int remainingMessages = mainPhaseMessages % MAIN_PHASE_THREADS;
        
        // Start worker threads
        ExecutorService executorService = Executors.newFixedThreadPool(MAIN_PHASE_THREADS);
        CountDownLatch latch = new CountDownLatch(MAIN_PHASE_THREADS);
        
        for (int i = 0; i < MAIN_PHASE_THREADS; i++) {
            if (i > 0 && i % 5 == 0) {
                Thread.sleep(100);
            }
            
            int messagesToSend = messagesPerThread;
            if (i < remainingMessages) {
                messagesToSend++;
            }
            
            SenderWorker worker = new SenderWorker(
                i, SERVER_URL, messageQueue, metricsCollector,
                messagesToSend, latch
            );
            executorService.submit(worker);
        }
        
        System.out.println("All main phase threads started, waiting for completion...");
        System.out.println("This may take several minutes...\n");
        
        latch.await();
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.MINUTES);
        generatorThread.join();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double throughput = (mainPhaseMessages * 1000.0) / duration;
        
        System.out.println("\n--- Main Phase Completed ---");
        System.out.println("Threads: " + MAIN_PHASE_THREADS);
        System.out.println("Messages: " + mainPhaseMessages);
        System.out.println("Duration: " + String.format("%.3f", duration / 1000.0) + " seconds");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " msg/sec");
    }

    private void displayResults(MetricsCollector metricsCollector, long startTime, long endTime) {
        long duration = endTime - startTime;
        int successCount = metricsCollector.getSuccessCount();
        int failureCount = metricsCollector.getFailureCount();
        int totalAttempted = successCount + failureCount;
        
        System.out.println("\n============================================");
        System.out.println("  OVERALL RESULTS");
        System.out.println("============================================");
        System.out.println("Total messages attempted: " + totalAttempted);
        System.out.println("Successful: " + successCount);
        System.out.println("Failed: " + failureCount);
        System.out.println("Success rate: " + String.format("%.2f", (successCount * 100.0) / totalAttempted) + "%");
        System.out.println("Total runtime: " + String.format("%.3f", duration / 1000.0) + " seconds");
        System.out.println("Overall throughput: " + String.format("%.2f", (successCount * 1000.0) / duration) + " msg/sec");
        System.out.println("Total connections: " + metricsCollector.getConnectionCount());
        System.out.println("============================================");
    }
}