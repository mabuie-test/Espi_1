<?php
require __DIR__ . '/../../src/bootstrap.php';

$auth = require_auth();
$userId = (int)$auth['uid'];
$deviceToken = trim((string)($_POST['device_token'] ?? $_SERVER['HTTP_X_DEVICE_TOKEN'] ?? ''));

$event = $_POST['event'] ?? 'frame';
$fileName = basename((string)($_POST['session'] ?? $_POST['file'] ?? ('live_' . $userId)));
$mediaType = (string)($_POST['mode'] ?? 'video_audio');
$status = (string)($_POST['status'] ?? 'connected');
$source = (string)($_POST['source'] ?? 'auto');

$deviceId = null;
if ($deviceToken !== '') {
    $stmt = db()->prepare('SELECT id FROM devices WHERE user_id = ? AND device_token = ? LIMIT 1');
    $stmt->execute([$userId, $deviceToken]);
    $device = $stmt->fetch();
    if ($device) {
        $deviceId = (int)$device['id'];
        db()->prepare('UPDATE devices SET online_status = 1, last_seen = NOW() WHERE id = ?')->execute([$deviceId]);
    }
}

$frameBytes = 0;
if (isset($_FILES['chunk']) && is_uploaded_file($_FILES['chunk']['tmp_name'])) {
    $tmp = $_FILES['chunk']['tmp_name'];
    $target = $config['upload_dir'] . '/live_' . $fileName . '.part';
    $content = file_get_contents($tmp);
    file_put_contents($target, $content, FILE_APPEND);
    $frameBytes = strlen($content);
}

$select = db()->prepare('SELECT id FROM media_sessions WHERE user_id = ? AND file_name = ? LIMIT 1');
$select->execute([$userId, $fileName]);
$row = $select->fetch();
if ($row) {
    $upd = db()->prepare('UPDATE media_sessions SET stream_status = ?, bytes_received = bytes_received + ?, source_name = ?, device_id = COALESCE(?, device_id) WHERE id = ?');
    $upd->execute([$status, $frameBytes, $source, $deviceId, (int)$row['id']]);
} else {
    $ins = db()->prepare('INSERT INTO media_sessions (user_id, device_id, file_name, media_type, source_name, stream_status, bytes_received) VALUES (?, ?, ?, ?, ?, ?, ?)');
    $ins->execute([$userId, $deviceId, $fileName, $mediaType, $source, $status, $frameBytes]);
}

log_event('info', 'Evento de streaming', $userId, [
    'event' => $event,
    'file' => $fileName,
    'status' => $status,
    'bytes' => $frameBytes,
    'source' => $source,
]);

json_response(['ok' => true, 'event' => $event, 'bytes' => $frameBytes]);
