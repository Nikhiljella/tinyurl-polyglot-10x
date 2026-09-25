using System.Collections.Concurrent;
using System.Security.Cryptography;
using System.Text.Json.Serialization;

var builder = WebApplication.CreateBuilder(args);
var app = builder.Build();

var port = Environment.GetEnvironmentVariable("PORT") ?? "8006";
var baseUrl = Environment.GetEnvironmentVariable("BASE_URL") ?? $"http://localhost:{port}";

var store = new ConcurrentDictionary<string, UrlRecord>();

string GenerateSlug(int length = 7)
{
    const string chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    var buffer = new byte[length];
    RandomNumberGenerator.Fill(buffer);
    return new string(buffer.Select(b => chars[b % chars.Length]).ToArray());
}

app.MapGet("/api/health", () => Results.Ok(new { status = "ok", stack = "06-dotnet-minimal" }));

app.MapPost("/api/shorten", (ShortenRequest req) =>
{
    if (string.IsNullOrWhiteSpace(req.Url) || (!req.Url.StartsWith("http://") && !req.Url.StartsWith("https://")))
    {
        return Results.BadRequest(new { error = "Invalid URL scheme: must start with http:// or https://" });
    }

    string slug;
    if (!string.IsNullOrWhiteSpace(req.CustomAlias))
    {
        slug = req.CustomAlias.Trim();
        if (store.ContainsKey(slug))
        {
            return Results.BadRequest(new { error = "Custom alias already exists" });
        }
    }
    else
    {
        string? candidate = null;
        for (int i = 0; i < 5; i++)
        {
            var gen = GenerateSlug(7);
            if (!store.ContainsKey(gen))
            {
                candidate = gen;
                break;
            }
        }
        if (candidate == null)
        {
            return Results.StatusCode(StatusCodes.Status500InternalServerError);
        }
        slug = candidate;
    }

    var record = new UrlRecord
    {
        Id = slug,
        OriginalUrl = req.Url,
        ShortUrl = $"{baseUrl}/{slug}",
        ClickCount = 0,
        CreatedAt = DateTime.UtcNow.ToString("o")
    };

    if (!store.TryAdd(slug, record))
    {
        return Results.BadRequest(new { error = "Custom alias already exists" });
    }

    return Results.Created($"/api/stats/{slug}", record);
});

app.MapGet("/api/stats/{id}", (string id) =>
{
    if (store.TryGetValue(id, out var record))
    {
        return Results.Ok(record);
    }
    return Results.NotFound(new { error = "URL not found" });
});

app.MapGet("/{id}", (string id) =>
{
    if (id.StartsWith("api", StringComparison.OrdinalIgnoreCase))
    {
        return Results.NotFound(new { error = "Not found" });
    }

    if (store.TryGetValue(id, out var record))
    {
        Interlocked.Increment(ref record.ClickCount);
        return Results.Redirect(record.OriginalUrl, permanent: false);
    }

    return Results.NotFound(new { error = "URL not found" });
});

app.Run($"http://0.0.0.0:{port}");

public class UrlRecord
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = string.Empty;

    [JsonPropertyName("original_url")]
    public string OriginalUrl { get; set; } = string.Empty;

    [JsonPropertyName("short_url")]
    public string ShortUrl { get; set; } = string.Empty;

    [JsonPropertyName("click_count")]
    public long ClickCount;

    [JsonPropertyName("created_at")]
    public string CreatedAt { get; set; } = string.Empty;
}

public class ShortenRequest
{
    [JsonPropertyName("url")]
    public string? Url { get; set; }

    [JsonPropertyName("custom_alias")]
    public string? CustomAlias { get; set; }
}
