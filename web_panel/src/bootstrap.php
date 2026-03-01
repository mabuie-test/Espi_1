<?php
$config = require __DIR__ . '/config.php';

if (!is_dir($config['upload_dir'])) {
    mkdir($config['upload_dir'], 0775, true);
}

function db(): PDO {
    static $pdo = null;
    global $config;
    if ($pdo instanceof PDO) {
        return $pdo;
    }

    $dsn = sprintf(
        'mysql:host=%s;dbname=%s;charset=%s',
        $config['db']['host'],
        $config['db']['name'],
        $config['db']['charset']
    );

    $pdo = new PDO($dsn, $config['db']['user'], $config['db']['pass'], [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);

    return $pdo;
}

function json_response(array $data, int $status = 200): void {
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

function base64url_encode(string $data): string {
    return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
}

function base64url_decode(string $data): string {
    return base64_decode(strtr($data, '-_', '+/'));
}

function jwt_encode(array $payload, string $secret): string {
    $header = ['alg' => 'HS256', 'typ' => 'JWT'];
    $segments = [
        base64url_encode(json_encode($header)),
        base64url_encode(json_encode($payload)),
    ];
    $signature = hash_hmac('sha256', implode('.', $segments), $secret, true);
    $segments[] = base64url_encode($signature);
    return implode('.', $segments);
}

function jwt_decode(string $token, string $secret): ?array {
    $parts = explode('.', $token);
    if (count($parts) !== 3) {
        return null;
    }

    [$h, $p, $s] = $parts;
    $check = base64url_encode(hash_hmac('sha256', "$h.$p", $secret, true));
    if (!hash_equals($check, $s)) {
        return null;
    }

    $payload = json_decode(base64url_decode($p), true);
    if (!$payload || !isset($payload['exp']) || time() > (int)$payload['exp']) {
        return null;
    }
    return $payload;
}

function bearer_token(): ?string {
    $header = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
    if (stripos($header, 'Bearer ') !== 0) {
        return null;
    }
    return substr($header, 7);
}

function require_auth(): array {
    global $config;
    $token = bearer_token();
    if (!$token) {
        json_response(['error' => 'missing_token'], 401);
    }

    $payload = jwt_decode($token, $config['jwt_secret']);
    if (!$payload) {
        json_response(['error' => 'invalid_token'], 401);
    }

    return $payload;
}

function log_event(string $level, string $message, ?int $userId = null, ?array $meta = null): void {
    $stmt = db()->prepare('INSERT INTO app_logs (user_id, level, message, meta_json) VALUES (?, ?, ?, ?)');
    $stmt->execute([
        $userId,
        $level,
        $message,
        $meta ? json_encode($meta, JSON_UNESCAPED_UNICODE) : null,
    ]);
}
