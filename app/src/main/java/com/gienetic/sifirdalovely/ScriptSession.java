package com.gienetic.sifirdalovely;

import java.util.ArrayList;
import java.util.List;

/** One Frida CLI process per session; tabs switch between buffered logs. */
public class ScriptSession {

    public interface LogListener {
        void onLog(int sessionId, String line);
        void onComplete(int sessionId, int exitCode);
    }

    private static final List<ScriptSession> sessions = new ArrayList<>();
    private static LogListener listener;

    public final int id;
    public final String name;
    public final String packageName;
    public final String scriptPath;
    public final StringBuilder log = new StringBuilder();

    private Process process;
    private boolean running = false;

    public ScriptSession(String name, String packageName, String scriptPath) {
        this.name = name;
        this.packageName = packageName;
        this.scriptPath = scriptPath;
        this.id = nextId++;
        sessions.add(this);
    }

    private static int nextId = 1;

    public static void setListener(LogListener l) { listener = l; }

    public static List<ScriptSession> all() { return sessions; }

    public static ScriptSession find(int id) {
        for (ScriptSession s : sessions) if (s.id == id) return s;
        return null;
    }

    public static void remove(int id) {
        ScriptSession s = find(id);
        if (s != null) {
            s.kill();
            sessions.remove(s);
        }
    }

    public static void killAll() {
        for (ScriptSession s : new ArrayList<>(sessions)) s.kill();
        sessions.clear();
    }

    public boolean isRunning() { return running; }

    public void setRunning(Process p) {
        this.process = p;
        this.running = true;
    }

    public void kill() {
        if (process != null) {
            try { process.destroy(); } catch (Exception ignored) {}
            process = null;
        }
        running = false;
    }

    public void appendLog(String line) {
        log.append(line).append("\n");
        if (listener != null) listener.onLog(id, line);
    }

    public void finish(int exitCode) {
        running = false;
        process = null;
        if (listener != null) listener.onComplete(id, exitCode);
    }
}
