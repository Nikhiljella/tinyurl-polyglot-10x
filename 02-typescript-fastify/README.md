# 02 - TypeScript + Fastify TinyURL

High-throughput, asynchronous TinyURL microservice built with TypeScript, Fastify, and an in-memory Map store.

## Features
- **In-Memory Store**: ES6 Map with constant-time lookup.
- **Fastify Web Engine**: Low-overhead HTTP processing.
- **Port**: `8002` (default)

## Running Locally

```bash
npm install
npm run build
PORT=8002 npm start
```

## Running with Docker

```bash
docker build -t tinyurl-ts-fastify .
docker run -p 8002:8002 tinyurl-ts-fastify
```
