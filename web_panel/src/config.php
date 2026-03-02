<?php
return [
    'db' => [
        'host' => '127.0.0.1',
        'name' => 'espi_panel',
        'user' => 'espi_user',
        'pass' => 'change_me',
        'charset' => 'utf8mb4',
    ],
    'jwt_secret' => 'CHANGE_THIS_SECRET',
    'device_ingest_key' => 'CHANGE_DEVICE_KEY',
    'ingest_user_id' => 1,
    'upload_dir' => __DIR__ . '/../storage/uploads',
    'base_url' => 'https://your-domain.example',
];
