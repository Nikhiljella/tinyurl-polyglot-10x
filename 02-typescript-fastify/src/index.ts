import Fastify from "fastify";
import { MemoryStore, generateSlug, UrlRecord } from "./store";

const fastify = Fastify({ logger: true });
const store = new MemoryStore();

const PORT = parseInt(process.env.PORT || "8002", 10);
const BASE_URL = process.env.BASE_URL || `http://localhost:${PORT}`;

function isValidUrl(candidate: string): boolean {
  try {
    const parsed = new URL(candidate);
    return parsed.protocol === "http:" || parsed.protocol === "https:";
  } catch {
    return false;
  }
}

// Health check
fastify.get("/api/health", async () => {
  return { status: "ok", stack: "02-typescript-fastify" };
});

// Shorten URL
fastify.post<{ Body: { url: string; custom_alias?: string } }>(
  "/api/shorten",
  async (request, reply) => {
    const { url, custom_alias } = request.body || {};

    if (!url || typeof url !== "string" || !isValidUrl(url)) {
      return reply.code(400).send({ error: "Invalid URL: must be valid http/https URL" });
    }

    let slug = "";
    if (custom_alias && typeof custom_alias === "string") {
      slug = custom_alias.trim();
      if (store.has(slug)) {
        return reply.code(400).send({ error: "Custom alias already exists" });
      }
    } else {
      for (let i = 0; i < 5; i++) {
        const candidate = generateSlug(7);
        if (!store.has(candidate)) {
          slug = candidate;
          break;
        }
      }
      if (!slug) {
        return reply.code(500).send({ error: "Failed to generate unique slug" });
      }
    }

    const record: UrlRecord = {
      id: slug,
      original_url: url,
      short_url: `${BASE_URL}/${slug}`,
      click_count: 0,
      created_at: new Date().toISOString(),
    };

    store.save(record);
    return reply.code(201).send(record);
  }
);

// Stats
fastify.get<{ Params: { id: string } }>("/api/stats/:id", async (request, reply) => {
  const { id } = request.params;
  const record = store.get(id);
  if (!record) {
    return reply.code(404).send({ error: "URL not found" });
  }
  return reply.code(200).send(record);
});

// Redirect
fastify.get<{ Params: { id: string } }>("/:id", async (request, reply) => {
  const { id } = request.params;
  if (!id || id.startsWith("api")) {
    return reply.code(404).send({ error: "Not found" });
  }

  const record = store.incrementClick(id);
  if (!record) {
    return reply.code(404).send({ error: "URL not found" });
  }

  return reply.redirect(302, record.original_url);
});

const start = async () => {
  try {
    await fastify.listen({ port: PORT, host: "0.0.0.0" });
    console.log(`02-typescript-fastify running on port ${PORT}`);
  } catch (err) {
    fastify.log.error(err);
    process.exit(1);
  }
};

start();
