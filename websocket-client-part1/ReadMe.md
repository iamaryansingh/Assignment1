# WebSocket Load Testing Client - Part 1

## Overview

Basic multithreaded WebSocket client that sends 500,000 messages to a WebSocket server and measures fundamental performance metrics.

---

## Requirements

- Java 11+
- Maven 3.6+
- Running WebSocket server

---

## Build

```bash
cd websocket-client-part1
mvn clean package
```

---

## Configuration

Edit `src/main/java/com/chatflow/client/LoadTestClient.java`:

```java
private static final String SERVER_URL = "ws://localhost:8080/chat/";
private static final int MAIN_PHASE_THREADS = 45;
```

---

## Run

```bash
java -jar target/websocket-client-part1-1.0-SNAPSHOT.jar
```

---

## How It Works

### Two-Phase Execution

**Warmup Phase:**
- 32 threads
- Each sends 1,000 messages
- Total: 32,000 messages

**Main Phase:**
- 45 threads (configurable)
- Sends remaining 468,000 messages
- Total: 500,000 messages

### Architecture

```
MessageGenerator → BlockingQueue → SenderWorkers → WebSocket Server
```

- Single producer thread generates messages
- BlockingQueue buffers messages (thread-safe)
- Multiple worker threads send concurrently
- Connection pooling per room
- Retry logic with exponential backoff

---

## Project Structure

```
websocket-client-part1/
├── pom.xml
├── README.md
└── src/main/java/com/chatflow/client/
    ├── LoadTestClient.java
    ├── model/
    │   └── ChatMessage.java
    ├── generator/
    │   └── MessageGenerator.java
    ├── worker/
    │   └── SenderWorker.java
    └── metrics/
        └── MetricsCollector.java
```

---

## Components

**LoadTestClient.java** - Main orchestrator, manages test phases and thread pools

**MessageGenerator.java** - Generates 500K messages with random valid data

**SenderWorker.java** - Worker threads that send messages via WebSocket with retry logic

**MetricsCollector.java** - Thread-safe tracking of success/failure counts and connections

**ChatMessage.java** - Message data model

---

---