package com.espi.streamer;

public final class ServerConfig {
    private ServerConfig() {}

    public static final String BASE_URL = "https://your-domain.example";
    public static final String WS_STREAM_URL = "wss://your-domain.example/ws/stream.php";
    public static final String API_LOGIN = BASE_URL + "/api/login.php";
    public static final String API_UPLOAD = BASE_URL + "/api/upload.php";
    public static final String API_DEVICE_REGISTER = BASE_URL + "/api/device_register.php";
    public static final String API_DEVICE_COMMAND = BASE_URL + "/api/device_command.php";
    public static final String API_STREAM_INGEST = BASE_URL + "/api/stream_ingest.php";
}
