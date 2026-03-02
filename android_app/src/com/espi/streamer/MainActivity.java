package com.espi.streamer;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQ_PERMISSIONS = 101;
    private static final int REQ_ADMIN = 202;

    private TextView statusText;
    private Spinner sourceSpinner;
    private PrivacyManager privacyManager;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            privacyManager = new PrivacyManager(this);
            authManager = new AuthManager(this);
            devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            adminComponent = new ComponentName(this, AdminReceiver.class);

            statusText = findViewById(R.id.statusText);
            sourceSpinner = findViewById(R.id.sourceSpinner);
            CheckBox remoteControlCheck = findViewById(R.id.remoteControlCheck);
            Button connectButton = findViewById(R.id.connectButton);
            Button enableAdminButton = findViewById(R.id.enableAdminButton);
            Button consentButton = findViewById(R.id.consentButton);
            Button startVideoAudioButton = findViewById(R.id.startVideoAudioButton);
            Button startAudioButton = findViewById(R.id.startAudioButton);
            Button pauseButton = findViewById(R.id.pauseButton);
            Button resumeButton = findViewById(R.id.resumeButton);
            Button stopButton = findViewById(R.id.stopButton);

            if (statusText == null || sourceSpinner == null || remoteControlCheck == null ||
                enableAdminButton == null || consentButton == null || startVideoAudioButton == null ||
                startAudioButton == null || pauseButton == null || resumeButton == null || stopButton == null ||
                connectButton == null) {
                Toast.makeText(this, "Falha ao carregar interface. Reinstale o app.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            ArrayAdapter<String> sourceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[] {"Frontal", "Traseira", "Microfone principal"});
            sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sourceSpinner.setAdapter(sourceAdapter);

            statusText.setText(privacyManager.hasConsent()
                ? "Consentimento persistido ativo"
                : "Pronto para iniciar");

            connectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final boolean ok = authManager.registerDevice(ServerConfig.BASE_URL, Build.MODEL);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    statusText.setText(ok ? "Dispositivo registado no painel" : "Falha ao registar dispositivo");
                                }
                            });
                        }
                    }).start();
                }
            });

            remoteControlCheck.setChecked(privacyManager.isRemoteControlEnabled());
            remoteControlCheck.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isAdminEnabled()) {
                        remoteControlCheck.setChecked(false);
                        Toast.makeText(MainActivity.this, "Ative admin device antes do controlo remoto.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (!privacyManager.hasConsent()) {
                        remoteControlCheck.setChecked(false);
                        Toast.makeText(MainActivity.this, "Dê consentimento explícito primeiro.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    boolean checked = remoteControlCheck.isChecked();
                    privacyManager.setRemoteControlEnabled(checked);
                    Toast.makeText(MainActivity.this,
                        checked ? "Controle remoto ativado por você" : "Controle remoto desativado", Toast.LENGTH_SHORT).show();
                }
            });

            enableAdminButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestAdminPrivileges();
                }
            });

            requestRuntimePermissions();

            consentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean first = privacyManager.setConsentOnce();
                    statusText.setText(first ? "Consentimento registado" : "Consentimento já registado anteriormente");
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
        } catch (Throwable ex) {
            Toast.makeText(this, "Erro ao iniciar app: " + ex.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private boolean isAdminEnabled() {
        return devicePolicyManager != null && devicePolicyManager.isAdminActive(adminComponent);
    }

    private void requestAdminPrivileges() {
        if (isAdminEnabled()) {
            Toast.makeText(this, "Admin device já está ativo.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Necessário para melhorar estabilidade do comando remoto com transparência.");
        startActivityForResult(intent, REQ_ADMIN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ADMIN && statusText != null) {
            statusText.setText(isAdminEnabled()
                ? "Admin device ativado com sucesso"
                : "Admin device não foi ativado");
        }
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
        if (!privacyManager.hasConsent()) {
            Toast.makeText(this, "É obrigatório consentimento explícito.", Toast.LENGTH_LONG).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permissões não concedidas.", Toast.LENGTH_LONG).show();
            return;
        }

        String source = "auto";
        if (sourceSpinner != null && sourceSpinner.getSelectedItem() != null) {
            source = sourceSpinner.getSelectedItem().toString();
        }

        Intent intent = new Intent(this, RecordingService.class);
        intent.setAction(RecordingService.ACTION_START);
        intent.putExtra("mode", mode);
        intent.putExtra("source", source);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        if (statusText != null) {
            statusText.setText("Gravação ativa: " + mode);
        }
    }

    private void sendAction(String action) {
        Intent intent = new Intent(this, RecordingService.class);
        intent.setAction(action);
        startService(intent);
    }
}
