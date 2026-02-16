package com.chatflow.client.analysis;

import com.chatflow.client.model.MessageMetric;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ChartGenerator {
    
    private static final int BUCKET_SIZE_MS = 10000; // 10 seconds

    public static void generateThroughputChart(List<MessageMetric> metrics, 
                                              String filename, 
                                              long startTime) {
        // Group messages by 10-second buckets
        Map<Integer, Integer> buckets = new TreeMap<>();
        
        for (MessageMetric metric : metrics) {
            if (metric.isSuccess()) {
                long elapsedMs = metric.getTimestamp() - startTime;
                int bucketIndex = (int) (elapsedMs / BUCKET_SIZE_MS);
                buckets.put(bucketIndex, buckets.getOrDefault(bucketIndex, 0) + 1);
            }
        }

        // Create dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        for (Map.Entry<Integer, Integer> entry : buckets.entrySet()) {
            int bucketIndex = entry.getKey();
            int messageCount = entry.getValue();
            double throughput = (double) messageCount / (BUCKET_SIZE_MS / 1000.0);
            
            String timeLabel = (bucketIndex * 10) + "s";
            dataset.addValue(throughput, "Throughput", timeLabel);
        }

        // Create chart
        JFreeChart chart = ChartFactory.createLineChart(
            "Throughput Over Time",
            "Time (seconds)",
            "Messages per Second",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        // Save chart as PNG
        try {
            ChartUtils.saveChartAsPNG(new File(filename), chart, 1200, 600);
            System.out.println("Chart saved to: " + filename);
        } catch (IOException e) {
            System.err.println("Error saving chart: " + e.getMessage());
            e.printStackTrace();
        }
    }
}