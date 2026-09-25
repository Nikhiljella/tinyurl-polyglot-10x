package com.tinyurl.controller;

import com.tinyurl.model.ShortenRequest;
import com.tinyurl.model.UrlRecord;
import com.tinyurl.store.MemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
public class TinyUrlController {

    private final MemoryStore store;
    private final String baseUrl;

    public TinyUrlController(MemoryStore store, @Value("${app.base-url}") String baseUrl) {
        this.store = store;
        this.baseUrl = baseUrl;
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "stack", "05-java-spring"
        ));
    }

    @PostMapping("/api/shorten")
    public ResponseEntity<?> shorten(@RequestBody ShortenRequest request) {
        if (request.getUrl() == null || (!request.getUrl().startsWith("http://") && !request.getUrl().startsWith("https://"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid URL scheme: must start with http:// or https://"));
        }

        String slug;
        if (request.getCustom_alias() != null && !request.getCustom_alias().trim().isEmpty()) {
            slug = request.getCustom_alias().trim();
            if (store.exists(slug)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Custom alias already exists"));
            }
        } else {
            String candidate = null;
            for (int i = 0; i < 5; i++) {
                String gen = store.generateSlug(7);
                if (!store.exists(gen)) {
                    candidate = gen;
                    break;
                }
            }
            if (candidate == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to generate unique slug"));
            }
            slug = candidate;
        }

        UrlRecord record = new UrlRecord(slug, request.getUrl(), baseUrl + "/" + slug);
        store.save(record);
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }

    @GetMapping("/api/stats/{id}")
    public ResponseEntity<?> stats(@PathVariable String id) {
        return store.get(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "URL not found")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> redirect(@PathVariable String id) {
        if (id.startsWith("api")) {
            return ResponseEntity.notFound().build();
        }
        return store.incrementClick(id)
                .<ResponseEntity<?>>map(rec -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setLocation(URI.create(rec.getOriginal_url()));
                    return new ResponseEntity<>(headers, HttpStatus.FOUND);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "URL not found")));
    }
}
