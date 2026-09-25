package main

import (
	"crypto/rand"
	"encoding/json"
	"fmt"
	"log"
	"math/big"
	"net/http"
	"net/url"
	"os"
	"strings"
	"sync"
	"time"
)

type URLRecord struct {
	ID          string    `json:"id"`
	OriginalURL string    `json:"original_url"`
	ShortURL    string    `json:"short_url"`
	ClickCount  int64     `json:"click_count"`
	CreatedAt   time.Time `json:"created_at"`
}

type MemoryStore struct {
	mu   sync.RWMutex
	urls map[string]*URLRecord
}

func NewMemoryStore() *MemoryStore {
	return &MemoryStore{
		urls: make(map[string]*URLRecord),
	}
}

func (s *MemoryStore) Save(record *URLRecord) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	if _, exists := s.urls[record.ID]; exists {
		return fmt.Errorf("alias already exists")
	}
	s.urls[record.ID] = record
	return nil
}

func (s *MemoryStore) Get(id string) (*URLRecord, bool) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	rec, exists := s.urls[id]
	return rec, exists
}

func (s *MemoryStore) IncrementClick(id string) (*URLRecord, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	rec, exists := s.urls[id]
	if !exists {
		return nil, false
	}
	rec.ClickCount++
	return rec, true
}

const base62Chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

func generateSlug(length int) (string, error) {
	var result strings.Builder
	for i := 0; i < length; i++ {
		num, err := rand.Int(rand.Reader, big.NewInt(int64(len(base62Chars))))
		if err != nil {
			return "", err
		}
		result.WriteByte(base62Chars[num.Int64()])
	}
	return result.String(), nil
}

type ShortenRequest struct {
	URL         string `json:"url"`
	CustomAlias string `json:"custom_alias,omitempty"`
}

type Server struct {
	store   *MemoryStore
	baseURL string
}

func (srv *Server) handleHealth(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{
		"status": "ok",
		"stack":  "01-go",
	})
}

func (srv *Server) handleShorten(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	var req ShortenRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || strings.TrimSpace(req.URL) == "" {
		http.Error(w, `{"error": "Invalid request body: 'url' is required"}`, http.StatusBadRequest)
		return
	}

	parsedURL, err := url.ParseRequestURI(req.URL)
	if err != nil || (parsedURL.Scheme != "http" && parsedURL.Scheme != "https") {
		http.Error(w, `{"error": "Invalid URL scheme: must start with http:// or https://"}`, http.StatusBadRequest)
		return
	}

	var slug string
	if strings.TrimSpace(req.CustomAlias) != "" {
		slug = strings.TrimSpace(req.CustomAlias)
		if _, exists := srv.store.Get(slug); exists {
			http.Error(w, `{"error": "Custom alias already exists"}`, http.StatusBadRequest)
			return
		}
	} else {
		for i := 0; i < 5; i++ {
			candidate, err := generateSlug(7)
			if err != nil {
				http.Error(w, `{"error": "Slug generation failed"}`, http.StatusInternalServerError)
				return
			}
			if _, exists := srv.store.Get(candidate); !exists {
				slug = candidate
				break
			}
		}
		if slug == "" {
			http.Error(w, `{"error": "Failed to generate unique slug"}`, http.StatusInternalServerError)
			return
		}
	}

	record := &URLRecord{
		ID:          slug,
		OriginalURL: req.URL,
		ShortURL:    fmt.Sprintf("%s/%s", srv.baseURL, slug),
		ClickCount:  0,
		CreatedAt:   time.Now().UTC(),
	}

	if err := srv.store.Save(record); err != nil {
		http.Error(w, fmt.Sprintf(`{"error": "%s"}`, err.Error()), http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(record)
}

func (srv *Server) handleStats(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	id := strings.TrimPrefix(r.URL.Path, "/api/stats/")
	if id == "" {
		http.Error(w, `{"error": "ID parameter required"}`, http.StatusBadRequest)
		return
	}

	record, found := srv.store.Get(id)
	if !found {
		http.Error(w, `{"error": "URL not found"}`, http.StatusNotFound)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(record)
}

func (srv *Server) handleRedirect(w http.ResponseWriter, r *http.Request) {
	id := strings.TrimPrefix(r.URL.Path, "/")
	if id == "" || strings.HasPrefix(id, "api/") {
		http.NotFound(w, r)
		return
	}

	record, found := srv.store.IncrementClick(id)
	if !found {
		http.Error(w, `{"error": "URL not found"}`, http.StatusNotFound)
		return
	}

	http.Redirect(w, r, record.OriginalURL, http.StatusFound)
}

func (srv *Server) Routes() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("/api/health", srv.handleHealth)
	mux.HandleFunc("/api/shorten", srv.handleShorten)
	mux.HandleFunc("/api/stats/", srv.handleStats)
	mux.HandleFunc("/", srv.handleRedirect)
	return mux
}

func main() {
	port := os.Getenv("PORT")
	if port == "" {
		port = "8001"
	}
	baseURL := os.Getenv("BASE_URL")
	if baseURL == "" {
		baseURL = fmt.Sprintf("http://localhost:%s", port)
	}

	server := &Server{
		store:   NewMemoryStore(),
		baseURL: baseURL,
	}

	addr := fmt.Sprintf(":%s", port)
	log.Printf("Starting 01-go TinyURL server on %s", addr)
	if err := http.ListenAndServe(addr, server.Routes()); err != nil {
		log.Fatalf("Server failed: %v", err)
	}
}
