<?php
/**
 * Front controller para hospedagens com webroot fixo em public_html.
 * Encaminha todas as rotas para ../web_panel/public.
 */

$base = realpath(__DIR__ . '/../web_panel/public');
if (!$base || !is_dir($base)) {
    http_response_code(500);
    echo 'Diretório web_panel/public não encontrado.';
    exit;
}

$uri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';
$path = ltrim($uri, '/');
$target = realpath($base . '/' . $path);

if ($target === false || strpos($target, $base) !== 0) {
    // fallback para index principal
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
