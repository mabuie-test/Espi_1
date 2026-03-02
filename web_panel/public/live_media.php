<?php
require __DIR__ . '/../src/bootstrap.php';
$userId = require_web_session();
$deviceId = (int)($_GET['device_id'] ?? 0);
if ($deviceId <= 0) {
    http_response_code(422);
    exit('device_id inválido');
}

$stmt = db()->prepare('SELECT file_name, media_type FROM media_sessions WHERE user_id = ? AND device_id = ? ORDER BY id DESC LIMIT 1');
$stmt->execute([$userId, $deviceId]);
$row = $stmt->fetch();
if (!$row) {
    http_response_code(404);
    exit('Sessão não encontrada');
}

$liveName = 'live_' . $row['file_name'] . '.part';
$path = realpath($config['upload_dir'] . '/' . $liveName);
$root = realpath($config['upload_dir']);
if (!$path || !$root || strpos($path, $root) !== 0 || !is_file($path)) {
    http_response_code(404);
    exit('Live não disponível');
}

$type = $row['media_type'] === 'audio_only' ? 'audio/aac' : 'video/mp2t';
header('Content-Type: ' . $type);
header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');
header('Pragma: no-cache');
header('Content-Length: ' . filesize($path));
readfile($path);
