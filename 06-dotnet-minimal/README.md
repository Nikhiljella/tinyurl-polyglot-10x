# 06 - C# / .NET 8 Minimal API TinyURL

Concise, high-performance TinyURL service built with modern C# and ASP.NET Core 8 Minimal APIs, featuring a lock-free `ConcurrentDictionary` and atomic `Interlocked.Increment`.

## Features
- **In-Memory Store**: Thread-safe `ConcurrentDictionary<string, UrlRecord>` with `Interlocked` atomic counter.
- **ASP.NET Core Minimal APIs**: Zero-boilerplate routing.
- **Port**: `8006` (default)

## Running Locally

```bash
dotnet run
```

## Running with Docker

```bash
docker build -t tinyurl-dotnet-minimal .
docker run -p 8006:8006 tinyurl-dotnet-minimal
```
