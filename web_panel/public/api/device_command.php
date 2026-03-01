<?php
require __DIR__ . '/../../src/bootstrap.php';

$userId = current_user_id();
if (!$userId) {
    json_response(['error' => 'unauthorized'], 401);
}

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $deviceToken = trim((string)($_SERVER['HTTP_X_DEVICE_TOKEN'] ?? $_GET['device_token'] ?? ''));
    if ($deviceToken === '') {
        json_response(['ok' => true, 'command' => null]);
    }

    $deviceStmt = db()->prepare('SELECT id FROM devices WHERE user_id = ? AND device_token = ? LIMIT 1');
    $deviceStmt->execute([$userId, $deviceToken]);
    $device = $deviceStmt->fetch();
    if (!$device) {
        json_response(['ok' => true, 'command' => null]);
    }

    $deviceId = (int)$device['id'];
    db()->prepare('UPDATE devices SET online_status = 1, last_seen = NOW() WHERE id = ?')->execute([$deviceId]);

    $cmdStmt = db()->prepare('SELECT id, action, mode, source_name FROM device_commands WHERE user_id = ? AND device_id = ? AND status = ? ORDER BY id ASC LIMIT 1');
    $cmdStmt->execute([$userId, $deviceId, 'queued']);
    $cmd = $cmdStmt->fetch();

    if (!$cmd) {
        json_response(['ok' => true, 'command' => null]);
    }

    db()->prepare('UPDATE device_commands SET status = ?, delivered_at = NOW() WHERE id = ?')->execute(['delivered', (int)$cmd['id']]);
    json_response(['ok' => true, 'command' => [
        'id' => (int)$cmd['id'],
        'action' => $cmd['action'],
        'mode' => $cmd['mode'],
        'source' => $cmd['source_name'],
    ]]);
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $body = json_decode(file_get_contents('php://input'), true) ?: [];
    $deviceId = (int)($body['device_id'] ?? 0);
    $action = (string)($body['action'] ?? 'stop');
    $mode = (string)($body['mode'] ?? 'audio_only');
    $source = (string)($body['source'] ?? 'auto');

    $allowedActions = ['start', 'stop'];
    $allowedModes = ['video_audio', 'audio_only'];
    $allowedSources = ['Frontal', 'Traseira', 'Microfone principal', 'auto'];
    if (!in_array($action, $allowedActions, true)) {
        json_response(['error' => 'invalid_action'], 422);
    }
    if (!in_array($mode, $allowedModes, true)) {
        json_response(['error' => 'invalid_mode'], 422);
    }
    if (!in_array($source, $allowedSources, true)) {
        json_response(['error' => 'invalid_source'], 422);
    }

    $check = db()->prepare('SELECT id FROM devices WHERE id = ? AND user_id = ? LIMIT 1');
    $check->execute([$deviceId, $userId]);
    if (!$check->fetch()) {
        json_response(['error' => 'invalid_device'], 422);
    }

    $ins = db()->prepare('INSERT INTO device_commands (user_id, device_id, action, mode, source_name, status) VALUES (?, ?, ?, ?, ?, ?)');
    $ins->execute([$userId, $deviceId, $action, $mode, $source, 'queued']);
    log_event('info', 'Comando remoto enfileirado', $userId, ['device_id' => $deviceId, 'action' => $action, 'mode' => $mode, 'source' => $source]);
    json_response(['ok' => true, 'id' => (int)db()->lastInsertId()], 201);
}

json_response(['error' => 'method_not_allowed'], 405);
