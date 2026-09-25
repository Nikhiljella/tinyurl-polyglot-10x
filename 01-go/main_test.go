package main

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestGoTinyURL(t *testing.T) {
	srv := &Server{
		store:   NewMemoryStore(),
		baseURL: "http://localhost:8001",
	}
	handler := srv.Routes()

	// 1. Health check
	req := httptest.NewRequest(http.MethodGet, "/api/health", nil)
	w := httptest.NewRecorder()
	handler.ServeHTTP(w, req)
	if w.Code != http.StatusOK {
		t.Fatalf("expected status 200, got %d", w.Code)
	}

	// 2. Shorten URL
	payload := []byte(`{"url":"https://example.com","custom_alias":"testlink"}`)
	req = httptest.NewRequest(http.MethodPost, "/api/shorten", bytes.NewBuffer(payload))
	w = httptest.NewRecorder()
	handler.ServeHTTP(w, req)
	if w.Code != http.StatusCreated {
		t.Fatalf("expected status 201, got %d: %s", w.Code, w.Body.String())
	}

	var rec URLRecord
	if err := json.Unmarshal(w.Body.Bytes(), &rec); err != nil {
		t.Fatalf("failed to decode response: %v", err)
	}
	if rec.ID != "testlink" || rec.OriginalURL != "https://example.com" {
		t.Fatalf("unexpected record: %+v", rec)
	}

	// 3. Collision check
	req = httptest.NewRequest(http.MethodPost, "/api/shorten", bytes.NewBuffer(payload))
	w = httptest.NewRecorder()
	handler.ServeHTTP(w, req)
	if w.Code != http.StatusBadRequest {
		t.Fatalf("expected 400 for duplicate alias, got %d", w.Code)
	}

	// 4. Redirect
	req = httptest.NewRequest(http.MethodGet, "/testlink", nil)
	w = httptest.NewRecorder()
	handler.ServeHTTP(w, req)
	if w.Code != http.StatusFound {
		t.Fatalf("expected 302 redirect, got %d", w.Code)
	}
	if loc := w.Header().Get("Location"); loc != "https://example.com" {
		t.Fatalf("expected redirect to https://example.com, got %s", loc)
	}

	// 5. Stats
	req = httptest.NewRequest(http.MethodGet, "/api/stats/testlink", nil)
	w = httptest.NewRecorder()
	handler.ServeHTTP(w, req)
	if w.Code != http.StatusOK {
		t.Fatalf("expected 200 stats, got %d", w.Code)
	}
	var stats URLRecord
	json.Unmarshal(w.Body.Bytes(), &stats)
	if stats.ClickCount != 1 {
		t.Fatalf("expected click_count 1, got %d", stats.ClickCount)
	}
}
