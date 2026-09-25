# 07 - Ruby + Sinatra TinyURL

Lightweight TinyURL service built with Ruby and Sinatra, using a `Mutex`-protected hash map for thread-safe in-memory storage.

## Features
- **In-Memory Store**: Thread-synchronized hash map.
- **Sinatra Microframework**: Clean DSL for routing.
- **Port**: `8007` (default)

## Running Locally

```bash
bundle install
PORT=8007 bundle exec ruby app.rb
```

## Running with Docker

```bash
docker build -t tinyurl-ruby-sinatra .
docker run -p 8007:8007 tinyurl-ruby-sinatra
```
