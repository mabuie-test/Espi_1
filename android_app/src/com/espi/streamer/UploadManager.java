package com.espi.streamer;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadManager {
    private static final int CHUNK_SIZE = 512 * 1024;
    private final AuthManager authManager;

    public UploadManager(Context context) {
        this.authManager = new AuthManager(context);
    }

    public void queueUpload(File file, String mediaType) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                uploadWithResume(file, mediaType);
            }
        }).start();
    }

    private void uploadWithResume(File file, String mediaType) {
        int attempts = 0;
        long offset = 0;

        while (attempts < 5 && offset < file.length()) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(ServerConfig.apiUpload());
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("X-Device-Key", ServerConfig.DEVICE_INGEST_KEY);
                conn.setRequestProperty("X-Device-Token", authManager.getDeviceToken());
                conn.setRequestProperty("X-File-Name", file.getName());
                conn.setRequestProperty("X-Media-Type", mediaType);
                conn.setRequestProperty("X-Offset", String.valueOf(offset));
                conn.setRequestProperty("Content-Type", "application/octet-stream");

                RandomAccessFile raf = new RandomAccessFile(file, "r");
                raf.seek(offset);
                byte[] buffer = new byte[CHUNK_SIZE];
                int read = raf.read(buffer);
                if (read <= 0) {
                    raf.close();
                    break;
                }
                conn.getOutputStream().write(buffer, 0, read);
                conn.getOutputStream().flush();
                raf.close();

                int status = conn.getResponseCode();
                if (status == 200 || status == 201) {
                    offset += read;
                    attempts = 0;
                } else {
                    attempts++;
                    Thread.sleep(1200L * attempts);
                }
            } catch (Exception ex) {
                attempts++;
                Log.e("UploadManager", "Upload falhou, tentativa " + attempts, ex);
                try {
                    Thread.sleep(1200L * attempts);
                } catch (InterruptedException ignored) {
                }
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        sendFinalizationEvent(file, mediaType, offset >= file.length());
    }

    private void sendFinalizationEvent(File file, String mediaType, boolean success) {
        try {
            JSONObject json = new JSONObject();
            json.put("file", file.getName());
            json.put("type", mediaType);
            json.put("success", success);
            Log.i("UploadManager", json.toString());
        } catch (Exception ignored) {
        }
    }
}
