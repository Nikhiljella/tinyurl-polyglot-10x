use axum::{
    extract::{Path, State},
    http::{header, HeaderMap, StatusCode},
    response::{IntoResponse, Json, Response},
    routing::{get, post},
    Router,
};
use chrono::{DateTime, Utc};
use rand::{distributions::Alphanumeric, Rng};
use serde::{Deserialize, Serialize};
use std::{
    collections::HashMap,
    net::SocketAddr,
    sync::{Arc, RwLock},
};

#[derive(Clone, Serialize, Deserialize, Debug)]
pub struct UrlRecord {
    pub id: String,
    pub original_url: String,
    pub short_url: String,
    pub click_count: u64,
    pub created_at: DateTime<Utc>,
}

#[derive(Default)]
pub struct AppState {
    pub urls: RwLock<HashMap<String, UrlRecord>>,
    pub base_url: String,
}

#[derive(Deserialize)]
pub struct ShortenRequest {
    pub url: String,
    pub custom_alias: Option<String>,
}

#[derive(Serialize)]
pub struct HealthResponse {
    pub status: &'static str,
    pub stack: &'static str,
}

#[derive(Serialize)]
pub struct ErrorResponse {
    pub error: String,
}

fn generate_slug(len: usize) -> String {
    rand::thread_rng()
        .sample_iter(&Alphanumeric)
        .take(len)
        .map(char::from)
        .collect()
}

async fn health_handler() -> Json<HealthResponse> {
    Json(HealthResponse {
        status: "ok",
        stack: "04-rust-axum",
    })
}

async fn shorten_handler(
    State(state): State<Arc<AppState>>,
    Json(payload): Json<ShortenRequest>,
) -> Result<(StatusCode, Json<UrlRecord>), (StatusCode, Json<ErrorResponse>)> {
    if !payload.url.starts_with("http://") && !payload.url.starts_with("https://") {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(ErrorResponse {
                error: "Invalid URL scheme: must start with http:// or https://".to_string(),
            }),
        ));
    }

    let slug = match payload.custom_alias {
        Some(alias) if !alias.trim().is_empty() => {
            let trimmed = alias.trim().to_string();
            let urls = state.urls.read().unwrap();
            if urls.contains_key(&trimmed) {
                return Err((
                    StatusCode::BAD_REQUEST,
                    Json(ErrorResponse {
                        error: "Custom alias already exists".to_string(),
                    }),
                ));
            }
            trimmed
        }
        _ => {
            let mut candidate = String::new();
            let urls = state.urls.read().unwrap();
            for _ in 0..5 {
                let gen = generate_slug(7);
                if !urls.contains_key(&gen) {
                    candidate = gen;
                    break;
                }
            }
            if candidate.is_empty() {
                return Err((
                    StatusCode::INTERNAL_SERVER_ERROR,
                    Json(ErrorResponse {
                        error: "Failed to generate unique slug".to_string(),
                    }),
                ));
            }
            candidate
        }
    };

    let record = UrlRecord {
        short_url: format!("{}/{}", state.base_url, slug),
        id: slug.clone(),
        original_url: payload.url,
        click_count: 0,
        created_at: Utc::now(),
    };

    {
        let mut urls = state.urls.write().unwrap();
        urls.insert(slug, record.clone());
    }

    Ok((StatusCode::CREATED, Json(record)))
}

async fn stats_handler(
    State(state): State<Arc<AppState>>,
    Path(id): Path<String>,
) -> Result<Json<UrlRecord>, (StatusCode, Json<ErrorResponse>)> {
    let urls = state.urls.read().unwrap();
    match urls.get(&id) {
        Some(rec) => Ok(Json(rec.clone())),
        None => Err((
            StatusCode::NOT_FOUND,
            Json(ErrorResponse {
                error: "URL not found".to_string(),
            }),
        )),
    }
}

async fn redirect_handler(
    State(state): State<Arc<AppState>>,
    Path(id): Path<String>,
) -> Response {
    if id.starts_with("api") {
        return (
            StatusCode::NOT_FOUND,
            Json(ErrorResponse {
                error: "Not found".to_string(),
            }),
        )
            .into_response();
    }

    let mut urls = state.urls.write().unwrap();
    match urls.get_mut(&id) {
        Some(record) => {
            record.click_count += 1;
            let mut headers = HeaderMap::new();
            if let Ok(loc) = header::HeaderValue::from_str(&record.original_url) {
                headers.insert(header::LOCATION, loc);
            }
            (StatusCode::FOUND, headers).into_response()
        }
        None => (
            StatusCode::NOT_FOUND,
            Json(ErrorResponse {
                error: "URL not found".to_string(),
            }),
        )
            .into_response(),
    }
}

#[tokio::main]
async fn main() {
    let port = std::env::var("PORT").unwrap_or_else(|_| "8004".to_string());
    let base_url = std::env::var("BASE_URL").unwrap_or_else(|_| format!("http://localhost:{}", port));

    let state = Arc::new(AppState {
        urls: RwLock::new(HashMap::new()),
        base_url,
    });

    let app = Router::new()
        .route("/api/health", get(health_handler))
        .route("/api/shorten", post(shorten_handler))
        .route("/api/stats/:id", get(stats_handler))
        .route("/:id", get(redirect_handler))
        .with_state(state);

    let addr: SocketAddr = format!("0.0.0.0:{}", port).parse().unwrap();
    println!("04-rust-axum listening on {}", addr);
    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}
