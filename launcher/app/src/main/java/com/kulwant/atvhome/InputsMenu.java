package com.kulwant.atvhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Compact glass dropdown listing HDMI ports, AV and Live TV; switches via the TV input framework. */
final class InputsMenu {

    private static final String TAG = "InputsMenu";

    // The MediaTek TV Center activity that renders the live TV surface directly.
    private static final String MEDIATEK_PKG = "com.mediatek.wwtv.tvcenter";
    private static final String MEDIATEK_LIVE_ACTIVITY =
            "com.mediatek.wwtv.tvcenter.nav.TurnkeyUiMainActivity";
    // Fallback activity name seen on some MediaTek variants.
    private static final String MEDIATEK_LIVE_ACTIVITY_ALT =
            "com.mitv.livetv.nav.TurnkeyUiMainActivity";

    static final class Item {
        String label;
        boolean connected;
        Intent intent;
        String source;
    }

    final FrameLayout panel;
    private final LinearLayout list;
    private final Context c;
    private final List<Item> items = new ArrayList<>();
    private final List<TextView> rows = new ArrayList<>();
    private int sel;

    InputsMenu(Context c) {
        this.c = c;
        panel = new FrameLayout(c);
        panel.setBackground(Ui.rounded(0xEB1C1C1E, Ui.dp(c, 16), Ui.dp(c, 1), 0x24FFFFFF));
        panel.setPadding(Ui.dp(c, 6), Ui.dp(c, 6), Ui.dp(c, 6), Ui.dp(c, 6));
        panel.setElevation(Ui.dp(c, 12));
        panel.setVisibility(View.GONE);
        list = new LinearLayout(c);
        list.setOrientation(LinearLayout.VERTICAL);
        panel.addView(list);
    }

    boolean isOpen() { return panel.getVisibility() == View.VISIBLE; }

    void open(int leftPx, int topPx) {
        load();
        list.removeAllViews();
        rows.clear();
        for (Item it : items) {
            TextView t = Ui.text(c, 400, 15, it.connected ? 0xFFFFFFFF : 0x80FFFFFF);
            t.setText(it.label);
            t.setGravity(Gravity.CENTER_VERTICAL);
            t.setPadding(Ui.dp(c, 14), 0, Ui.dp(c, 14), 0);
            list.addView(t, new LinearLayout.LayoutParams(Ui.dp(c, 188), Ui.dp(c, 38)));
            rows.add(t);
        }
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = leftPx;
        lp.topMargin = topPx;
        panel.setLayoutParams(lp);
        sel = 0;
        paint();
        panel.setVisibility(View.VISIBLE);
        panel.setAlpha(0f);
        panel.setPivotY(0);
        panel.setScaleY(0.94f);
        panel.animate().alpha(1f).scaleY(1f).setDuration(160).start();
    }

    void close() {
        panel.animate().alpha(0f).setDuration(110).withEndAction(() -> panel.setVisibility(View.GONE)).start();
    }

    void move(int d) {
        int n = sel + d;
        if (n < 0 || n >= items.size()) return;
        sel = n;
        paint();
    }

    Item current() { return items.isEmpty() ? null : items.get(sel); }

    Intent selected() {
        Item it = current();
        if (it == null) return null;
        Log.d(TAG, "selected label=" + it.label + " source=" + it.source);
        return it.intent;
    }

    private void paint() {
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setBackground(i == sel ? Ui.rounded(0x2EFFFFFF, Ui.dp(c, 11), Ui.dp(c, 1), 0x40FFFFFF) : null);
        }
    }

    private void load() {
        items.clear();
        items.addAll(list(c));
        for (Item it : items) {
            Log.d(TAG, "item=" + it.label + " source=" + it.source + " intent=" + it.intent);
        }
    }

    static List<Item> list(Context c) {
        List<Item> items = new ArrayList<>();
        TvInputManager tim = (TvInputManager) c.getSystemService(Context.TV_INPUT_SERVICE);
        if (tim == null) return items;
        List<Item> hdmi = new ArrayList<>(), av = new ArrayList<>();
        TvInputInfo tuner = null;
        for (TvInputInfo info : tim.getTvInputList()) {
            if (info.isHidden(c)) continue;
            int type = info.getType();
            if (type == TvInputInfo.TYPE_TUNER) {
                if (tuner == null) tuner = info;
                continue;
            }
            if (!info.isPassthroughInput()) continue;
            Item it = new Item();
            CharSequence custom = info.loadCustomLabel(c);
            it.label = String.valueOf(custom != null ? custom : info.loadLabel(c));
            it.connected = tim.getInputState(info.getId()) != TvInputManager.INPUT_STATE_DISCONNECTED;
            it.intent = new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(info.getId()))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            it.source = "passthrough";
            if (type == TvInputInfo.TYPE_HDMI) hdmi.add(it); else av.add(it);
        }
        Collections.sort(hdmi, (a, b) -> a.label.compareTo(b.label));
        items.addAll(hdmi);
        items.addAll(av);
        if (tuner != null) {
            Item live = buildLive(c, tuner);
            if (live != null) items.add(live);
        }
        return items;
    }

    private static Item buildLive(Context c, TvInputInfo tuner) {
        Intent i = liveTvIntent(c, tuner);
        if (i == null) {
            Log.w(TAG, "no Live TV intent");
            return null;
        }
        Item it = new Item();
        it.label = "TV ao vivo";
        it.connected = true;
        it.intent = i;
        it.source = liveSource;
        return it;
    }

    private static String liveSource = "?";

    private static Intent liveTvIntent(Context c, TvInputInfo tuner) {
        // URI genérica de canais — testada com force-stop + am start na TV MediaTek.
        // buildChannelsUriForInput(tuner.getId()) NÃO funciona (app ignora e restaura HDMI).
        // content://android.media.tv/channel funciona.
        Intent i = new Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("content://android.media.tv/channel"));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Log.e("InputsMenu", "liveTvIntent: " + i + " (tuner.id=" + tuner.getId() + ")");
        return i;
    }

    private static String tunerPackage(TvInputInfo tuner) {
        try {
            if (tuner.getServiceInfo() != null) return tuner.getServiceInfo().packageName;
        } catch (Exception ignored) {
        }
        String id = tuner.getId();
        int slash = id.indexOf('/');
        return slash > 0 ? id.substring(0, slash) : id;
    }
}
