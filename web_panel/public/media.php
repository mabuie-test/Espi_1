<?php
require __DIR__ . '/../src/bootstrap.php';

$file = basename((string)($_GET['f'] ?? ''));
$path = realpath($config['upload_dir'] . '/' . $file);
$root = realpath($config['upload_dir']);

if (!$path || !$root || strpos($path, $root) !== 0 || !is_file($path)) {
    http_response_code(404);
    exit('Arquivo não encontrado');
}

$ext = strtolower(pathinfo($path, PATHINFO_EXTENSION));
$contentType = 'application/octet-stream';
if ($ext === 'mp4') {
    $contentType = 'video/mp4';
} elseif ($ext === 'm4a') {
    $contentType = 'audio/mp4';
}
header('Content-Type: ' . $contentType);
header('Content-Length: ' . filesize($path));
readfile($path);
