<?php
require __DIR__ . '/../../src/bootstrap.php';

$auth = require_auth();
$userId = (int)$auth['uid'];
$body = json_decode(file_get_contents('php://input'), true) ?: [];
$name = trim((string)($body['device_name'] ?? 'Android'));
$token = trim((string)($body['device_token'] ?? ''));

if ($token === '') {
    json_response(['error' => 'missing_device_token'], 422);
}

$check = db()->prepare('SELECT id FROM devices WHERE device_token = ? LIMIT 1');
$check->execute([$token]);
$row = $check->fetch();
if ($row) {
    db()->prepare('UPDATE devices SET user_id = ?, device_name = ?, online_status = 1, last_seen = NOW() WHERE id = ?')
        ->execute([$userId, $name, (int)$row['id']]);
    json_response(['ok' => true, 'device_id' => (int)$row['id']]);
}

$ins = db()->prepare('INSERT INTO devices (user_id, device_name, device_token, online_status, last_seen) VALUES (?, ?, ?, 1, NOW())');
$ins->execute([$userId, $name, $token]);
json_response(['ok' => true, 'device_id' => (int)db()->lastInsertId()], 201);
