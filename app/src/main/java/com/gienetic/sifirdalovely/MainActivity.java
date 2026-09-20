package com.gienetic.sifirdalovely;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ScriptSession.LogListener {

    private SwitchMaterial switchServer;
    private TextView txtStatus, txtTerminal;
    private Button btnInstallFrida, btnPickFile, btnRun, btnStop, btnScripts, btnPickApp;
    private ImageButton btnSettings;
    private EditText etPackageName, etScriptPath;
    private ScrollView scrollTerminal;

    private boolean updatingSwitch = false;
    private boolean includeSystemApps = false;
    private static final int PICK_SCRIPT_REQUEST = 101;
    private static final int REQ_OVERLAY_PERM = 202;
    private static final int REQ_NOTIF_PERM = 203;

    // Multi-script sessions
    private TabLayout tabSessions;
    private final List<ScriptSession> activeSessions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        checkStoragePermission();
        ScriptSession.setListener(this);
        initTabListener();
        bootstrapEnvironment();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bootstrapEnvironment();
    }

    // ---------------------------------------------------------------- env

    private void bootstrapEnvironment() {
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

    private void initEnvironment() {
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

    private void setControlsEnabled(boolean enabled) {
        switchServer.setEnabled(enabled);
        btnRun.setEnabled(enabled);
        btnStop.setEnabled(enabled);
        btnScripts.setEnabled(enabled);
        btnPickFile.setEnabled(enabled);
        btnPickApp.setEnabled(enabled);
        btnInstallFrida.setEnabled(enabled);
    }

    // ---------------------------------------------------------------- views

    private void initViews() {
        switchServer = findViewById(R.id.switchServer);
        txtStatus = findViewById(R.id.txtStatus);
        txtTerminal = findViewById(R.id.txtTerminal);
        btnInstallFrida = findViewById(R.id.btnInstallFrida);
        btnPickFile = findViewById(R.id.btnPickFile);
        btnRun = findViewById(R.id.btnRun);
        btnStop = findViewById(R.id.btnStop);
        btnScripts = findViewById(R.id.btnScripts);
        btnPickApp = findViewById(R.id.btnPickApp);
        btnSettings = findViewById(R.id.btnSettings);
        etPackageName = findViewById(R.id.etPackageName);
        etScriptPath = findViewById(R.id.etScriptPath);
        scrollTerminal = findViewById(R.id.scrollTerminal);
        tabSessions = findViewById(R.id.tabSessions);

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
        btnStop.setOnClickListener(v -> stopAllSessions());
        btnPickApp.setOnClickListener(v -> showAppPicker());
        btnScripts.setOnClickListener(v -> showBundledScripts());
        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        requestNotificationPermission();
    }

    // ------------------------------------------------------------- app picker

    private void showAppPicker() {
        appendLog("[PICKER] Memuat daftar aplikasi...");
        new Thread(() -> {
            List<AppListLoader.AppEntry> apps =
                    AppListLoader.loadInstalledApps(this, includeSystemApps);
            runOnUiThread(() -> {
                if (apps.isEmpty()) {
                    appendLog("[PICKER] Tidak ada aplikasi ditemukan");
                    return;
                }
                String[] labels = new String[apps.size()];
                for (int i = 0; i < apps.size(); i++) {
                    AppListLoader.AppEntry a = apps.get(i);
                    labels[i] = (a.isSystem ? "[SYS] " : "      ") + a.label + "  —  " + a.packageName;
                }
                new AlertDialog.Builder(this)
                        .setTitle("Pilih aplikasi target (" + apps.size() + ")")
                        .setItems(labels, (d, which) -> {
                            etPackageName.setText(apps.get(which).packageName);
                            appendLog("[PICKER] Target: " + apps.get(which).packageName);
                        })
                        .setNeutralButton(includeSystemApps ? "Sembunyikan system" : "Tampilkan system",
                                (d, w) -> { includeSystemApps = !includeSystemApps; showAppPicker(); })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }).start();
    }

    // ------------------------------------------------------------- execution

    private void executeFridaScript() {
        String pkg = etPackageName.getText().toString().trim();
        String script = etScriptPath.getText().toString().trim();

        if (pkg.isEmpty()) {
            Toast.makeText(this, "Pilih aplikasi target dulu!", Toast.LENGTH_SHORT).show();
            showAppPicker();
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
                String sessionName = script.isEmpty()
                        ? pkg + " (no script)"
                        : pkg + " • " + new File(script).getName();
                ScriptSession session = new ScriptSession(sessionName, pkg, script);
                activeSessions.add(session);
                refreshTabs();
                selectSession(session);

                appendLog("\n[SESSION " + session.id + "] Hooking: " + pkg);
                StringBuilder cmd = new StringBuilder();
                cmd.append("export PATH=/data/data/com.termux/files/usr/bin:/system/bin:/system/xbin; ");
                cmd.append("frida -D local -f ").append(pkg);
                if (!script.isEmpty()) {
                    cmd.append(" -l '").append(script).append("'");
                }

                btnRun.setEnabled(true);
                btnStop.setEnabled(true);

                Process p = RootShell.runCommandAsync(cmd.toString(), new RootShell.LogCallback() {
                    @Override
                    public void onLog(String line) { session.appendLog(line); }

                    @Override
                    public void onComplete(int exitCode) {
                        session.finish(exitCode);
                        runOnUiThread(() -> appendLog("[SESSION " + session.id + "] Exit: " + exitCode));
                    }
                });
                session.setRunning(p);
            });
        }).start();
    }

    // ------------------------------------------------------------- tabs

    private void initTabListener() {
        tabSessions.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Object tag = tab.getTag();
                if (tag instanceof Integer) {
                    ScriptSession s = ScriptSession.find((Integer) tag);
                    if (s != null) selectSession(s);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void refreshTabs() {
        tabSessions.removeAllTabs();
        for (ScriptSession s : activeSessions) {
            TabLayout.Tab t = tabSessions.newTab();
            t.setText((s.isRunning() ? "● " : "○ ") + s.name);
            t.setTag(s.id);
            tabSessions.addTab(t);
        }
        if (activeSessions.isEmpty()) tabSessions.setVisibility(View.GONE);
        else tabSessions.setVisibility(View.VISIBLE);
    }

    private void selectSession(ScriptSession s) {
        txtTerminal.setText(s.log.toString());
        scrollTerminal.post(() -> scrollTerminal.fullScroll(View.FOCUS_DOWN));
    }

    private void stopAllSessions() {
        for (ScriptSession s : new ArrayList<>(activeSessions)) {
            s.kill();
            appendLog("[SESSION " + s.id + "] Stopped");
        }
        activeSessions.clear();
        refreshTabs();
        btnStop.setEnabled(false);
    }

    // ------------------------------------------------------------- log output

    private void appendLog(String text) {
        txtTerminal.append(text + "\n");
        scrollTerminal.post(() -> scrollTerminal.fullScroll(View.FOCUS_DOWN));
        OverlayTerminalService.log(this, text);
    }

    // ScriptSession.LogListener
    @Override
    public void onLog(int sessionId, String line) {
        ScriptSession current = currentSession();
        if (current != null && current.id == sessionId) {
            runOnUiThread(() -> {
                txtTerminal.append(line + "\n");
                scrollTerminal.post(() -> scrollTerminal.fullScroll(View.FOCUS_DOWN));
                OverlayTerminalService.log(this, line);
            });
        }
    }

    @Override
    public void onComplete(int sessionId, int exitCode) {
        runOnUiThread(() -> {
            ScriptSession s = ScriptSession.find(sessionId);
            if (s != null) appendLog("[SESSION " + sessionId + "] Finished (" + exitCode + ")");
            refreshTabs();
        });
    }

    private ScriptSession currentSession() {
        if (activeSessions.isEmpty()) return null;
        int pos = tabSessions.getSelectedTabPosition();
        if (pos < 0 || pos >= activeSessions.size()) return activeSessions.get(activeSessions.size() - 1);
        return activeSessions.get(pos);
    }

    // ------------------------------------------------------------- frida install

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

                RootShell.runCommandAsync(cmd, new RootSessionCallback("INSTALL"));
            });
        }).start();
    }

    // ------------------------------------------------------------- script picker

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY_PERM) {
            if (Settings.canDrawOverlays(this)) {
                appendLog("[OVERLAY] Izin diberikan");
            } else {
                appendLog("[OVERLAY] Izin ditolak");
            }
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
        new AlertDialog.Builder(this)
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

    // ------------------------------------------------------------- PiP

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

    // ------------------------------------------------------------- perms

    private void checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
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

    // ------------------------------------------------------------- helpers

    private class RootSessionCallback implements RootShell.LogCallback {
        private final String tag;
        RootSessionCallback(String tag) { this.tag = tag; }
        @Override public void onLog(String line) { appendLog("[" + tag + "] " + line); }
        @Override public void onComplete(int exitCode) {
            appendLog("[" + tag + "] Exit: " + exitCode);
            if ("INSTALL".equals(tag)) {
                btnInstallFrida.setEnabled(true);
                initEnvironment();
            }
        }
    }

    private static class SimpleLogCallback implements RootShell.LogCallback {
        private final String tag;
        SimpleLogCallback(String tag) { this.tag = tag; }
        @Override public void onLog(String line) { /* swallowed, parent logs */ }
        @Override public void onComplete(int exitCode) { /* handled by caller */ }
    }
}
