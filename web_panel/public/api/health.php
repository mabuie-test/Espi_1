<?php
require __DIR__ . '/../../src/bootstrap.php';

$ok = true;
$db = true;
$userId = current_user_id();
if (!$userId) {
    json_response(['ok' => false, 'auth' => false, 'db' => false], 401);
}

try {
    db()->query('SELECT 1');
} catch (Throwable $e) {
    $ok = false;
    $db = false;
}

json_response([
    'ok' => $ok,
    'auth' => true,
    'db' => $db,
    'time' => date('c'),
]);
