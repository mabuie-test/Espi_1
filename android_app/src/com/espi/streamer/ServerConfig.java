package com.espi.streamer;

public final class ServerConfig {
    private ServerConfig() {}

    public static String BASE_URL = "https://your-domain.example";

    public static String wsStreamUrl() { return BASE_URL.replace("https://", "wss://") + "/ws/stream.php"; }
    public static String apiLogin() { return BASE_URL + "/api/login.php"; }
    public static String apiUpload() { return BASE_URL + "/api/upload.php"; }
    public static String apiDeviceRegister() { return BASE_URL + "/api/device_register.php"; }
    public static String apiDeviceCommand() { return BASE_URL + "/api/device_command.php"; }
    public static String apiStreamIngest() { return BASE_URL + "/api/stream_ingest.php"; }
}
