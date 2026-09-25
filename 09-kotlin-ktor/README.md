# 09 - Kotlin + Ktor TinyURL

Asynchronous, coroutine-based TinyURL service built with Kotlin and Ktor Netty, using `ConcurrentHashMap` and `AtomicLong` for thread-safe state.

## Features
- **In-Memory Store**: Thread-safe `ConcurrentHashMap` with atomic counters.
- **Ktor Framework**: Lightweight, coroutine-native Netty server.
- **Port**: `8009` (default)

## Running Locally

```bash
./gradlew run
```

## Running with Docker

```bash
docker build -t tinyurl-kotlin-ktor .
docker run -p 8009:8009 tinyurl-kotlin-ktor
```
