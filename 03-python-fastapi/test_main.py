import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app, follow_redirects=False)

def test_health():
    res = client.get("/api/health")
    assert res.status_code == 200
    assert res.json()["status"] == "ok"
    assert res.json()["stack"] == "03-python-fastapi"

def test_shorten_and_redirect():
    payload = {"url": "https://example.com/fastapi", "custom_alias": "pytest-alias"}
    res = client.post("/api/shorten", json=payload)
    assert res.status_code == 201
    data = res.json()
    assert data["id"] == "pytest-alias"
    assert data["original_url"] == "https://example.com/fastapi"
    assert data["click_count"] == 0

    # Test collision
    res_dup = client.post("/api/shorten", json=payload)
    assert res_dup.status_code == 400

    # Test redirect
    res_red = client.get("/pytest-alias")
    assert res_red.status_code == 302
    assert res_red.headers["location"] == "https://example.com/fastapi"

    # Test stats
    res_stats = client.get("/api/stats/pytest-alias")
    assert res_stats.status_code == 200
    assert res_stats.json()["click_count"] == 1
