# 10 - Bun + Hono TinyURL

Ultra-fast, next-generation TinyURL service built with Bun runtime and Hono web framework, with in-memory `Map` storage.

## Features
- **In-Memory Store**: Native JS `Map` with nano-second retrieval.
- **Hono on Bun**: Extremely lightweight, fast edge-ready web framework.
- **Port**: `8010` (default)

## Running Locally

```bash
bun install
PORT=8010 bun run src/index.ts
```

## Running with Docker

```bash
docker build -t tinyurl-bun-hono .
docker run -p 8010:8010 tinyurl-bun-hono
```
