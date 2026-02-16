package com.chatflow.client.analysis;

import com.chatflow.client.model.MessageMetric;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CSVWriter {
    
    public static void writeMetrics(List<MessageMetric> metrics, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            // Write header
            writer.write("timestamp,messageType,latencyMs,statusCode,roomId\n");
            
            // Write data
            for (MessageMetric metric : metrics) {
                writer.write(metric.toString() + "\n");
            }
            
            System.out.println("Metrics written to: " + filename);
            
        } catch (IOException e) {
            System.err.println("Error writing CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
}