package com.kulwant.atvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Calendar;

/**
 * Relógio sempre visível sobre qualquer app (TYPE_APPLICATION_OVERLAY).
 * Mostrado quando o launcher vai para segundo plano, escondido quando volta.
 * Não é focável nem tocável: o D-pad continua indo para o app em primeiro plano.
 */
final class ClockOverlay {

    private static final String TAG = "ClockOverlay";
    private static final String[] WEEKDAYS = {"Dom.", "Seg.", "Ter.", "Qua.", "Qui.", "Sex.", "S\u00e1b."};

    private static ClockOverlay instance;

    private final Context app;
    private final WindowManager wm;
    private final Handler main = new Handler(Looper.getMainLooper());
    private TextView view;
    private boolean showing;

    private ClockOverlay(Context c) {
        app = c.getApplicationContext();
        wm = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
    }

    static synchronized void init(Context c) {
        if (instance == null) instance = new ClockOverlay(c);
    }

    static void show(Context c) {
        init(c);
        if (instance != null) instance.doShow();
    }

    static void hide(Context c) {
        if (instance != null) instance.doHide();
    }

    static void refresh(Context c) {
        if (instance != null) instance.doRefresh();
    }

    private void doShow() {
        Log.e(TAG, "doShow() chamado (showing=" + showing + " view=" + (view != null) + ")");
        if (showing) { update(); return; }
        if (!Settings.canDrawOverlays(app)) {
            Log.e(TAG, "sem SYSTEM_ALERT_WINDOW");
            return;
        }

        SharedPreferences p = app.getSharedPreferences("home", Context.MODE_PRIVATE);
        if (!p.getBoolean("overlay_enabled", true)) {
            Log.e(TAG, "overlay desativado nas configuracoes");
            return;
        }
        int pos = p.getInt("overlay_position", 1);   // 0=TL 1=TR 2=BL 3=BR

        if (view == null) {
            view = new TextView(app);
            view.setTextColor(0xFFFFFFFF);
            view.setShadowLayer(4f, 0f, 1f, 0xCC000000);
            view.setTextSize(12);
            view.setPadding(dp(12), dp(6), dp(12), dp(6));
            view.setBackground(Ui.rounded(0x66000000, dp(12), dp(1), 0x33FFFFFF));
            view.setGravity(Gravity.CENTER);
        }

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        switch (pos) {
            case 0:  lp.gravity = Gravity.TOP | Gravity.START;    break;  // TL
            case 2:  lp.gravity = Gravity.BOTTOM | Gravity.START; break;  // BL
            case 3:  lp.gravity = Gravity.BOTTOM | Gravity.END;   break;  // BR
            case 1:
            default: lp.gravity = Gravity.TOP | Gravity.END;      break;  // TR
        }
        lp.x = dp(16);
        lp.y = dp(12);

        try {
            wm.addView(view, lp);
            Log.e(TAG, "overlay adicionado (pos=" + pos + ")");
        } catch (Exception e) {
            Log.e(TAG, "addView falhou", e);
            return;
        }
        showing = true;
        update();
        schedule();
    }

    private void doHide() {
        Log.e(TAG, "doHide() chamado (showing=" + showing + ")");
        main.removeCallbacks(tick);
        if (view != null && showing) {
            try {
                wm.removeView(view);
                Log.e(TAG, "overlay removido");
            } catch (Exception e) {
                Log.e(TAG, "removeView falhou", e);
            }
        }
        showing = false;
        view = null;   // <- corrige bug: recria na próxima chamada
    }

    private void doRefresh() {
        if (showing) update();
    }

    private void update() {
        if (view == null) return;
        view.setText(format());
    }

    private final Runnable tick = () -> {
        update();
        schedule();
    };

    private void schedule() {
        main.removeCallbacks(tick);
        long now = System.currentTimeMillis();
        main.postDelayed(tick, 60_000 - now % 60_000 + 50);
    }

    private String format() {
        SharedPreferences p = app.getSharedPreferences("home", Context.MODE_PRIVATE);
        int preset = p.getInt("clock_preset", 0);
        int mode = p.getInt("clock_24h_mode", 0);
        boolean secs = p.getBoolean("clock_show_seconds", false);

        Calendar now = Calendar.getInstance();
        boolean h24 = (mode == 1) || (mode == 0 && DateFormat.is24HourFormat(app));
        String tp = h24 ? (secs ? "HH:mm:ss" : "HH:mm") : (secs ? "h:mm:ss a" : "h:mm a");
        String time = DateFormat.format(tp, now).toString();

        StringBuilder sb = new StringBuilder();
        switch (preset) {
            case 1:
                sb.append(DateFormat.format("dd/MM", now)).append(' ').append(time);
                break;
            case 2:
                sb.append(DateFormat.format("dd", now)).append(". ")
                  .append(WEEKDAYS[now.get(Calendar.DAY_OF_WEEK) - 1]).append(' ').append(time);
                break;
            default:
                sb.append(WEEKDAYS[now.get(Calendar.DAY_OF_WEEK) - 1]).append(' ').append(time);
        }
        return sb.toString();
    }

    private int dp(int v) {
        return Ui.dp(app, v);
    }
}
