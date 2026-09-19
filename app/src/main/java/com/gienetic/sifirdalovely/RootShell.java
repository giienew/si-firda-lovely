package com.gienetic.sifirdalovely;

import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class RootShell {

    public interface LogCallback {
        void onLog(String line);
        void onComplete(int exitCode);
    }

    public static boolean isRootAvailable() {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("id\nexit\n");
            os.flush();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static Process runCommandAsync(String command, LogCallback callback) {
        Handler h = new Handler(Looper.getMainLooper());
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());

            new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        String l = line;
                        h.post(() -> callback.onLog(l));
                    }
                } catch (Exception ignored) {}
            }).start();

            new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        String l = "[ERR] " + line;
                        h.post(() -> callback.onLog(l));
                    }
                } catch (Exception ignored) {}
            }).start();

            new Thread(() -> {
                try {
                    int code = process.waitFor();
                    h.post(() -> callback.onComplete(code));
                } catch (Exception e) {
                    h.post(() -> callback.onComplete(-1));
                }
            }).start();

            os.writeBytes(command + "\nexit\n");
            os.flush();
            return process;

        } catch (Exception e) {
            h.post(() -> {
                callback.onLog("[EXEC ERROR] " + e.getMessage());
                callback.onComplete(-1);
            });
            return null;
        }
    }

    public static String runCommandSync(String command) {
        StringBuilder out = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes(command + "\nexit\n");
            os.flush();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) out.append(line).append("\n");
            p.waitFor();
        } catch (Exception e) {
            out.append("Error: ").append(e.getMessage());
        }
        return out.toString().trim();
    }
}
