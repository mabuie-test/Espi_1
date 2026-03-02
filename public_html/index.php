<?php
$base = realpath(__DIR__ . '/../web_panel/public');
if (!$base || !is_dir($base)) {
    http_response_code(500);
    echo 'Diretório web_panel/public não encontrado.';
    exit;
}

$uri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';
$uri = preg_replace('#^/index\.php#', '', $uri);
$path = ltrim($uri, '/');
$path = str_replace('..', '', $path);

if ($path === '') {
    $target = $base . '/index.php';
} else {
    $target = $base . '/' . $path;
}

if (is_dir($target)) {
    $target = rtrim($target, DIRECTORY_SEPARATOR) . DIRECTORY_SEPARATOR . 'index.php';
}

if (!is_file($target)) {
    $target = $base . '/index.php';
}

$targetReal = realpath($target);
if ($targetReal === false || strpos($targetReal, $base) !== 0) {
    http_response_code(403);
    echo 'Acesso inválido.';
    exit;
}

$ext = strtolower(pathinfo($targetReal, PATHINFO_EXTENSION));
if ($ext === 'php') {
    require $targetReal;
    exit;
}

$map = [
    'css' => 'text/css; charset=UTF-8',
    'js' => 'application/javascript; charset=UTF-8',
    'json' => 'application/json; charset=UTF-8',
    'png' => 'image/png',
    'jpg' => 'image/jpeg',
    'jpeg' => 'image/jpeg',
    'gif' => 'image/gif',
    'svg' => 'image/svg+xml',
    'mp4' => 'video/mp4',
    'm4a' => 'audio/mp4',
];
$mime = $map[$ext] ?? (function_exists('mime_content_type') ? mime_content_type($targetReal) : false);
if (!$mime) {
    $mime = 'application/octet-stream';
}
header('Content-Type: ' . $mime);

header('Content-Length: ' . filesize($targetReal));
readfile($targetReal);
