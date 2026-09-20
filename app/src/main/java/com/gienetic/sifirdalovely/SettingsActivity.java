package com.gienetic.sifirdalovely;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView tvRoot = findViewById(R.id.tvRootState);
        TextView tvFrida = findViewById(R.id.tvFridaState);
        TextView tvArch = findViewById(R.id.tvArch);

        new Thread(() -> {
            boolean root = RootShell.isRootAvailable();
            boolean installed = FridaManager.isFridaServerInstalled();
            String ver = installed ? FridaManager.getInstalledVersion() : "not installed";
            runOnUiThread(() -> {
                tvRoot.setText(root ? "Root: GRANTED ✓" : "Root: NOT GRANTED");
                tvRoot.setTextColor(root
                        ? getColor(R.color.accent_success) : getColor(R.color.accent_danger));
                tvFrida.setText("Frida Server: " + ver);
                tvFrida.setTextColor(installed
                        ? getColor(R.color.accent_success) : getColor(R.color.accent_danger));
            });
        }).start();

        tvArch.setText("CPU ABI: " + FridaManager.getCpuAbi()
                + "  |  Android " + Build.VERSION.RELEASE
                + " (API " + Build.VERSION.SDK_INT + ")");

        findViewById(R.id.btnAbout).setOnClickListener(v -> showAbout());
        findViewById(R.id.btnTele).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://t.me/Gieneticc"))));
        findViewById(R.id.btnRepo).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/giienew/si-firda-lovely"))));
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("si firda lovely")
                .setMessage(getString(R.string.about_text))
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
