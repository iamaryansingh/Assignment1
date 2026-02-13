# WebSocket Chat Server

## Overview

The WebSocket Chat Server is a scalable, production-ready server implementation designed to handle high-volume concurrent chat connections. It accepts WebSocket connections from multiple clients, validates incoming messages against strict business rules, and echoes validated responses back to clients.

This server is part of the ChatFlow distributed messaging platform and is designed for CS6650 Assignment 1 - Building Scalable Distributed Systems.

---

## System Requirements

### Prerequisites

- Java Development Kit (JDK) 11 or higher
- Apache Maven 3.6 or higher
- Minimum 1GB RAM available
- Network access on ports 8080 and 8081

### Verify Prerequisites

```bash
java -version
# Expected: openjdk version "11.0.x" or higher

mvn -version  
# Expected: Apache Maven 3.6.x or higher
```

---

## Technology Stack

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| Language | Java | 11+ | Core implementation language |
| WebSocket | Java-WebSocket | 1.5.4 | WebSocket protocol implementation |
| JSON Processing | Gson | 2.10.1 | Message serialization and deserialization |
| HTTP Server | Java HttpServer | JDK Built-in | Health check endpoint |
| Logging | SLF4J | 2.0.9 | Logging framework |
| Build Tool | Maven | 3.6+ | Build automation and dependency management |

---

## Quick Start Guide

### Build

```bash
cd websocket-chat-server
mvn clean package
```

### Run

```bash
java -jar target/websocket-chat-server-1.0-SNAPSHOT.jar
```

### Test

```bash
curl http://localhost:8081/health
```

Expected response: JSON with `"status":"healthy"`

---

For complete documentation, see sections below.