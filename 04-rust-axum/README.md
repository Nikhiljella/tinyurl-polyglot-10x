# 04 - Rust + Axum TinyURL

Blazing fast, memory-safe TinyURL microservice built with Rust, Axum, Tokio, and an `Arc<RwLock<HashMap>>` in-memory store.

## Features
- **In-Memory Store**: Safe multi-threaded read/write locking with `Arc<RwLock<HashMap<String, UrlRecord>>>`.
- **Axum Web Framework**: Ergonomic, modular web routing built on Tower and Hyper.
- **Port**: `8004` (default)

## Running Locally

```bash
cargo build --release
PORT=8004 ./target/release/tinyurl-rust-axum
```

## Running with Docker

```bash
docker build -t tinyurl-rust-axum .
docker run -p 8004:8004 tinyurl-rust-axum
```
