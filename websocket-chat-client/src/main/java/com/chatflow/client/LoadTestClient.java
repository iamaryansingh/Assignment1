package com.chatflow.client;

import com.chatflow.client.analysis.CSVWriter;
import com.chatflow.client.analysis.ChartGenerator;
import com.chatflow.client.analysis.StatisticsCalculator;
import com.chatflow.client.generator.MessageGenerator;
import com.chatflow.client.metrics.MetricsCollector;
import com.chatflow.client.model.ChatMessage;
import com.chatflow.client.model.MessageMetric;
import com.chatflow.client.worker.SenderWorker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadTestClient {
    
    // private static final String SERVER_URL = "ws://localhost:8080/chat/";
    private static final String SERVER_URL = "ws://35.89.93.23:8080/chat/";

    private static final int TOTAL_MESSAGES = 500000;
    
    // WARMUP: Fewer threads, more messages per thread
    private static final int WARMUP_THREADS = 32; 
    private static final int WARMUP_MESSAGES_PER_THREAD = 1000; 
    
    // MAIN: Conservative thread count
    private static final int MAIN_PHASE_THREADS = 32; 
    
    private static final int QUEUE_CAPACITY = 50000;

    public static void main(String[] args) {
        System.out.println("============================================");
        System.out.println("  WEBSOCKET LOAD TEST CLIENT - PART 2");
        System.out.println("============================================");
        System.out.println("Configuration:");
        System.out.println("  Server: " + SERVER_URL);
        System.out.println("  Total messages: " + TOTAL_MESSAGES);
        System.out.println("  Warmup threads: " + WARMUP_THREADS);
        System.out.println("  Main phase threads: " + MAIN_PHASE_THREADS);
        System.out.println("============================================");
        System.out.println();
        
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
            
            long totalDuration = overallEndTime - overallStartTime;

            // Display results
            displayResults(metricsCollector, overallStartTime, overallEndTime);

            // Generate outputs
            generateOutputs(metricsCollector, overallStartTime, totalDuration);
            
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
        System.out.println("Total warmup messages: " + warmupMessages);
        System.out.println();
        
        long startTime = System.currentTimeMillis();
        
        // Start message generator
        Thread generatorThread = new Thread(
            new MessageGenerator(messageQueue, warmupMessages)
        );
        generatorThread.start();
        
        // Wait for queue to have some messages
        System.out.println("Waiting for message queue to fill...");
        Thread.sleep(2000);
        
        // Start worker threads with small delays between them
        ExecutorService executorService = Executors.newFixedThreadPool(WARMUP_THREADS);
        CountDownLatch latch = new CountDownLatch(WARMUP_THREADS);
        
        for (int i = 0; i < WARMUP_THREADS; i++) {
            // Small delay to avoid connection burst
            if (i > 0 && i % 5 == 0) {
                Thread.sleep(100);
            }
            
            SenderWorker worker = new SenderWorker(
                i, SERVER_URL, messageQueue, metricsCollector,
                WARMUP_MESSAGES_PER_THREAD, latch
            );
            executorService.submit(worker);
        }
        
        System.out.println("All warmup threads started, waiting for completion...");
        
        // Wait for completion
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
        System.out.println("Duration: " + (duration / 1000.0) + " seconds");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " msg/sec");
    }

    private void runMainPhase(BlockingQueue<ChatMessage> messageQueue,
                             MetricsCollector metricsCollector) 
            throws InterruptedException {
        
        int warmupMessages = WARMUP_THREADS * WARMUP_MESSAGES_PER_THREAD;
        int mainPhaseMessages = TOTAL_MESSAGES - warmupMessages;
        
        System.out.println("Starting main phase with " + MAIN_PHASE_THREADS + " threads...");
        System.out.println("Remaining messages to send: " + mainPhaseMessages);
        System.out.println();
        
        long startTime = System.currentTimeMillis();
        
        // Start message generator
        Thread generatorThread = new Thread(
            new MessageGenerator(messageQueue, mainPhaseMessages)
        );
        generatorThread.start();
        
        // Wait for queue to have some messages
        System.out.println("Waiting for message queue to fill...");
        Thread.sleep(2000);
        
        // Calculate messages per thread
        int messagesPerThread = mainPhaseMessages / MAIN_PHASE_THREADS;
        int remainingMessages = mainPhaseMessages % MAIN_PHASE_THREADS;
        
        // Start worker threads with small delays
        ExecutorService executorService = Executors.newFixedThreadPool(MAIN_PHASE_THREADS);
        CountDownLatch latch = new CountDownLatch(MAIN_PHASE_THREADS);
        
        for (int i = 0; i < MAIN_PHASE_THREADS; i++) {
            // Small delay to avoid connection burst
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
        System.out.println("This may take several minutes...");
        System.out.println();
        
        // Wait for completion
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
        System.out.println("Duration: " + (duration / 1000.0) + " seconds");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " msg/sec");
    }

    private void displayResults(MetricsCollector metricsCollector, 
                               long startTime, long endTime) {
        
        System.out.println("\n============================================");
        System.out.println("  OVERALL RESULTS");
        System.out.println("============================================");
        
        int successCount = metricsCollector.getSuccessCount();
        int failureCount = metricsCollector.getFailureCount();
        int totalMessages = successCount + failureCount;
        long duration = endTime - startTime;
        double throughput = (successCount * 1000.0) / duration;
        
        System.out.println("Total messages attempted: " + TOTAL_MESSAGES);
        System.out.println("Successful: " + successCount);
        System.out.println("Failed: " + failureCount);
        System.out.println("Success rate: " + String.format("%.2f", (successCount * 100.0 / TOTAL_MESSAGES)) + "%");
        System.out.println("Total runtime: " + (duration / 1000.0) + " seconds");
        System.out.println("Overall throughput: " + String.format("%.2f", throughput) + " msg/sec");
        System.out.println("Total connections: " + metricsCollector.getTotalConnections());
        
        // statistics
        List<MessageMetric> metrics = metricsCollector.getMetrics();
        
        if (metrics.isEmpty()) {
            System.out.println("\nNo metrics collected - all messages failed!");
            return;
        }
        
        StatisticsCalculator.Statistics stats = StatisticsCalculator.calculate(metrics);
        
        System.out.println("\n=== LATENCY STATISTICS ===");
        System.out.println("Mean response time: " + String.format("%.2f", stats.getMean()) + " ms");
        System.out.println("Median response time: " + stats.getMedian() + " ms");
        System.out.println("95th percentile: " + stats.getP95() + " ms");
        System.out.println("99th percentile: " + stats.getP99() + " ms");
        System.out.println("Min response time: " + stats.getMin() + " ms");
        System.out.println("Max response time: " + stats.getMax() + " ms");
        
        // Throughput by room
        System.out.println("\n=== THROUGHPUT BY ROOM ===");
        Map<String, AtomicInteger> roomCounts = metricsCollector.getRoomMessageCounts();
        roomCounts.entrySet().stream()
            .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
            .forEach(entry -> {
                double roomThroughput = (entry.getValue().get() * 1000.0) / duration;
                System.out.println(entry.getKey() + ": " + entry.getValue().get() + 
                                 " messages (" + String.format("%.2f", roomThroughput) + " msg/sec)");
            });
        
        // Message type distribution
        System.out.println("\n=== MESSAGE TYPE DISTRIBUTION ===");
        Map<String, AtomicInteger> typeCounts = metricsCollector.getMessageTypeCounts();
        typeCounts.forEach((type, count) -> {
            double percentage = (count.get() * 100.0) / successCount;
            System.out.println(type + ": " + count.get() + " (" + 
                             String.format("%.2f", percentage) + "%)");
        });
        
        System.out.println("\n============================================");
    }

    private void generateOutputs(MetricsCollector metricsCollector, long startTime, long duration) {
        System.out.println("\n=== GENERATING OUTPUTS ===");
        
        List<MessageMetric> metrics = metricsCollector.getMetrics();
        
        if (metrics.isEmpty()) {
            System.out.println("No metrics to export - all messages failed!");
            return;
        }
        
        int successCount = metricsCollector.getSuccessCount();
        
        // 1. Write per-message metrics CSV
        CSVWriter.writeMetrics(metrics, "results/metrics.csv");
        
        // 2. Write throughput by room CSV
        writeThroughputByRoomCSV(metricsCollector.getRoomMessageCounts(), duration);
        
        // 3. Write message type distribution CSV
        writeMessageTypeDistributionCSV(metricsCollector.getMessageTypeCounts(), successCount);
        
        // 4. Write summary statistics CSV
        writeSummaryStatisticsCSV(metrics, successCount, metricsCollector.getFailureCount(), duration);
        
        // 5. Generate throughput chart
        ChartGenerator.generateThroughputChart(metrics, "results/throughput_chart.png", startTime);
        
        System.out.println("\nAll outputs generated successfully!");
        System.out.println("  - results/metrics.csv");
        System.out.println("  - results/throughput_by_room.csv");
        System.out.println("  - results/message_type_distribution.csv");
        System.out.println("  - results/summary_statistics.csv");
        System.out.println("  - results/throughput_chart.png");
    }
    
    private void writeThroughputByRoomCSV(Map<String, AtomicInteger> roomCounts, long duration) {
        try (java.io.FileWriter writer = new java.io.FileWriter("results/throughput_by_room.csv")) {
            // Write header
            writer.write("roomId,messageCount,throughput_msg_per_sec\n");
            
            // Write data sorted by room
            roomCounts.entrySet().stream()
                .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
                .forEach(entry -> {
                    try {
                        double roomThroughput = (entry.getValue().get() * 1000.0) / duration;
                        writer.write(entry.getKey() + "," + 
                                   entry.getValue().get() + "," + 
                                   String.format("%.2f", roomThroughput) + "\n");
                    } catch (Exception e) {
                        System.err.println("Error writing room throughput: " + e.getMessage());
                    }
                });
            
            System.out.println("Throughput by room CSV written");
            
        } catch (Exception e) {
            System.err.println("Error writing throughput by room CSV: " + e.getMessage());
        }
    }
    
    private void writeMessageTypeDistributionCSV(Map<String, AtomicInteger> typeCounts, int totalSuccess) {
        try (java.io.FileWriter writer = new java.io.FileWriter("results/message_type_distribution.csv")) {
            // Write header
            writer.write("messageType,count,percentage\n");
            
            // Write data
            typeCounts.forEach((type, count) -> {
                try {
                    double percentage = (count.get() * 100.0) / totalSuccess;
                    writer.write(type + "," + 
                               count.get() + "," + 
                               String.format("%.2f", percentage) + "\n");
                } catch (Exception e) {
                    System.err.println("Error writing message type: " + e.getMessage());
                }
            });
            
            System.out.println("Message type distribution CSV written");
            
        } catch (Exception e) {
            System.err.println("Error writing message type distribution CSV: " + e.getMessage());
        }
    }
    
    private void writeSummaryStatisticsCSV(List<MessageMetric> metrics, int successCount, 
                                          int failureCount, long duration) {
        try (java.io.FileWriter writer = new java.io.FileWriter("results/summary_statistics.csv")) {
            // Calculate statistics
            StatisticsCalculator.Statistics stats = StatisticsCalculator.calculate(metrics);
            
            // Write as key-value pairs
            writer.write("metric,value,unit\n");
            writer.write("total_messages," + (successCount + failureCount) + ",messages\n");
            writer.write("successful_messages," + successCount + ",messages\n");
            writer.write("failed_messages," + failureCount + ",messages\n");
            writer.write("success_rate," + String.format("%.2f", (successCount * 100.0) / (successCount + failureCount)) + ",percent\n");
            writer.write("total_duration," + (duration / 1000.0) + ",seconds\n");
            writer.write("overall_throughput," + String.format("%.2f", (successCount * 1000.0) / duration) + ",msg_per_sec\n");
            writer.write("mean_latency," + String.format("%.2f", stats.getMean()) + ",ms\n");
            writer.write("median_latency," + stats.getMedian() + ",ms\n");
            writer.write("p95_latency," + stats.getP95() + ",ms\n");
            writer.write("p99_latency," + stats.getP99() + ",ms\n");
            writer.write("min_latency," + stats.getMin() + ",ms\n");
            writer.write("max_latency," + stats.getMax() + ",ms\n");
            
            System.out.println("Summary statistics CSV written");
            
        } catch (Exception e) {
            System.err.println("Error writing summary statistics CSV: " + e.getMessage());
        }
    }
}