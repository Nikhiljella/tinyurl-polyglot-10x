<?php

use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Slim\Factory\AppFactory;
use TinyUrl\MemoryStore;

require __DIR__ . '/../vendor/autoload.php';

$app = AppFactory::create();
$app->addBodyParsingMiddleware();
$app->addRoutingMiddleware();

$store = new MemoryStore();
$port = getenv('PORT') ?: '8008';
$baseUrl = getenv('BASE_URL') ?: "http://localhost:{$port}";

$app->get('/api/health', function (Request $request, Response $response) {
    $payload = json_encode(['status' => 'ok', 'stack' => '08-php-slim']);
    $response->getBody()->write($payload);
    return $response->withHeader('Content-Type', 'application/json');
});

$app->post('/api/shorten', function (Request $request, Response $response) use ($store, $baseUrl) {
    $data = (array)$request->getParsedBody();
    $url = isset($data['url']) ? trim($data['url']) : '';

    if (empty($url) || (!str_starts_with($url, 'http://') && !str_starts_with($url, 'https://'))) {
        $response->getBody()->write(json_encode(['error' => 'Invalid URL scheme: must start with http:// or https://']));
        return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
    }

    $customAlias = isset($data['custom_alias']) ? trim($data['custom_alias']) : null;
    $slug = null;

    if ($customAlias) {
        if ($store->exists($customAlias)) {
            $response->getBody()->write(json_encode(['error' => 'Custom alias already exists']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
        }
        $slug = $customAlias;
    } else {
        for ($i = 0; $i < 5; $i++) {
            $cand = $store->generateSlug(7);
            if (!$store->exists($cand)) {
                $slug = $cand;
                break;
            }
        }
        if (!$slug) {
            $response->getBody()->write(json_encode(['error' => 'Failed to generate unique slug']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(500);
        }
    }

    $shortUrl = "{$baseUrl}/{$slug}";
    $store->save($slug, $url, $shortUrl);
    $record = $store->get($slug);

    $response->getBody()->write(json_encode($record));
    return $response->withHeader('Content-Type', 'application/json')->withStatus(201);
});

$app->get('/api/stats/{id}', function (Request $request, Response $response, array $args) use ($store) {
    $id = $args['id'];
    $record = $store->get($id);
    if (!$record) {
        $response->getBody()->write(json_encode(['error' => 'URL not found']));
        return $response->withHeader('Content-Type', 'application/json')->withStatus(404);
    }

    $response->getBody()->write(json_encode($record));
    return $response->withHeader('Content-Type', 'application/json');
});

$app->get('/{id}', function (Request $request, Response $response, array $args) use ($store) {
    $id = $args['id'];
    if (str_starts_with($id, 'api')) {
        return $response->withStatus(404);
    }

    $record = $store->incrementClick($id);
    if (!$record) {
        $response->getBody()->write(json_encode(['error' => 'URL not found']));
        return $response->withHeader('Content-Type', 'application/json')->withStatus(404);
    }

    return $response->withHeader('Location', $record['original_url'])->withStatus(302);
});

$app->run();
