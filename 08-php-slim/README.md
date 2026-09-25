# 08 - PHP 8 + Slim Framework TinyURL

Modern PHP 8 TinyURL service built with Slim 4 and an in-memory/shared memory store.

## Features
- **In-Memory Store**: Fast in-memory SQLite (`/dev/shm` / temp buffer) for stateful concurrent handling.
- **Slim 4**: Lightweight PSR-7 / PSR-15 compliant micro-framework.
- **Port**: `8008` (default)

## Running Locally

```bash
composer install
PORT=8008 php -S 0.0.0.0:8008 -t public public/index.php
```

## Running with Docker

```bash
docker build -t tinyurl-php-slim .
docker run -p 8008:8008 tinyurl-php-slim
```
