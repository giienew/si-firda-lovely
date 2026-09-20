package com.gienetic.sifirdalovely;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private SwitchMaterial switchServer;
    private TextView txtStatus, txtTerminal;
    private Button btnInstallFrida, btnPickFile, btnRun, btnStop, btnOverlay, btnScripts;
    private EditText etPackageName, etScriptPath;
    private ScrollView scrollTerminal;

    private Process runningProcess = null;
    private boolean overlayActive = false;
    private boolean updatingSwitch = false;
    private static final int PICK_SCRIPT_REQUEST = 101;
    private static final int REQ_OVERLAY_PERM = 202;
    private static final int REQ_NOTIF_PERM = 203;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        checkStoragePermission();
        new Thread(() -> {
            final boolean hasRoot = RootShell.isRootAvailable();
            runOnUiThread(() -> {
                if (!hasRoot) {
                    txtStatus.setText("Status: ROOT TIDAK ADA — aktifkan root di Magisk/KSU, lalu buka ulang app");
                    txtStatus.setTextColor(getColor(R.color.accent_danger));
                    setControlsEnabled(false);
                } else {
                    initEnvironment();
                }
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(() -> {
            final boolean hasRoot = RootShell.isRootAvailable();
            runOnUiThread(() -> {
                if (!hasRoot) {
                    txtStatus.setText("Status: ROOT TIDAK ADA — aktifkan root di Magisk/KSU, lalu buka ulang app");
                    txtStatus.setTextColor(getColor(R.color.accent_danger));
                    setControlsEnabled(false);
                } else {
                    initEnvironment();
                }
            });
        }).start();
    }

    private void setControlsEnabled(boolean enabled) {
        switchServer.setEnabled(enabled);
        btnRun.setEnabled(enabled);
        btnStop.setEnabled(enabled);
        btnScripts.setEnabled(enabled);
        btnPickFile.setEnabled(enabled);
        btnOverlay.setEnabled(enabled);
        btnInstallFrida.setEnabled(enabled);
    }

    private void initViews() {
        switchServer = findViewById(R.id.switchServer);
        txtStatus = findViewById(R.id.txtStatus);
        txtTerminal = findViewById(R.id.txtTerminal);
        btnInstallFrida = findViewById(R.id.btnInstallFrida);
        btnPickFile = findViewById(R.id.btnPickFile);
        btnRun = findViewById(R.id.btnRun);
        btnStop = findViewById(R.id.btnStop);
        btnOverlay = findViewById(R.id.btnOverlay);
        btnScripts = findViewById(R.id.btnScripts);
        etPackageName = findViewById(R.id.etPackageName);
        etScriptPath = findViewById(R.id.etScriptPath);
        scrollTerminal = findViewById(R.id.scrollTerminal);

        switchServer.setOnCheckedChangeListener((btn, isChecked) -> {
            if (updatingSwitch) return;
            if (!RootShell.isRootAvailable()) {
                appendLog("[ERROR] ROOT tidak tersedia. Aktifkan root di Magisk/KSU/APatch dulu.");
                switchServer.setChecked(false);
                setControlsEnabled(false);
                return;
            }
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
            String[] mimes = {"application/javascript", "text/javascript", "text/plain", "application/octet-stream"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimes);
            startActivityForResult(intent, PICK_SCRIPT_REQUEST);
        });

        btnInstallFrida.setOnClickListener(v -> installFridaOnline());
        btnRun.setOnClickListener(v -> executeFridaScript());
        btnStop.setOnClickListener(v -> stopExecution());

        btnOverlay.setOnClickListener(v -> toggleOverlay());
        btnScripts.setOnClickListener(v -> showBundledScripts());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            btnOverlay.setVisibility(View.GONE);
        } else {
            requestNotificationPermission();
        }
    }

    private void initEnvironment() {
        // All RootShell calls here are blocking — must run off the main thread.
        new Thread(() -> {
            final boolean installed = FridaManager.isFridaServerInstalled();
            final boolean running = FridaManager.isFridaRunning();
            final String ver = installed ? FridaManager.getInstalledVersion() : "";
            runOnUiThread(() -> {
                setControlsEnabled(true);
                if (installed) {
                    txtStatus.setText("Status: Frida Server " + ver + " (" + (running ? "RUNNING" : "STOPPED") + ")");
                    txtStatus.setTextColor(getColor(R.color.accent_success));
                    updatingSwitch = true;
                    switchServer.setChecked(running);
                    updatingSwitch = false;
                    btnInstallFrida.setVisibility(View.GONE);
                } else {
                    txtStatus.setText("Status: frida-server belum ada di /data/local/tmp");
                    txtStatus.setTextColor(getColor(R.color.accent_danger));
                    btnInstallFrida.setVisibility(View.VISIBLE);
                }
            });
        }).start();
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

        btnRun.setEnabled(false);
        new Thread(() -> {
            if (!RootShell.isRootAvailable()) {
                runOnUiThread(() -> {
                    appendLog("[ERROR] ROOT tidak tersedia. Aktifkan root di Magisk/KSU/APatch dulu.");
                    txtStatus.setText("Status: ROOT TIDAK ADA — aktifkan root, lalu restart app");
                    txtStatus.setTextColor(getColor(R.color.accent_danger));
                    setControlsEnabled(false);
                    btnRun.setEnabled(true);
                });
                return;
            }

            if (!FridaManager.isFridaRunning()) {
                runOnUiThread(() -> appendLog("[SYSTEM] frida-server belum jalan, start dulu..."));
                FridaManager.startServer(new SimpleLogCallback("AutoStart"));
                try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                if (!FridaManager.isFridaRunning()) {
                    runOnUiThread(() -> {
                        appendLog("[ERROR] frida-server gagal start. Nyalakan switch Frida Server dulu.");
                        btnRun.setEnabled(true);
                    });
                    return;
                }
            }

            final String serverVer = FridaManager.getInstalledVersion().trim();
            final String clientVer = RootShell.runCommandSync("frida --version 2>/dev/null || echo unknown").trim();
            if (!"unknown".equals(clientVer) && !clientVer.isEmpty()
                    && !serverVer.isEmpty() && !serverVer.equals(clientVer)) {
                runOnUiThread(() -> appendLog("[WARN] Version mismatch! server=" + serverVer + " client=" + clientVer));
            }

            runOnUiThread(() -> {
                appendLog("\n[START] Hooking target: " + pkg);
                StringBuilder cmd = new StringBuilder();
                cmd.append("export PATH=/data/data/com.termux/files/usr/bin:/system/bin:/system/xbin; ");
                cmd.append("frida -D local -f ").append(pkg);
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
            });
        }).start();
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
        if (overlayActive) OverlayTerminalService.log(this, text);
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

    private void toggleOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !Settings.canDrawOverlays(this)) {
            appendLog("[OVERLAY] Butuh izin Display over other apps");
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, REQ_OVERLAY_PERM);
            return;
        }
        if (overlayActive) {
            OverlayTerminalService.sendAction(this, OverlayTerminalService.ACTION_STOP);
            overlayActive = false;
            btnOverlay.setText("Floating Terminal");
            appendLog("[OVERLAY] Ditutup");
        } else {
            Intent i = new Intent(this, OverlayTerminalService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i);
            else startService(i);
            overlayActive = true;
            btnOverlay.setText("Close Overlay");
            appendLog("[OVERLAY] Aktif — drag header, scroll, pilih teks");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY_PERM) {
            if (Settings.canDrawOverlays(this)) toggleOverlay();
            else appendLog("[OVERLAY] Izin ditolak");
            return;
        }
        if (requestCode == PICK_SCRIPT_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;
            File cached = copyUriToCache(uri);
            if (cached != null) etScriptPath.setText(cached.getAbsolutePath());
            else appendLog("[ERROR] Gagal copy script ke cache");
        }
    }

    private File copyUriToCache(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            File out = new File(getCacheDir(), "script_" + System.currentTimeMillis() + ".js");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                byte[] b = new byte[8192];
                int n;
                while ((n = in.read(b)) > 0) fos.write(b, 0, n);
            }
            return out;
        } catch (Exception e) {
            appendLog("[ERROR] copyUriToCache: " + e.getMessage());
            return null;
        }
    }

    private void showBundledScripts() {
        String[] files;
        try {
            files = getAssets().list("scripts");
        } catch (Exception e) {
            appendLog("[ERROR] Tidak bisa baca scripts: " + e.getMessage());
            return;
        }
        if (files == null || files.length == 0) {
            appendLog("[SCRIPTS] Tidak ada script bundled");
            return;
        }
        List<String> js = new ArrayList<>();
        Collections.addAll(js, files);
        js.remove("README.md");
        Collections.sort(js);

        String[] arr = js.toArray(new String[0]);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Bundled Scripts (" + arr.length + ")")
                .setItems(arr, (d, which) -> {
                    String name = arr[which];
                    File cached = copyAssetToCache("scripts/" + name, name);
                    if (cached != null) {
                        etScriptPath.setText(cached.getAbsolutePath());
                        appendLog("[SCRIPTS] Dipilih: " + name);
                    } else appendLog("[ERROR] Gagal copy " + name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private File copyAssetToCache(String assetPath, String name) {
        try (InputStream in = getAssets().open(assetPath)) {
            File out = new File(getCacheDir(), "bundled_" + name);
            try (FileOutputStream fos = new FileOutputStream(out)) {
                byte[] b = new byte[8192];
                int n;
                while ((n = in.read(b)) > 0) fos.write(b, 0, n);
            }
            return out;
        } catch (Exception e) {
            appendLog("[ERROR] copyAssetToCache: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                enterPictureInPictureMode(
                        new android.app.PictureInPictureParams.Builder().build());
            } catch (Exception ignored) {}
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF_PERM);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIF_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                appendLog("[SYSTEM] Notifikasi diizinkan");
            else
                appendLog("[WARN] Notifikasi diblokir — overlay tetap jalan, notifikasi service tidak muncul");
        }
    }

    private class SimpleLogCallback implements RootShell.LogCallback {
        private final String tag;
        SimpleLogCallback(String tag) { this.tag = tag; }
        @Override public void onLog(String line) { appendLog("[" + tag + "] " + line); }
        @Override public void onComplete(int exitCode) { appendLog("[" + tag + "] Exit: " + exitCode); }
    }
}
