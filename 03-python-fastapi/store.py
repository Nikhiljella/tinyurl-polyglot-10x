import secrets
import string
import threading
from datetime import datetime, timezone
from typing import Optional, Dict
from pydantic import BaseModel, Field

BASE62_ALPHABET = string.ascii_letters + string.digits

class URLRecord(BaseModel):
    id: str
    original_url: str
    short_url: str
    click_count: int = 0
    created_at: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())

class MemoryStore:
    def __init__(self):
        self._lock = threading.Lock()
        self._store: Dict[str, URLRecord] = {}

    def save(self, record: URLRecord) -> bool:
        with self._lock:
            if record.id in self._store:
                return False
            self._store[record.id] = record
            return True

    def get(self, id: str) -> Optional[URLRecord]:
        with self._lock:
            return self._store.get(id)

    def increment_click(self, id: str) -> Optional[URLRecord]:
        with self._lock:
            record = self._store.get(id)
            if not record:
                return None
            record.click_count += 1
            return record

    def exists(self, id: str) -> bool:
        with self._lock:
            return id in self._store

def generate_slug(length: int = 7) -> str:
    return "".join(secrets.choice(BASE62_ALPHABET) for _ in range(length))
