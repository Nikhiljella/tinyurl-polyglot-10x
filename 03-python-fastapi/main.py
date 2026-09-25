import os
import uvicorn
from fastapi import FastAPI, HTTPException, status
from fastapi.responses import RedirectResponse
from pydantic import BaseModel, HttpUrl
from typing import Optional
from store import MemoryStore, URLRecord, generate_slug

app = FastAPI(title="TinyURL Python FastAPI", version="1.0.0")
store = MemoryStore()

PORT = int(os.getenv("PORT", "8003"))
BASE_URL = os.getenv("BASE_URL", f"http://localhost:{PORT}")

class ShortenRequest(BaseModel):
    url: HttpUrl
    custom_alias: Optional[str] = None

@app.get("/api/health")
def health():
    return {"status": "ok", "stack": "03-python-fastapi"}

@app.post("/api/shorten", status_code=status.HTTP_201_CREATED, response_model=URLRecord)
def shorten_url(payload: ShortenRequest):
    alias = payload.custom_alias.strip() if payload.custom_alias else None
    
    if alias:
        if store.exists(alias):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Custom alias already exists"
            )
        slug = alias
    else:
        slug = None
        for _ in range(5):
            candidate = generate_slug(7)
            if not store.exists(candidate):
                slug = candidate
                break
        if not slug:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Failed to generate unique slug"
            )

    record = URLRecord(
        id=slug,
        original_url=str(payload.url),
        short_url=f"{BASE_URL}/{slug}",
        click_count=0
    )
    store.save(record)
    return record

@app.get("/api/stats/{item_id}", response_model=URLRecord)
def get_stats(item_id: str):
    record = store.get(item_id)
    if not record:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="URL not found"
        )
    return record

@app.get("/{item_id}")
def redirect_to_url(item_id: str):
    if item_id.startswith("api"):
        raise HTTPException(status_code=404, detail="Not Found")
    
    record = store.increment_click(item_id)
    if not record:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="URL not found"
        )
    return RedirectResponse(url=record.original_url, status_code=status.HTTP_302_FOUND)

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=PORT, log_level="info")
