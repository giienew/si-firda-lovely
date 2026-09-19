package com.gienetic.sifirdalovely;

import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FridaManager {

    public static final String FRIDA_BIN = "/data/local/tmp/frida-server";

    public static boolean isFridaServerInstalled() {
        String r = RootShell.runCommandSync("[ -f " + FRIDA_BIN + " ] && echo FOUND || echo NOT_FOUND");
        return r.contains("FOUND");
    }

    public static boolean isFridaRunning() {
        String r = RootShell.runCommandSync("pidof frida-server");
        return !r.isEmpty() && !r.startsWith("Error");
    }

    public static String getInstalledVersion() {
        String r = RootShell.runCommandSync(FRIDA_BIN + " --version 2>/dev/null || echo unknown");
        return r.trim();
    }

    public static void startServer(RootShell.LogCallback cb) {
        RootShell.runCommandAsync("chmod 755 " + FRIDA_BIN + " && " + FRIDA_BIN + " -D", cb);
    }

    public static void stopServer(RootShell.LogCallback cb) {
        RootShell.runCommandAsync("killall -9 frida-server 2>/dev/null && echo stopped", cb);
    }

    public static String getCpuAbi() {
        for (String abi : Build.SUPPORTED_ABIS) {
            String a = abi.toLowerCase();
            if (a.contains("arm64")) return "arm64";
            if (a.contains("arm"))   return "arm";
            if (a.contains("x86_64")) return "x86_64";
            if (a.contains("x86"))   return "x86";
        }
        return "arm64";
    }

    /** Runs on background thread — caller must wrap in new Thread() */
    public static String getLatestDownloadUrl() {
        try {
            URL url = new URL("https://api.github.com/repos/frida/frida/releases/latest");
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(15000);
            c.setRequestProperty("User-Agent", "si-firda-lovely/1.0 (Gienetic)");
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            JSONObject json = new JSONObject(sb.toString());
            String tag = json.getString("tag_name");
            JSONArray assets = json.getJSONArray("assets");
            String target = "frida-server-" + tag + "-android-" + getCpuAbi() + ".xz";
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                if (asset.getString("name").equals(target)) {
                    return asset.getString("browser_download_url");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
