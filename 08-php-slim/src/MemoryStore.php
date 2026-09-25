<?php

namespace TinyUrl;

use PDO;

class MemoryStore {
    private PDO $pdo;
    private const BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public function __construct() {
        // Use shared in-memory SQLite database
        $dbPath = file_exists('/dev/shm') ? '/dev/shm/tinyurl.db' : sys_get_temp_dir() . '/tinyurl_mem.db';
        $this->pdo = new PDO("sqlite:{$dbPath}");
        $this->pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        
        $this->pdo->exec("
            CREATE TABLE IF NOT EXISTS urls (
                id TEXT PRIMARY KEY,
                original_url TEXT NOT NULL,
                short_url TEXT NOT NULL,
                click_count INTEGER DEFAULT 0,
                created_at TEXT NOT NULL
            )
        ");
    }

    public function save(string $id, string $originalUrl, string $shortUrl): bool {
        $stmt = $this->pdo->prepare("SELECT COUNT(*) FROM urls WHERE id = :id");
        $stmt->execute([':id' => $id]);
        if ($stmt->fetchColumn() > 0) {
            return false;
        }

        $stmt = $this->pdo->prepare("
            INSERT INTO urls (id, original_url, short_url, click_count, created_at)
            VALUES (:id, :orig, :short, 0, :created)
        ");
        return $stmt->execute([
            ':id' => $id,
            ':orig' => $originalUrl,
            ':short' => $shortUrl,
            ':created' => gmdate('Y-m-d\TH:i:s\Z')
        ]);
    }

    public function get(string $id): ?array {
        $stmt = $this->pdo->prepare("SELECT id, original_url, short_url, click_count, created_at FROM urls WHERE id = :id");
        $stmt->execute([':id' => $id]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        if (!$row) return null;
        $row['click_count'] = (int)$row['click_count'];
        return $row;
    }

    public function incrementClick(string $id): ?array {
        $stmt = $this->pdo->prepare("UPDATE urls SET click_count = click_count + 1 WHERE id = :id");
        $stmt->execute([':id' => $id]);
        if ($stmt->rowCount() === 0) {
            return null;
        }
        return $this->get($id);
    }

    public function exists(string $id): bool {
        $stmt = $this->pdo->prepare("SELECT 1 FROM urls WHERE id = :id");
        $stmt->execute([':id' => $id]);
        return (bool)$stmt->fetchColumn();
    }

    public function generateSlug(int $length = 7): string {
        $slug = '';
        $max = strlen(self::BASE62) - 1;
        for ($i = 0; $i < $length; $i++) {
            $slug .= self::BASE62[random_int(0, $max)];
        }
        return $slug;
    }
}
