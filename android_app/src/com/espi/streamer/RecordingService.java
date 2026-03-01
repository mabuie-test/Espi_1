package com.espi.streamer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class RecordingService extends Service {
    public static final String ACTION_START = "com.espi.streamer.START";
    public static final String ACTION_PAUSE = "com.espi.streamer.PAUSE";
    public static final String ACTION_RESUME = "com.espi.streamer.RESUME";
    public static final String ACTION_STOP = "com.espi.streamer.STOP";

    public static final String MODE_VIDEO_AUDIO = "video_audio";
    public static final String MODE_AUDIO_ONLY = "audio_only";

    private static final String CHANNEL_ID = "recording_channel";
    private static final int NOTIFICATION_ID = 1337;

    private MediaRecorder recorder;
    private File outputFile;
    private String currentMode;
    private StreamClient streamClient;
    private UploadManager uploadManager;
    private CommandClient commandClient;
    private final Handler statsHandler = new Handler(Looper.getMainLooper());
    private final Runnable statsRunnable = new Runnable() {
        @Override
        public void run() {
            if (recorder != null) {
                streamClient.sendControlEvent("heartbeat");
                statsHandler.postDelayed(this, 5000);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        streamClient = new StreamClient(getApplicationContext());
        uploadManager = new UploadManager(getApplicationContext());
        commandClient = new CommandClient(getApplicationContext(), new CommandClient.CommandHandler() {
            @Override
            public void onStartRequested(String mode, String source) {
                if (recorder == null) {
                    startRecorder(mode, source, true);
                }
            }

            @Override
            public void onStopRequested() {
                if (recorder != null) {
                    stopRecorderAndUpload();
                }
            }
        });
        createNotificationChannel();
        commandClient.startPolling();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            String mode = intent.getStringExtra("mode");
            if (mode == null) {
                mode = MODE_AUDIO_ONLY;
            }
            String source = intent.getStringExtra("source");
            if (source == null) {
                source = "auto";
            }
            startRecorder(mode, source, false);
        } else if (ACTION_PAUSE.equals(action)) {
            pauseRecorder();
        } else if (ACTION_RESUME.equals(action)) {
            resumeRecorder();
        } else if (ACTION_STOP.equals(action)) {
            stopRecorderAndUpload();
        }
        return START_STICKY;
    }

    private void startRecorder(String mode, String source, boolean remotelyTriggered) {
        stopRecorderIfRunning();
        currentMode = mode;
        recorder = new MediaRecorder();

        if (MODE_VIDEO_AUDIO.equals(mode)) {
            recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setVideoFrameRate(24);
            recorder.setVideoSize(640, 480);
            recorder.setVideoEncodingBitRate(700_000);
            outputFile = new File(getExternalFilesDir(null), "video_" + System.currentTimeMillis() + ".mp4");
        } else {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(64_000);
            outputFile = new File(getExternalFilesDir(null), "audio_" + System.currentTimeMillis() + ".m4a");
        }

        recorder.setOutputFile(outputFile.getAbsolutePath());

        try {
            startForeground(NOTIFICATION_ID, buildNotification("Transmissão ativa"));
            recorder.prepare();
            recorder.start();
            streamClient.startStreaming(currentMode, outputFile.getName(), source, remotelyTriggered);
            statsHandler.post(statsRunnable);
        } catch (IOException | RuntimeException ex) {
            Log.e("RecordingService", "Erro ao iniciar gravação", ex);
            stopSelf();
        }
    }

    private void pauseRecorder() {
        if (recorder == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        try {
            recorder.pause();
            streamClient.sendControlEvent("paused");
        } catch (RuntimeException ex) {
            Log.e("RecordingService", "Falha ao pausar", ex);
        }
    }

    private void resumeRecorder() {
        if (recorder == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        try {
            recorder.resume();
            streamClient.sendControlEvent("resumed");
        } catch (RuntimeException ex) {
            Log.e("RecordingService", "Falha ao retomar", ex);
        }
    }

    private void stopRecorderAndUpload() {
        statsHandler.removeCallbacks(statsRunnable);
        stopRecorderIfRunning();
        if (outputFile != null && outputFile.exists()) {
            File compressed = CompressionUtils.compressMedia(getApplicationContext(), outputFile, currentMode);
            uploadManager.queueUpload(compressed, currentMode);
        }
        streamClient.sendControlEvent("stopped");
        streamClient.stopStreaming();
        stopForeground(true);
        stopSelf();
    }

    private void stopRecorderIfRunning() {
        if (recorder == null) {
            return;
        }
        try {
            recorder.stop();
        } catch (RuntimeException ignored) {
        }
        recorder.reset();
        recorder.release();
        recorder = null;
    }

    private Notification buildNotification(String text) {
        Intent openIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (openIntent == null) {
            openIntent = new Intent();
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? new Notification.Builder(this, CHANNEL_ID)
            : new Notification.Builder(this);

        return builder
            .setContentTitle("ESPI transmissão ativa")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            "Gravação em background",
            NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        statsHandler.removeCallbacks(statsRunnable);
        commandClient.stopPolling();
        stopRecorderIfRunning();
        streamClient.stopStreaming();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
