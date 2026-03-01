<?php
require __DIR__ . '/../../src/bootstrap.php';

$body = json_decode(file_get_contents('php://input'), true);
$username = trim((string)($body['username'] ?? ''));
$password = (string)($body['password'] ?? '');

if ($username === '' || $password === '') {
    json_response(['error' => 'missing_credentials'], 422);
}

$stmt = db()->prepare('SELECT id, password_hash FROM users WHERE username = ? LIMIT 1');
$stmt->execute([$username]);
$user = $stmt->fetch();

if (!$user || !password_verify($password, $user['password_hash'])) {
    log_event('warning', 'Tentativa de login inválida', null, ['username' => $username]);
    json_response(['error' => 'invalid_credentials'], 401);
}

$token = jwt_encode([
    'uid' => (int)$user['id'],
    'usr' => $username,
    'iat' => time(),
    'exp' => time() + 3600,
], $config['jwt_secret']);

log_event('info', 'Login realizado', (int)$user['id']);
json_response(['token' => $token]);
