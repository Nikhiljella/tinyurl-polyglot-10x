package com.tinyurl.store;

import com.tinyurl.model.UrlRecord;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MemoryStore {
    private static final String BASE62_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, UrlRecord> store = new ConcurrentHashMap<>();

    public boolean save(UrlRecord record) {
        return store.putIfAbsent(record.getId(), record) == null;
    }

    public Optional<UrlRecord> get(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public Optional<UrlRecord> incrementClick(String id) {
        UrlRecord record = store.get(id);
        if (record != null) {
            record.incrementClick();
            return Optional.of(record);
        }
        return Optional.empty();
    }

    public boolean exists(String id) {
        return store.containsKey(id);
    }

    public String generateSlug(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE62_CHARS.charAt(RANDOM.nextInt(BASE62_CHARS.length())));
        }
        return sb.toString();
    }
}
