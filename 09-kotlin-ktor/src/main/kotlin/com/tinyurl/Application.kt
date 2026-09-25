package com.tinyurl

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.security.SecureRandom
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Serializable
data class UrlRecord(
    val id: String,
    val original_url: String,
    val short_url: String,
    var click_count: Long,
    val created_at: String
)

@Serializable
data class ShortenRequest(
    val url: String,
    val custom_alias: String? = null
)

@Serializable
data class HealthResponse(
    val status: String,
    val stack: String
)

@Serializable
data class ErrorResponse(
    val error: String
)

class MemoryStore {
    private val records = ConcurrentHashMap<String, UrlRecord>()
    private val clicks = ConcurrentHashMap<String, AtomicLong>()
    private val base62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private val random = SecureRandom()

    fun save(record: UrlRecord): Boolean {
        if (records.putIfAbsent(record.id, record) != null) {
            return false
        }
        clicks[record.id] = AtomicLong(0)
        return true
    }

    fun get(id: String): UrlRecord? {
        val rec = records[id] ?: return null
        val currentClicks = clicks[id]?.get() ?: 0L
        return rec.copy(click_count = currentClicks)
    }

    fun incrementClick(id: String): UrlRecord? {
        val rec = records[id] ?: return null
        val count = clicks.computeIfAbsent(id) { AtomicLong(0) }.incrementAndGet()
        return rec.copy(click_count = count)
    }

    fun exists(id: String): Boolean = records.containsKey(id)

    fun generateSlug(length: Int = 7): String {
        val sb = java.lang.StringBuilder(length)
        for (i in 0 until length) {
            sb.append(base62[random.nextInt(base62.length)])
        }
        return sb.toString()
    }
}

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8009
    val baseUrl = System.getenv("BASE_URL") ?: "http://localhost:$port"
    val store = MemoryStore()

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json()
        }

        routing {
            get("/api/health") {
                call.respond(HealthResponse("ok", "09-kotlin-ktor"))
            }

            post("/api/shorten") {
                val req = try {
                    call.receive<ShortenRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request payload"))
                    return@post
                }

                if (!req.url.startsWith("http://") && !req.url.startsWith("https://")) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid URL scheme: must start with http:// or https://"))
                    return@post
                }

                val slug: String = if (!req.custom_alias.isNullOrBlank()) {
                    val alias = req.custom_alias.trim()
                    if (store.exists(alias)) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Custom alias already exists"))
                        return@post
                    }
                    alias
                } else {
                    var candidate: String? = null
                    for (i in 0 until 5) {
                        val gen = store.generateSlug(7)
                        if (!store.exists(gen)) {
                            candidate = gen
                            break
                        }
                    }
                    if (candidate == null) {
                        call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Failed to generate unique slug"))
                        return@post
                    }
                    candidate
                }

                val record = UrlRecord(
                    id = slug,
                    original_url = req.url,
                    short_url = "$baseUrl/$slug",
                    click_count = 0,
                    created_at = Instant.now().toString()
                )
                store.save(record)
                call.respond(HttpStatusCode.Created, record)
            }

            get("/api/stats/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val rec = store.get(id)
                if (rec == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("URL not found"))
                } else {
                    call.respond(HttpStatusCode.OK, rec)
                }
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                if (id.startsWith("api")) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                val rec = store.incrementClick(id)
                if (rec == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("URL not found"))
                } else {
                    call.respondRedirect(rec.original_url, permanent = false)
                }
            }
        }
    }.start(wait = true)
}
