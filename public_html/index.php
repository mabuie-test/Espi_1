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

$candidate = $base . '/' . $path;
$real = realpath($candidate);
if ($path === '' || $path === false) {
    $target = $base . '/index.php';
} elseif ($real !== false && strpos($real, $base) === 0) {
    $target = $real;
} elseif (is_file($candidate) && strpos($candidate, $base) === 0) {
    $target = $candidate;
} else {
    $target = $base . '/index.php';
}

if (is_dir($target)) {
    $target = rtrim($target, DIRECTORY_SEPARATOR) . DIRECTORY_SEPARATOR . 'index.php';
}

if (!file_exists($target)) {
    http_response_code(404);
    echo 'Recurso não encontrado.';
    exit;
}

$ext = strtolower(pathinfo($target, PATHINFO_EXTENSION));
if ($ext === 'php') {
    require $target;
    exit;
}

$mime = function_exists('mime_content_type') ? mime_content_type($target) : 'application/octet-stream';
header('Content-Type: ' . $mime);
header('Content-Length: ' . filesize($target));
readfile($target);
