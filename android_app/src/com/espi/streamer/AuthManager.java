package com.espi.streamer;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class AuthManager {
    private static final String PREFS = "auth";
    private static final String KEY_DEVICE_TOKEN = "device_token";

    private final SharedPreferences prefs;

    public AuthManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getDeviceToken() {
        String token = prefs.getString(KEY_DEVICE_TOKEN, "");
        if (token.isEmpty()) {
            token = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_DEVICE_TOKEN, token).apply();
        }
        return token;
    }

    public boolean registerDevice(String baseUrl, String deviceName) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/api/device_register.php");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("X-Device-Key", ServerConfig.DEVICE_INGEST_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject body = new JSONObject();
            body.put("device_name", deviceName);
            body.put("device_token", getDeviceToken());

            OutputStream output = conn.getOutputStream();
            output.write(body.toString().getBytes("UTF-8"));
            output.flush();

            int code = conn.getResponseCode();
            return code == 200 || code == 201;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
