CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    device_name VARCHAR(150) NOT NULL,
    device_token VARCHAR(128) NOT NULL UNIQUE,
    online_status TINYINT(1) DEFAULT 0,
    last_seen TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS media_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    device_id BIGINT NULL,
    file_name VARCHAR(255) NOT NULL,
    media_type ENUM('video_audio','audio_only') NOT NULL,
    source_name VARCHAR(80) NULL,
    stream_status ENUM('connected','paused','resumed','heartbeat','stopped','uploaded') DEFAULT 'connected',
    bytes_received BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (device_id) REFERENCES devices(id),
    UNIQUE KEY uniq_user_file (user_id, file_name)
);

CREATE TABLE IF NOT EXISTS device_commands (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    device_id BIGINT NOT NULL,
    action ENUM('start','stop') NOT NULL,
    mode ENUM('video_audio','audio_only') DEFAULT 'audio_only',
    source_name VARCHAR(80) DEFAULT 'auto',
    status ENUM('queued','delivered','done','rejected') DEFAULT 'queued',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (device_id) REFERENCES devices(id)
);

CREATE TABLE IF NOT EXISTS app_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    level VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    meta_json JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_created_at (created_at)
);
