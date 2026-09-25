import { Hono } from "hono";

interface UrlRecord {
  id: string;
  original_url: string;
  short_url: string;
  click_count: number;
  created_at: string;
}

const store = new Map<string, UrlRecord>();
const BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

function generateSlug(length = 7): string {
  let result = "";
  const random = new Uint8Array(length);
  crypto.getRandomValues(random);
  for (let i = 0; i < length; i++) {
    result += BASE62[random[i] % BASE62.length];
  }
  return result;
}

const app = new Hono();

const PORT = parseInt(process.env.PORT || "8010", 10);
const BASE_URL = process.env.BASE_URL || `http://localhost:${PORT}`;

app.get("/api/health", (c) => {
  return c.json({ status: "ok", stack: "10-bun-hono" });
});

app.post("/api/shorten", async (c) => {
  let body: any;
  try {
    body = await c.req.json();
  } catch {
    return c.json({ error: "Invalid JSON body" }, 400);
  }

  const url = body.url?.trim();
  if (!url || (!url.startsWith("http://") && !url.startsWith("https://"))) {
    return c.json({ error: "Invalid URL scheme: must start with http:// or https://" }, 400);
  }

  let slug = "";
  if (body.custom_alias && typeof body.custom_alias === "string") {
    slug = body.custom_alias.trim();
    if (store.has(slug)) {
      return c.json({ error: "Custom alias already exists" }, 400);
    }
  } else {
    for (let i = 0; i < 5; i++) {
      const cand = generateSlug(7);
      if (!store.has(cand)) {
        slug = cand;
        break;
      }
    }
    if (!slug) {
      return c.json({ error: "Failed to generate unique slug" }, 500);
    }
  }

  const record: UrlRecord = {
    id: slug,
    original_url: url,
    short_url: `${BASE_URL}/${slug}`,
    click_count: 0,
    created_at: new Date().toISOString(),
  };

  store.set(slug, record);
  return c.json(record, 201);
});

app.get("/api/stats/:id", (c) => {
  const id = c.req.param("id");
  const record = store.get(id);
  if (!record) {
    return c.json({ error: "URL not found" }, 404);
  }
  return c.json(record, 200);
});

app.get("/:id", (c) => {
  const id = c.req.param("id");
  if (id.startsWith("api")) {
    return c.text("Not found", 404);
  }

  const record = store.get(id);
  if (!record) {
    return c.json({ error: "URL not found" }, 404);
  }

  record.click_count += 1;
  return c.redirect(record.original_url, 302);
});

export default {
  port: PORT,
  fetch: app.fetch,
};
console.log(`10-bun-hono running on port ${PORT}`);
