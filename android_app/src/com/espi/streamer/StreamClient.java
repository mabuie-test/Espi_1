package com.espi.streamer;

import android.content.Context;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class StreamClient {
    private static final int LIVE_CHUNK_SIZE = 128 * 1024;

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
            ws.connect();
        } catch (Throwable ex) {
            Log.w("StreamClient", "WebSocket indisponível, usando fallback HTTP", ex);
            ws = null;
        }

        try {
            JSONObject start = new JSONObject();
            start.put("event", "start");
            start.put("status", "connected");
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

    public long sendLiveChunk(File file, long offset, String mode, String session, String source) {
        if (file == null || !file.exists()) {
            return offset;
        }
        long length = file.length();
        if (length <= offset) {
            return offset;
        }

        HttpURLConnection conn = null;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            raf.seek(offset);
            int toRead = (int) Math.min(LIVE_CHUNK_SIZE, length - offset);
            byte[] chunk = new byte[toRead];
            int read = raf.read(chunk);
            if (read <= 0) {
                return offset;
            }

            String boundary = "----espiBoundary" + System.currentTimeMillis();
            URL url = new URL(ServerConfig.apiStreamIngest());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("X-Device-Key", ServerConfig.DEVICE_INGEST_KEY);
            conn.setRequestProperty("X-Device-Token", authManager.getDeviceToken());
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            writeField(bos, boundary, "event", "frame");
            writeField(bos, boundary, "status", "connected");
            writeField(bos, boundary, "session", session);
            writeField(bos, boundary, "mode", mode);
            writeField(bos, boundary, "source", source);
            bos.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
            bos.write("Content-Disposition: form-data; name=\"chunk\"; filename=\"live.part\"\r\n".getBytes("UTF-8"));
            bos.write("Content-Type: application/octet-stream\r\n\r\n".getBytes("UTF-8"));
            bos.write(chunk, 0, read);
            bos.write("\r\n".getBytes("UTF-8"));
            bos.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));

            OutputStream out = conn.getOutputStream();
            out.write(bos.toByteArray());
            out.flush();

            int code = conn.getResponseCode();
            if (code == 200 || code == 201) {
                return offset + read;
            }
            return offset;
        } catch (Exception ex) {
            Log.w("StreamClient", "Falha ao enviar live chunk", ex);
            return offset;
        } finally {
            try {
                if (raf != null) {
                    raf.close();
                }
            } catch (Exception ignored) {}
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void writeField(ByteArrayOutputStream bos, String boundary, String name, String value) throws Exception {
        bos.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
        bos.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes("UTF-8"));
        bos.write(value.getBytes("UTF-8"));
        bos.write("\r\n".getBytes("UTF-8"));
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
            conn.setRequestProperty("X-Device-Key", ServerConfig.DEVICE_INGEST_KEY);
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
