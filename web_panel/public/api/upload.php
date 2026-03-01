<?php
require __DIR__ . '/../../src/bootstrap.php';

$auth = require_auth();
$userId = (int)$auth['uid'];

$fileName = basename((string)($_SERVER['HTTP_X_FILE_NAME'] ?? ''));
$mediaType = (string)($_SERVER['HTTP_X_MEDIA_TYPE'] ?? 'audio_only');
$offset = (int)($_SERVER['HTTP_X_OFFSET'] ?? 0);

if ($fileName === '') {
    json_response(['error' => 'missing_file_name'], 422);
}

$target = $config['upload_dir'] . '/' . $fileName;
$input = fopen('php://input', 'rb');
if (!$input) {
    json_response(['error' => 'invalid_body'], 400);
}

$mode = file_exists($target) ? 'c+b' : 'w+b';
$out = fopen($target, $mode);
if (!$out) {
    json_response(['error' => 'open_failed'], 500);
}

fseek($out, $offset);
$written = stream_copy_to_stream($input, $out);
fclose($input);
fclose($out);

$stmt = db()->prepare(
    'INSERT INTO media_sessions (user_id, file_name, media_type, stream_status, bytes_received)
     VALUES (?, ?, ?, ?, ?)
     ON DUPLICATE KEY UPDATE bytes_received = bytes_received + VALUES(bytes_received), stream_status = VALUES(stream_status)'
);

// Ensure unique constraint behavior by checking if row exists first.
$select = db()->prepare('SELECT id FROM media_sessions WHERE user_id = ? AND file_name = ? LIMIT 1');
$select->execute([$userId, $fileName]);
$row = $select->fetch();
if ($row) {
    $upd = db()->prepare('UPDATE media_sessions SET bytes_received = bytes_received + ?, stream_status = ? WHERE id = ?');
    $upd->execute([(int)$written, 'uploaded', (int)$row['id']]);
} else {
    $ins = db()->prepare('INSERT INTO media_sessions (user_id, file_name, media_type, stream_status, bytes_received) VALUES (?, ?, ?, ?, ?)');
    $ins->execute([$userId, $fileName, $mediaType, 'uploaded', (int)$written]);
}

log_event('info', 'Chunk recebido', $userId, [
    'file' => $fileName,
    'offset' => $offset,
    'written' => (int)$written,
    'type' => $mediaType,
]);

json_response(['ok' => true, 'written' => (int)$written], 201);
