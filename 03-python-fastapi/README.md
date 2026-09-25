# 03 - Python + FastAPI TinyURL

Modern, asynchronous TinyURL microservice built with Python 3, FastAPI, Pydantic v2, and thread-safe in-memory store.

## Features
- **In-Memory Store**: `threading.Lock`-guarded dictionary.
- **FastAPI / Pydantic**: Automated request validation and OpenAPI doc generation (`/docs`).
- **Port**: `8003` (default)

## Running Locally

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# Run tests
pytest test_main.py -v

# Run server
PORT=8003 python main.py
```

## Running with Docker

```bash
docker build -t tinyurl-python-fastapi .
docker run -p 8003:8003 tinyurl-python-fastapi
```
