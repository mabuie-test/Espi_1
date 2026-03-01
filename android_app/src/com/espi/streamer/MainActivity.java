package com.espi.streamer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQ_PERMISSIONS = 101;

    private TextView statusText;
    private boolean userConsent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        Button consentButton = findViewById(R.id.consentButton);
        Button startVideoAudioButton = findViewById(R.id.startVideoAudioButton);
        Button startAudioButton = findViewById(R.id.startAudioButton);
        Button pauseButton = findViewById(R.id.pauseButton);
        Button resumeButton = findViewById(R.id.resumeButton);
        Button stopButton = findViewById(R.id.stopButton);

        requestRuntimePermissions();

        consentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userConsent = true;
                statusText.setText("Consentimento registrado");
            }
        });

        startVideoAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording(RecordingService.MODE_VIDEO_AUDIO);
            }
        });

        startAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording(RecordingService.MODE_AUDIO_ONLY);
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendAction(RecordingService.ACTION_PAUSE);
                statusText.setText("Gravação pausada");
            }
        });

        resumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendAction(RecordingService.ACTION_RESUME);
                statusText.setText("Gravação retomada");
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendAction(RecordingService.ACTION_STOP);
                statusText.setText("Gravação finalizada");
            }
        });
    }

    private void requestRuntimePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = new String[] {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            };
            requestPermissions(permissions, REQ_PERMISSIONS);
        }
    }

    private void startRecording(String mode) {
        if (!userConsent) {
            Toast.makeText(this, "É obrigatório consentimento explícito.", Toast.LENGTH_LONG).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permissões não concedidas.", Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(this, RecordingService.class);
        intent.setAction(RecordingService.ACTION_START);
        intent.putExtra("mode", mode);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        statusText.setText("Gravação ativa: " + mode);
    }

    private void sendAction(String action) {
        Intent intent = new Intent(this, RecordingService.class);
        intent.setAction(action);
        startService(intent);
    }
}
