package com.espi.streamer;

import android.content.Context;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
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
            URI uri = new URI("wss://your-domain.example/ws/stream.php");
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
                    scheduleReconnect();
                }
            };
            ws.addHeader("Authorization", "Bearer " + authManager.getToken());
            ws.connect();

            JSONObject start = new JSONObject();
            start.put("event", "start");
            start.put("mode", mode);
            start.put("session", sessionName);
            start.put("source", source);
            start.put("remote", remotelyTriggered);
            sendJson(start);
        } catch (Exception ex) {
            Log.e("StreamClient", "Falha ao iniciar streaming", ex);
        }
    }

    public void sendControlEvent(String event) {
        try {
            JSONObject json = new JSONObject();
            json.put("event", event);
            json.put("free_mem_mb", memoryManager.getFreeMemoryMb());
            json.put("cpu_hint", memoryManager.cpuHint());
            sendJson(json);
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
