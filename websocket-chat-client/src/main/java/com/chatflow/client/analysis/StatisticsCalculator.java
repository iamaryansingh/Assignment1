package com.chatflow.client.analysis;

import com.chatflow.client.model.MessageMetric;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatisticsCalculator {
    
    public static Statistics calculate(List<MessageMetric> metrics) {
        if (metrics.isEmpty()) {
            return new Statistics();
        }

        // Extract latencies from successful messages
        List<Long> latencies = new ArrayList<>();
        for (MessageMetric metric : metrics) {
            if (metric.isSuccess()) {
                latencies.add(metric.getLatencyMs());
            }
        }

        if (latencies.isEmpty()) {
            return new Statistics();
        }

        // Sort for percentile calculations
        Collections.sort(latencies);

        long sum = 0;
        for (long latency : latencies) {
            sum += latency;
        }

        double mean = (double) sum / latencies.size();
        long median = getPercentile(latencies, 50);
        long p95 = getPercentile(latencies, 95);
        long p99 = getPercentile(latencies, 99);
        long min = latencies.get(0);
        long max = latencies.get(latencies.size() - 1);

        return new Statistics(mean, median, p95, p99, min, max);
    }

    private static long getPercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) {
            return 0;
        }
        
        int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        
        return sortedValues.get(index);
    }

    public static class Statistics {
        private final double mean;
        private final long median;
        private final long p95;
        private final long p99;
        private final long min;
        private final long max;

        public Statistics() {
            this(0, 0, 0, 0, 0, 0);
        }

        public Statistics(double mean, long median, long p95, long p99, long min, long max) {
            this.mean = mean;
            this.median = median;
            this.p95 = p95;
            this.p99 = p99;
            this.min = min;
            this.max = max;
        }

        public double getMean() { return mean; }
        public long getMedian() { return median; }
        public long getP95() { return p95; }
        public long getP99() { return p99; }
        public long getMin() { return min; }
        public long getMax() { return max; }

        @Override
        public String toString() {
            return String.format(
                "Mean: %.2f ms, Median: %d ms, 95th: %d ms, 99th: %d ms, Min: %d ms, Max: %d ms",
                mean, median, p95, p99, min, max
            );
        }
    }
}