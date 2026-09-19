package com.gienetic.sifirdalovely;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class OverlayTerminalService extends Service {

    public static final String ACTION_LOG = "si_firda_lovely.LOG";
    public static final String EXTRA_LINE = "line";
    public static final String ACTION_CLEAR = "si_firda_lovely.CLEAR";
    public static final String ACTION_STOP = "si_firda_lovely.STOP";

    private static final String CHANNEL_ID = "sfl_overlay_channel";
    private static final int NOTIF_ID = 4242;

    private WindowManager wm;
    private View rootView;
    private TextView logText;
    private View logContainer;
    private WindowManager.LayoutParams rootParams;
    private boolean expanded = true;
    private boolean minimized = false;

    private final StringBuilder buffer = new StringBuilder();
    private static final int MAX_LINES = 800;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_LOG.equals(action)) {
                appendLine(intent.getStringExtra(EXTRA_LINE));
                return START_STICKY;
            }
            if (ACTION_CLEAR.equals(action)) {
                buffer.setLength(0);
                if (logText != null) logText.setText("");
                return START_STICKY;
            }
            if (ACTION_STOP.equals(action)) {
                stopSelfSafely();
                return START_NOT_STICKY;
            }
        }

        startForegroundInternal();
        showOverlay();
        return START_STICKY;
    }

    private void startForegroundInternal() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Floating Terminal", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Terminal overlay running");
            nm.createNotificationChannel(ch);
        }

        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("si firda lovely")
                .setContentText("Floating terminal active")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();

        startForeground(NOTIF_ID, n);
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private void showOverlay() {
        if (rootView != null) return;

        // Header
        TextView btnMin = new TextView(this);
        btnMin.setText("—");
        btnMin.setTextColor(Color.WHITE);
        btnMin.setTextSize(16f);
        btnMin.setPadding(dp(10), dp(4), dp(10), dp(4));

        TextView btnExpand = new TextView(this);
        btnExpand.setText("⛶");
        btnExpand.setTextColor(Color.WHITE);
        btnExpand.setTextSize(15f);
        btnExpand.setPadding(dp(10), dp(4), dp(10), dp(4));

        TextView btnCopy = new TextView(this);
        btnCopy.setText("⧉");
        btnCopy.setTextColor(Color.WHITE);
        btnCopy.setTextSize(15f);
        btnCopy.setPadding(dp(10), dp(4), dp(10), dp(4));

        TextView btnClose = new TextView(this);
        btnClose.setText("✕");
        btnClose.setTextColor(Color.parseColor("#F87171"));
        btnClose.setTextSize(15f);
        btnClose.setPadding(dp(10), dp(4), dp(10), dp(4));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(Color.parseColor("#0B1220"));
        header.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        hp.gravity = Gravity.CENTER_VERTICAL;

        TextView title = new TextView(this);
        title.setText(" si firda lovely • terminal");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(12f);
        title.setLayoutParams(hp);
        header.addView(title);
        header.addView(btnCopy);
        header.addView(btnMin);
        header.addView(btnExpand);
        header.addView(btnClose);

        // Log body
        logText = new TextView(this);
        logText.setTypeface(android.graphics.Typeface.MONOSPACE);
        logText.setTextSize(11f);
        logText.setTextColor(Color.parseColor("#38BDF8"));
        logText.setBackgroundColor(Color.parseColor("#020617"));
        logText.setPadding(dp(8), dp(8), dp(8), dp(8));
        logText.setTextIsSelectable(true);
        logText.setMovementMethod(android.text.method.ArrowKeyMovementMethod.getInstance());
        if (buffer.length() > 0) logText.setText(buffer.toString());

        // Minimized chip label
        final TextView chip = new TextView(this);
        chip.setText("▣ si firda lovely");
        chip.setTextColor(Color.WHITE);
        chip.setBackgroundColor(Color.parseColor("#0284C7"));
        chip.setPadding(dp(14), dp(8), dp(14), dp(8));
        chip.setVisibility(View.GONE);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setBackgroundColor(Color.parseColor("#020617"));
        body.addView(header);
        int bodyW = getResources().getDisplayMetrics().widthPixels - dp(16);
        body.getLayoutParams().width = bodyW;
        body.addView(logText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(220)));

        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setGravity(Gravity.CENTER);
        chipRow.addView(chip);

        rootView = new LinearLayout(this);
        ((LinearLayout) rootView).setOrientation(LinearLayout.VERTICAL);
        ((LinearLayout) rootView).addView(body);
        ((LinearLayout) rootView).addView(chipRow);

        rootParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        rootParams.gravity = Gravity.TOP | Gravity.START;
        rootParams.x = 0;
        rootParams.y = dp(60);

        try {
            wm.addView(rootView, rootParams);
        } catch (Exception e) {
            Toast.makeText(this, "Overlay gagal: " + e.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        // Drag on header
        header.setOnTouchListener(new View.OnTouchListener() {
            int initialX, initialY;
            float touchX, touchY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = rootParams.x;
                        initialY = rootParams.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        rootParams.x = initialX + (int) (event.getRawX() - touchX);
                        rootParams.y = initialY + (int) (event.getRawY() - touchY);
                        clampToBounds();
                        wm.updateViewLayout(rootView, rootParams);
                        return true;
                }
                return false;
            }
        });

        chip.setOnClickListener(v -> setMinimized(false));

        btnMin.setOnClickListener(v -> setMinimized(true));
        btnExpand.setOnClickListener(v -> setExpanded(!expanded));
        btnClose.setOnClickListener(v -> {
            stopSelfSafely();
            android.content.Intent st = new Intent(OverlayTerminalService.this, OverlayTerminalService.class);
            stopService(st);
        });
        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("si firda lovely log", logText.getText()));
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
        });
    }

    private void setMinimized(boolean min) {
        minimized = min;
        if (rootView == null) return;
        LinearLayout ll = (LinearLayout) rootView;
        ll.getChildAt(0).setVisibility(min ? View.GONE : View.VISIBLE);
        TextView chip = (TextView) ((LinearLayout) ll.getChildAt(1)).getChildAt(0);
        chip.setVisibility(min ? View.VISIBLE : View.GONE);
        try { wm.updateViewLayout(rootView, rootParams); } catch (Exception ignored) {}
    }

    private void setExpanded(boolean exp) {
        expanded = exp;
        if (rootView == null || logText == null) return;
        logText.getLayoutParams().height = exp ? dp(460) : dp(220);
        try { wm.updateViewLayout(rootView, rootParams); } catch (Exception ignored) {}
    }

    private void clampToBounds() {
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        if (rootParams.x < 0) rootParams.x = 0;
        if (rootParams.x > screenW - dp(40)) rootParams.x = screenW - dp(40);
        if (rootParams.y < 0) rootParams.y = 0;
        if (rootParams.y > screenH - dp(40)) rootParams.y = screenH - dp(40);
    }

    public static void log(Context ctx, String line) {
        Intent i = new Intent(ctx, OverlayTerminalService.class);
        i.setAction(ACTION_LOG);
        i.putExtra(EXTRA_LINE, line);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(i);
        } else {
            ctx.startService(i);
        }
    }

    public static void sendAction(Context ctx, String action) {
        Intent i = new Intent(ctx, OverlayTerminalService.class);
        i.setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(i);
        } else {
            ctx.startService(i);
        }
    }

    private void appendLine(String line) {
        if (line == null) return;
        buffer.append(line).append("\n");
        while (buffer.indexOf("\n") >= 0 && countLines(buffer) > MAX_LINES) {
            buffer.delete(0, buffer.indexOf("\n") + 1);
        }
        if (logText != null) {
            android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
            h.post(() -> {
                logText.setText(buffer.toString());
                logText.bringPointIntoView(buffer.length());
            });
        }
    }
    private int countLines(StringBuilder sb) {
        int c = 0;
        for (int i = 0; i < sb.length(); i++) if (sb.charAt(i) == '\n') c++;
        return c;
    }

    private void stopSelfSafely() {
        if (rootView != null) {
            try { wm.removeView(rootView); } catch (Exception ignored) {}
            rootView = null;
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (rootView != null) {
            try { wm.removeView(rootView); } catch (Exception ignored) {}
            rootView = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
