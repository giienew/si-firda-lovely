package com.gienetic.sifirdalovely;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity {

    private SwitchMaterial switchServer;
    private TextView txtStatus, txtTerminal;
    private Button btnInstallFrida, btnPickFile, btnRun, btnStop;
    private EditText etPackageName, etScriptPath;
    private ScrollView scrollTerminal;

    private Process runningProcess = null;
    private static final int PICK_SCRIPT_REQUEST = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        checkStoragePermission();
        initEnvironment();
    }

    private void initViews() {
        switchServer = findViewById(R.id.switchServer);
        txtStatus = findViewById(R.id.txtStatus);
        txtTerminal = findViewById(R.id.txtTerminal);
        btnInstallFrida = findViewById(R.id.btnInstallFrida);
        btnPickFile = findViewById(R.id.btnPickFile);
        btnRun = findViewById(R.id.btnRun);
        btnStop = findViewById(R.id.btnStop);
        etPackageName = findViewById(R.id.etPackageName);
        etScriptPath = findViewById(R.id.etScriptPath);
        scrollTerminal = findViewById(R.id.scrollTerminal);

        switchServer.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                appendLog("[SYSTEM] Menjalankan frida-server daemon...");
                FridaManager.startServer(new SimpleLogCallback("Frida Server ON"));
            } else {
                appendLog("[SYSTEM] Mematikan frida-server...");
                FridaManager.stopServer(new SimpleLogCallback("Frida Server OFF"));
            }
        });

        btnPickFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_SCRIPT_REQUEST);
        });

        btnInstallFrida.setOnClickListener(v -> installFridaOnline());
        btnRun.setOnClickListener(v -> executeFridaScript());
        btnStop.setOnClickListener(v -> stopExecution());
    }

    private void initEnvironment() {
        if (!RootShell.isRootAvailable()) {
            txtStatus.setText("Status: ROOT TIDAK TERDETEKSI (KSU/Magisk/APatch dibutuhkan)");
            txtStatus.setTextColor(getColor(R.color.accent_danger));
            return;
        }

        boolean installed = FridaManager.isFridaServerInstalled();
        boolean running = FridaManager.isFridaRunning();

        if (installed) {
            String ver = FridaManager.getInstalledVersion();
            txtStatus.setText("Status: Frida Server " + ver + " (" + (running ? "RUNNING" : "STOPPED") + ")");
            txtStatus.setTextColor(getColor(R.color.accent_success));
            switchServer.setChecked(running);
            btnInstallFrida.setVisibility(View.GONE);
        } else {
            txtStatus.setText("Status: frida-server belum ada di /data/local/tmp");
            txtStatus.setTextColor(getColor(R.color.accent_danger));
            btnInstallFrida.setVisibility(View.VISIBLE);
        }
    }

    private void installFridaOnline() {
        appendLog("[DOWNLOAD] Mengambil metadata release Frida...");
        btnInstallFrida.setEnabled(false);

        new Thread(() -> {
            String dlUrl = FridaManager.getLatestDownloadUrl();
            runOnUiThread(() -> {
                if (dlUrl == null) {
                    appendLog("[ERROR] Gagal ambil URL release Frida.");
                    btnInstallFrida.setEnabled(true);
                    return;
                }
                appendLog("[DOWNLOAD] Downloading: " + dlUrl);
                String cmd = "curl -L -k '" + dlUrl + "' -o /data/local/tmp/frida.xz && " +
                             "(unxz -f /data/local/tmp/frida.xz || xz -d -f /data/local/tmp/frida.xz) && " +
                             "mv /data/local/tmp/frida* " + FridaManager.FRIDA_BIN + " 2>/dev/null; " +
                             "chmod 755 " + FridaManager.FRIDA_BIN;

                RootShell.runCommandAsync(cmd, new RootShell.LogCallback() {
                    @Override
                    public void onLog(String line) { appendLog(line); }
                    @Override
                    public void onComplete(int exitCode) {
                        appendLog("[INSTALL] Install finished with code: " + exitCode);
                        btnInstallFrida.setEnabled(true);
                        initEnvironment();
                    }
                });
            });
        }).start();
    }

    private void executeFridaScript() {
        String pkg = etPackageName.getText().toString().trim();
        String script = etScriptPath.getText().toString().trim();

        if (pkg.isEmpty()) {
            Toast.makeText(this, "Masukkan Package Name target!", Toast.LENGTH_SHORT).show();
            return;
        }

        appendLog("\n[START] Hooking target: " + pkg);
        StringBuilder cmd = new StringBuilder();
        cmd.append("export PATH=$PATH:/data/data/com.termux/files/usr/bin; ");
        cmd.append("frida -U -f ").append(pkg);
        if (!script.isEmpty()) {
            cmd.append(" -l '").append(script).append("'");
        }

        btnRun.setEnabled(false);
        btnStop.setEnabled(true);

        runningProcess = RootShell.runCommandAsync(cmd.toString(), new RootShell.LogCallback() {
            @Override
            public void onLog(String line) { appendLog(line); }
            @Override
            public void onComplete(int exitCode) {
                appendLog("[FINISH] Process exited: " + exitCode);
                btnRun.setEnabled(true);
                btnStop.setEnabled(false);
                runningProcess = null;
            }
        });
    }

    private void stopExecution() {
        if (runningProcess != null) {
            runningProcess.destroy();
            runningProcess = null;
        }
        RootShell.runCommandSync("killall -9 frida 2>/dev/null");
        appendLog("[SYSTEM] Stopped Frida execution.");
        btnRun.setEnabled(true);
        btnStop.setEnabled(false);
    }

    private void appendLog(String text) {
        txtTerminal.append(text + "\n");
        scrollTerminal.post(() -> scrollTerminal.fullScroll(View.FOCUS_DOWN));
    }

    private void checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_SCRIPT_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                etScriptPath.setText(uri.getPath());
            }
        }
    }

    private class SimpleLogCallback implements RootShell.LogCallback {
        private final String tag;
        SimpleLogCallback(String tag) { this.tag = tag; }
        @Override public void onLog(String line) { appendLog("[" + tag + "] " + line); }
        @Override public void onComplete(int exitCode) { appendLog("[" + tag + "] Exit: " + exitCode); }
    }
}
