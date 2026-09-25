# 01 - Go TinyURL (net/http + RWMutex)

A high-performance, thread-safe TinyURL service written in standard Go using `net/http` and `sync.RWMutex`.

## Features
- **In-Memory Store**: Safe for concurrent reads and writes with `sync.RWMutex`.
- **Base62 Slug Generator**: Cryptographically secure random 7-character short codes with collision avoidance.
- **Port**: `8001` (default)

## Running Locally

```bash
# Run tests
go test -v ./...

# Run service
PORT=8001 go run main.go
```

## Running with Docker

```bash
docker build -t tinyurl-go .
docker run -p 8001:8001 tinyurl-go
```
