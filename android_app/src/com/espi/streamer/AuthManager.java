package com.espi.streamer;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class AuthManager {
    private static final String PREFS = "auth";
    private static final String KEY_TOKEN = "jwt";
    private static final String KEY_DEVICE_TOKEN = "device_token";

    private final SharedPreferences prefs;

    public AuthManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, "");
    }

    public String getDeviceToken() {
        String token = prefs.getString(KEY_DEVICE_TOKEN, "");
        if (token.isEmpty()) {
            token = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_DEVICE_TOKEN, token).apply();
        }
        return token;
    }

    public boolean login(String baseUrl, String username, String password) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/api/login.php");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("password", password);

            OutputStream output = conn.getOutputStream();
            output.write(body.toString().getBytes("UTF-8"));
            output.flush();

            if (conn.getResponseCode() != 200) {
                return false;
            }

            BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            JSONObject json = new JSONObject(response.toString());
            String token = json.optString("token", "");
            if (token.isEmpty()) {
                return false;
            }
            prefs.edit().putString(KEY_TOKEN, token).apply();
            return true;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public boolean registerDevice(String baseUrl, String deviceName) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/api/device_register.php");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Bearer " + getToken());
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
