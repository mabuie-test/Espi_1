package com.espi.streamer;

import android.content.Context;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class StreamClient {
    private WebSocketClient ws;
    private final AuthManager authManager;
    private final MemoryManager memoryManager;

    public StreamClient(Context context) {
        this.authManager = new AuthManager(context);
        this.memoryManager = new MemoryManager();
    }

    public void startStreaming(String mode, String sessionName, String source, boolean remotelyTriggered) {
        try {
            URI uri = new URI(ServerConfig.wsStreamUrl());
            ws = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    sendControlEvent("connected");
                }

                @Override
                public void onMessage(String message) {
                    Log.d("StreamClient", "Mensagem servidor: " + message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.w("StreamClient", "WebSocket fechado: " + reason);
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    Log.e("StreamClient", "Erro websocket", ex);
                }
            };
            ws.addHeader("Authorization", "Bearer " + authManager.getToken());
            ws.connect();
        } catch (Throwable ex) {
            Log.w("StreamClient", "WebSocket indisponível, usando fallback HTTP", ex);
            ws = null;
        }

        try {
            JSONObject start = new JSONObject();
            start.put("event", "start");
            start.put("mode", mode);
            start.put("session", sessionName);
            start.put("source", source);
            start.put("remote", remotelyTriggered);
            sendJson(start);
            postEventHttp(start);
        } catch (Exception ex) {
            Log.e("StreamClient", "Falha ao enviar evento start", ex);
        }
    }

    public void sendControlEvent(String event) {
        try {
            JSONObject json = new JSONObject();
            json.put("event", event);
            json.put("status", event);
            json.put("free_mem_mb", memoryManager.getFreeMemoryMb());
            json.put("cpu_hint", memoryManager.cpuHint());
            sendJson(json);
            postEventHttp(json);
        } catch (Exception ignored) {
        }
    }

    public void stopStreaming() {
        if (ws != null) {
            try {
                ws.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void sendJson(JSONObject json) {
        if (ws != null && ws.isOpen()) {
            ws.send(json.toString());
        }
    }

    private void postEventHttp(JSONObject json) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(ServerConfig.apiStreamIngest());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Bearer " + authManager.getToken());
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Device-Token", authManager.getDeviceToken());
            OutputStream output = conn.getOutputStream();
            output.write(json.toString().getBytes("UTF-8"));
            output.flush();
            conn.getResponseCode();
        } catch (Exception ex) {
            Log.w("StreamClient", "Falha no fallback HTTP stream event", ex);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void scheduleReconnect() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (ws != null && !ws.isOpen()) {
                        ws.reconnect();
                    }
                } catch (Exception ignored) {
                }
            }
        }, 3000);
    }
}
