# 05 - Java + Spring Boot 3 TinyURL

Robust, enterprise-grade TinyURL service built with Java 17, Spring Boot 3 Web, and a `ConcurrentHashMap` thread-safe in-memory store.

## Features
- **In-Memory Store**: Thread-safe `ConcurrentHashMap`.
- **Spring MVC**: Standard Spring REST controllers and JSON serialization.
- **Port**: `8005` (default)

## Running Locally

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8005"
```

## Running with Docker

```bash
docker build -t tinyurl-java-spring .
docker run -p 8005:8005 tinyurl-java-spring
```
