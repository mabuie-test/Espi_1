package com.espi.streamer;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AuthManager {
    private static final String PREFS = "auth";
    private static final String KEY_TOKEN = "jwt";

    private final SharedPreferences prefs;

    public AuthManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, "");
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
}
