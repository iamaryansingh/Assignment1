# WebSocket Load Testing Client

## Overview

A multithreaded WebSocket client that sends 500,000 messages to a WebSocket server and collects performance metrics.

---

## Requirements

- Java 11+
- Maven 3.6+
- Running WebSocket server

---

## Build

```bash
cd websocket-chat-client
mvn clean package
```

---

## Configuration

Edit `LoadTestClient.java`:

```java
// Server URL
private static final String SERVER_URL = "ws://localhost:8080/chat/";

```

---

## Run

```bash
java -jar target/websocket-chat-client-1.0-SNAPSHOT.jar
```

---

## How It Works

### Architecture

```
MessageGenerator (1 thread)
    ↓
BlockingQueue (thread-safe)
    ↓
SenderWorkers (N threads)
    ↓
WebSocket Server
```

### Two-Phase Execution

**Phase 1: Warmup**
- 32 threads start
- Each sends 1,000 messages
- Total: 32,000 messages
- Threads terminate

**Phase 2: Main**
- Configurable threads start
- Send remaining 468,000 messages
- Total: 500,000 messages overall

### Message Generation

Each message contains:
- userId: Random 1-100,000
- username: "user{userId}"
- message: Random from 50 predefined messages
- roomId: "room{(userId % 20) + 1}" (20 rooms)
- messageType: 90% TEXT, 5% JOIN, 5% LEAVE
- timestamp: Current time (ISO-8601)

### Connection Management

- Connection pooling per room
- One persistent connection per room per worker
- Automatic reconnection on failure
- Graceful cleanup on completion

### Error Handling

**Retry Logic:**
- Up to 5 attempts per failed message
- Exponential backoff: 100ms → 200ms → 400ms → 800ms
- Failed messages tracked after all retries

**Connection Drops:**
- Detected via timeout (5 seconds)
- Connection removed from pool
- New connection created on retry
- No crashes on network issues

---

### Files

**results/metrics.csv**
```csv
timestamp,messageType,latencyMs,statusCode,roomId
1707598234567,TEXT,1,200,room5
1707598234568,TEXT,2,200,room12
```
500,000 rows with per-message data

**results/throughput_chart.png**
- Line chart showing throughput over time
- 10-second buckets

---

## Components

### LoadTestClient

Main orchestrator that:
- Manages test execution
- Creates thread pools
- Displays results
- Generates output files

### MessageGenerator

Producer thread that:
- Generates 500,000 messages
- Places in BlockingQueue
- Uses random valid data
- Runs once per phase

### SenderWorker

Consumer threads that:
- Take messages from queue
- Send via WebSocket
- Track latency
- Handle retries

### MetricsCollector

Thread-safe collector that:
- Records per-message metrics
- Tracks success/failure counts
- Aggregates statistics
- Provides final data

### Connection Pool

Per-worker connection management:
- One connection per room
- Persistent throughout test
- Auto-reconnect on failure

---

## Troubleshooting

### OutOfMemoryError: unable to create native thread

**Cause:** Too many threads

**Fix:**
```java
// In ReusableWebSocketClient constructor
setConnectionLostTimeout(0);
```

And/or reduce thread count:
```java
private static final int MAIN_PHASE_THREADS = 16;
```

### Connection Refused

**Cause:** Server not running

**Fix:** Start server first
```bash
cd websocket-chat-server
java -jar target/websocket-chat-server-1.0-SNAPSHOT.jar
```

### Timeouts

**Cause:** Server overloaded or timeout too short

**Fix:** Increase timeout or reduce threads
```java
private static final int RESPONSE_TIMEOUT_MS = 10000;
private static final int MAIN_PHASE_THREADS = 16;
```

---

## Project Structure

```
src/main/java/com/chatflow/client/
├── LoadTestClient.java           Main orchestrator
├── model/
│   ├── ChatMessage.java          Message model
│   └── MessageMetric.java        Metric model
├── generator/
│   └── MessageGenerator.java    Message producer
├── worker/
│   └── SenderWorker.java        Message sender
├── metrics/
│   └── MetricsCollector.java    Thread-safe metrics
└── analysis/
    ├── StatisticsCalculator.java Stats calculation
    ├── CSVWriter.java            CSV export
    └── ChartGenerator.java       Chart generation
```

---
