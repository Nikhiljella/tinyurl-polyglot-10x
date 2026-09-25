package com.tinyurl.model;

import java.time.Instant;

public class UrlRecord {
    private String id;
    private String original_url;
    private String short_url;
    private long click_count;
    private String created_at;

    public UrlRecord() {
    }

    public UrlRecord(String id, String originalUrl, String shortUrl) {
        this.id = id;
        this.original_url = originalUrl;
        this.short_url = shortUrl;
        this.click_count = 0;
        this.created_at = Instant.now().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOriginal_url() {
        return original_url;
    }

    public void setOriginal_url(String original_url) {
        this.original_url = original_url;
    }

    public String getShort_url() {
        return short_url;
    }

    public void setShort_url(String short_url) {
        this.short_url = short_url;
    }

    public long getClick_count() {
        return click_count;
    }

    public void setClick_count(long click_count) {
        this.click_count = click_count;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public synchronized void incrementClick() {
        this.click_count++;
    }
}
