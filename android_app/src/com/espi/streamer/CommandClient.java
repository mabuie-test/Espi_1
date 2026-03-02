package com.espi.streamer;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CommandClient {
    public interface CommandHandler {
        void onStartRequested(String mode, String source);
        void onStopRequested();
    }

    private volatile boolean running;
    private final AuthManager authManager;
    private final PrivacyManager privacyManager;
    private final CommandHandler handler;
    private final DevicePolicyManager dpm;
    private final ComponentName adminComponent;

    public CommandClient(Context context, CommandHandler handler) {
        this.authManager = new AuthManager(context);
        this.privacyManager = new PrivacyManager(context);
        this.handler = handler;
        this.dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.adminComponent = new ComponentName(context, AdminReceiver.class);
    }

    public void startPolling() {
        running = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    pollOnce();
                    try {
                        Thread.sleep(3500L);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }).start();
    }

    public void stopPolling() {
        running = false;
    }

    private boolean hasAdminPrivilege() {
        return dpm != null && dpm.isAdminActive(adminComponent);
    }

    private void pollOnce() {
        if (!privacyManager.hasConsent() || !privacyManager.isRemoteControlEnabled() || !hasAdminPrivilege()) {
            return;
        }

        HttpURLConnection conn = null;
        try {
            URL url = new URL(ServerConfig.apiDeviceCommand());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + authManager.getToken());
            conn.setRequestProperty("X-Device-Token", authManager.getDeviceToken());
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() != 200) {
                return;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            JSONObject json = new JSONObject(sb.toString());
            if (!json.optBoolean("ok", false) || json.isNull("command")) {
                return;
            }
            JSONObject cmd = json.getJSONObject("command");
            String action = cmd.optString("action", "");
            if ("start".equals(action)) {
                handler.onStartRequested(cmd.optString("mode", RecordingService.MODE_AUDIO_ONLY), cmd.optString("source", "auto"));
            } else if ("stop".equals(action)) {
                handler.onStopRequested();
            }
        } catch (Exception ex) {
            Log.w("CommandClient", "Falha ao consultar comando", ex);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
