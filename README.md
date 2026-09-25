# TinyURL Polyglot 10x 🚀

A high-performance TinyURL / URL shortener microservice implemented across **10 different modern technology stacks**, all sharing the exact same REST API contract, thread-safe in-memory storage, Docker support, and a unified test harness.

---

## 🌟 The 10 Tech Stacks

| # | Tech Stack | Framework / Libraries | In-Memory Mechanism | Default Port | Directory |
|---|---|---|---|:---:|---|
| **01** | **Go** | Standard `net/http` | `sync.RWMutex` + `map[string]*URLRecord` | `8001` | [`01-go/`](./01-go) |
| **02** | **TypeScript** | Node.js + Fastify | `Map<string, UrlRecord>` | `8002` | [`02-typescript-fastify/`](./02-typescript-fastify) |
| **03** | **Python** | FastAPI + Uvicorn | `threading.Lock` + `dict` | `8003` | [`03-python-fastapi/`](./03-python-fastapi) |
| **04** | **Rust** | Axum + Tokio | `Arc<RwLock<HashMap<String, UrlRecord>>>` | `8004` | [`04-rust-axum/`](./04-rust-axum) |
| **05** | **Java** | Spring Boot 3 Web | `ConcurrentHashMap<String, UrlRecord>` | `8005` | [`05-java-spring/`](./05-java-spring) |
| **06** | **C# / .NET** | ASP.NET Core 8 Minimal API | `ConcurrentDictionary<string, UrlRecord>` + `Interlocked` | `8006` | [`06-dotnet-minimal/`](./06-dotnet-minimal) |
| **07** | **Ruby** | Sinatra + Puma | `Mutex` + `Hash` | `8007` | [`07-ruby-sinatra/`](./07-ruby-sinatra) |
| **08** | **PHP** | Modern PHP 8 + Slim 4 | Shared in-memory SQLite (`/dev/shm` / memory buffer) | `8008` | [`08-php-slim/`](./08-php-slim) |
| **09** | **Kotlin** | Ktor + Netty | `ConcurrentHashMap` + `AtomicLong` | `8009` | [`09-kotlin-ktor/`](./09-kotlin-ktor) |
| **10** | **Bun** | Bun Runtime + Hono | Native JS `Map<string, UrlRecord>` | `8010` | [`10-bun-hono/`](./10-bun-hono) |

---

## 📡 Unified REST API Specification

Every single stack strictly obeys this specification:

### 1. Health Check
- **`GET /api/health`**
- **Response `200 OK`**:
```json
{
  "status": "ok",
  "stack": "01-go"
}
```

### 2. Shorten URL
- **`POST /api/shorten`**
- **Header**: `Content-Type: application/json`
- **Request Body**:
```json
{
  "url": "https://example.com/very/long/url",
  "custom_alias": "my-cool-link"  // Optional
}
```
- **Response `201 Created`**:
```json
{
  "id": "my-cool-link",
  "original_url": "https://example.com/very/long/url",
  "short_url": "http://localhost:8001/my-cool-link",
  "click_count": 0,
  "created_at": "2026-09-25T00:00:00Z"
}
```
- **Error `400 Bad Request`**: If URL scheme is not `http://` or `https://`, or if `custom_alias` is already in use.

### 3. Redirect to Original URL
- **`GET /:id`**
- **Response `302 Found`**:
  - `Location: https://example.com/very/long/url`
  - Increments `click_count` by 1 atomically.
- **Error `404 Not Found`**: If short code does not exist.

### 4. URL Analytics & Statistics
- **`GET /api/stats/:id`**
- **Response `200 OK`**:
```json
{
  "id": "my-cool-link",
  "original_url": "https://example.com/very/long/url",
  "short_url": "http://localhost:8001/my-cool-link",
  "click_count": 1,
  "created_at": "2026-09-25T00:00:00Z"
}
```

---

## 🐳 Running with Docker & Docker Compose

You can launch all 10 implementations at the same time using `docker compose`:

```bash
# Build and start all 10 services in detached mode
docker compose up --build -d

# View status
docker compose ps

# Run universal test suite across all services
./test-all.sh

# Stop all services
docker compose down
```

Or run any single service individually:
```bash
docker compose up --build 01-go
# or
docker compose up --build 03-python-fastapi
```

---

## 🧪 Universal Verification Test Suite

A universal test runner script is provided in `./test-all.sh`. It automatically verifies:
1. Health check response
2. Random Base62 7-character slug generation
3. Custom alias creation
4. Collision rejection (`400 Bad Request` on duplicate alias)
5. HTTP `302 Found` redirect and `Location` header validation
6. Analytics `click_count` increment verification

```bash
# Test all running services
./test-all.sh

# Or test a specific port
./test-all.sh 8001
```

---

## 💻 Local Quickstarts

### 01 - Go (Port 8001)
```bash
cd 01-go
go test -v ./...
PORT=8001 go run main.go
```

### 02 - TypeScript + Fastify (Port 8002)
```bash
cd 02-typescript-fastify
npm install
npm run build
PORT=8002 npm start
```

### 03 - Python + FastAPI (Port 8003)
```bash
cd 03-python-fastapi
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
PORT=8003 python main.py
```

### 04 - Rust + Axum (Port 8004)
```bash
cd 04-rust-axum
cargo build --release
PORT=8004 ./target/release/tinyurl-rust-axum
```

### 05 - Java + Spring Boot (Port 8005)
```bash
cd 05-java-spring
mvn spring-boot:run
```

### 06 - C# / .NET Minimal API (Port 8006)
```bash
cd 06-dotnet-minimal
dotnet run
```

### 07 - Ruby + Sinatra (Port 8007)
```bash
cd 07-ruby-sinatra
bundle install
PORT=8007 bundle exec ruby app.rb
```

### 08 - PHP 8 + Slim (Port 8008)
```bash
cd 08-php-slim
composer install
PORT=8008 php -S 0.0.0.0:8008 -t public public/index.php
```

### 09 - Kotlin + Ktor (Port 8009)
```bash
cd 09-kotlin-ktor
./gradlew run
```

### 10 - Bun + Hono (Port 8010)
```bash
cd 10-bun-hono
bun install
PORT=8010 bun run src/index.ts
```

---

## 🔒 Concurrency & Thread-Safety Design

TinyURL services under high load experience concurrent writes (link creation) and concurrent reads/updates (redirect click increments).
Every stack has been engineered with its idiomatic concurrency mechanism:
- **Go**: `sync.RWMutex` separating read locks (`RLock`) from write locks (`Lock`).
- **Rust**: `Arc<RwLock<HashMap>>` providing zero-cost data race safety at compile time.
- **Java**: `ConcurrentHashMap` with non-blocking reads and segmented bucket locks.
- **.NET**: `ConcurrentDictionary` and `Interlocked.Increment` for lockless atomic increments.
- **Python**: `threading.Lock` protecting the in-memory dictionary from race conditions.
- **Ruby**: `Mutex` synchronization wrapping the internal hash table.
- **Kotlin**: `ConcurrentHashMap` combined with `AtomicLong` counters.
- **PHP**: Shared memory SQLite connection with atomic transactions.
- **Node & Bun**: Single-threaded event loop atomic dictionary/map access.
