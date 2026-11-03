package com.kulwant.atvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Browses the TV's main storage with the D-pad, showing a grid of thumbnails.
 * Folders are blue tiles, images are previews, videos show the first frame with a play badge.
 * OK opens a folder or picks a file; Back goes up one level (or closes at the root).
 */
final class StoragePicker {

    interface OnPick {
        void picked(File f);
    }

    private static final int COLS = 4;
    private static final String[] IMAGE_EXT = {"jpg", "jpeg", "png", "webp", "bmp", "gif"};
    private static final String[] VIDEO_EXT = {"mp4", "mkv", "webm", "mov", "m4v", "3gp"};

    private static final class Entry {
        final String label;
        final File file;
        final boolean dir;
        final boolean video;
        Entry(String label, File file, boolean dir, boolean video) {
            this.label = label; this.file = file; this.dir = dir; this.video = video;
        }
    }

    final FrameLayout panel;
    private final Context c;
    private final TextView title, breadcrumb, count;
    private final LinearLayout grid;
    private final ScrollView scroll;
    private final List<Entry> entries = new ArrayList<>();
    private final List<View> cells = new ArrayList<>();
    private final List<View> borders = new ArrayList<>();
    private final List<ImageView> thumbs = new ArrayList<>();
    private File root, dir;
    private boolean withVideo;
    private OnPick cb;
    private int sel;

    StoragePicker(Context c) {
        this.c = c;
        panel = new FrameLayout(c);
        panel.setBackgroundColor(0xF2000000);
        panel.setVisibility(View.GONE);
        panel.setFocusable(true);
        panel.setFocusableInTouchMode(true);

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(c, 48), Ui.dp(c, 40), Ui.dp(c, 48), Ui.dp(c, 48));

        title = Ui.text(c, 600, 22, 0xFFFFFFFF);
        title.setText("Escolher arquivo");
        col.addView(title);

        breadcrumb = Ui.text(c, 500, 14, 0x99FFFFFF);
        breadcrumb.setSingleLine(true);
        breadcrumb.setEllipsize(TextUtils.TruncateAt.START);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = Ui.dp(c, 6);
        col.addView(breadcrumb, blp);

        count = Ui.text(c, 400, 13, 0x80FFFFFF);
        count.setSingleLine(true);
        count.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.topMargin = Ui.dp(c, 2);
        col.addView(count, clp);

        grid = new LinearLayout(c);
        grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(-1, -2);
        glp.topMargin = Ui.dp(c, 20);
        col.addView(grid, glp);

        scroll = new ScrollView(c);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(col);
        panel.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
    }

    boolean isOpen() { return panel.getVisibility() == View.VISIBLE; }

    boolean open(String titleText, boolean withVideo, OnPick cb) {
        File r = Wallpapers.USER_DIR;
        if (!r.isDirectory() || r.listFiles() == null) {
            r = Environment.getExternalStorageDirectory();
            if (r == null || r.listFiles() == null) return false;
        }
        root = r;
        dir = r;
        this.withVideo = withVideo;
        this.cb = cb;
        title.setText(titleText);
        sel = 0;
        load();
        panel.setVisibility(View.VISIBLE);
        return true;
    }

    void close() { panel.setVisibility(View.GONE); }

    /** Routes a key event while the picker is open. Returns true if it handled the event. */
    boolean dispatchKey(KeyEvent e) {
        if (!isOpen()) return false;
        if (e.getAction() != KeyEvent.ACTION_DOWN) return true;
        switch (e.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT:  move(-1, 0); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: move( 1, 0); return true;
            case KeyEvent.KEYCODE_DPAD_UP:    move( 0,-1); return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:  move( 0, 1); return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                if (e.getRepeatCount() == 0) select();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (e.getRepeatCount() == 0) back();
                return true;
        }
        return true;
    }

    private void move(int dx, int dy) {
        if (entries.isEmpty()) return;
        int col = sel % COLS, row = sel / COLS;
        int nCol = Math.max(0, Math.min(COLS - 1, col + dx));
        int nRow = Math.max(0, row + dy);
        int target = nRow * COLS + nCol;
        if (target >= entries.size()) target = entries.size() - 1;
        if (target < 0) target = 0;
        if (target == sel) return;
        sel = target;
        paint();
        ensureVisible();
    }

    private void select() {
        if (entries.isEmpty()) return;
        Entry e = entries.get(sel);
        if (e.file == null) { up(); }
        else if (e.dir) {
            dir = e.file;
            sel = 0;
            load();
        } else {
            OnPick k = cb;
            close();
            if (k != null) k.picked(e.file);
        }
    }

    private void back() {
        if (dir != null && !dir.equals(root)) up();
        else close();
    }

    private void up() {
        File prev = dir;
        File parent = dir.getParentFile();
        dir = parent == null || !dir.getPath().startsWith(root.getPath()) || dir.equals(root) ? root : parent;
        sel = 0;
        load();
        for (int i = 0; i < entries.size(); i++) {
            if (prev.equals(entries.get(i).file)) {
                sel = i;
                break;
            }
        }
        paint();
        ensureVisible();
    }

    private boolean matches(String name) {
        String n = name.toLowerCase(Locale.US);
        int dot = n.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = n.substring(dot + 1);
        for (String e : IMAGE_EXT) if (e.equals(ext)) return true;
        if (withVideo) for (String e : VIDEO_EXT) if (e.equals(ext)) return true;
        return false;
    }

    private boolean isVideo(String name) {
        String n = name.toLowerCase(Locale.US);
        int dot = n.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = n.substring(dot + 1);
        for (String e : VIDEO_EXT) if (e.equals(ext)) return true;
        return false;
    }

    private void load() {
        entries.clear();
        breadcrumb.setText(dir.getPath());
        File[] list = dir.listFiles();
        List<File> dirs = new ArrayList<>(), files = new ArrayList<>();
        if (list != null) {
            for (File f : list) {
                String n = f.getName();
                if (n.startsWith(".")) continue;
                if (f.isDirectory()) dirs.add(f);
                else if (matches(n)) files.add(f);
            }
        }
        Comparator<File> byName = (a, b) -> a.getName().compareToIgnoreCase(b.getName());
        dirs.sort(byName);
        files.sort(byName);
        if (!dir.equals(root)) entries.add(new Entry("..", null, true, false));
        for (File f : dirs) entries.add(new Entry(f.getName(), f, true, false));
        for (File f : files) entries.add(new Entry(f.getName(), f, false, isVideo(f.getName())));
        int nF = dirs.size(), nI = files.size();
        StringBuilder sb = new StringBuilder();
        if (!dir.equals(root)) sb.append("..  ·  ");
        if (nF > 0) sb.append(nF).append(nF == 1 ? " pasta" : " pastas");
        if (nI > 0) { if (sb.length() > 0) sb.append("  ·  "); sb.append(nI).append(nI == 1 ? " arquivo" : " arquivos"); }
        if (sb.length() == 0) sb.append("Pasta vazia");
        sb.append("   ·   OK abre/escolhe   ·   Voltar sobe");
        count.setText(sb.toString());
        build();
        sel = Math.max(0, Math.min(sel, entries.size() - 1));
        paint();
    }

    private void build() {
        grid.removeAllViews();
        cells.clear();
        borders.clear();
        thumbs.clear();

        if (entries.isEmpty()) {
            TextView empty = Ui.text(c, 400, 15, 0x99FFFFFF);
            empty.setText("Pasta vazia");
            grid.addView(empty);
            return;
        }

        int screenW = c.getResources().getDisplayMetrics().widthPixels;
        int pad = Ui.dp(c, 48);
        int gap = Ui.dp(c, 14);
        int cellW = (screenW - 2 * pad - (COLS - 1) * gap) / COLS;
        int cellH = cellW * 9 / 16;
        final int radius = Ui.dp(c, 14);

        LinearLayout row = null;
        for (int i = 0; i < entries.size(); i++) {
            if (i % COLS == 0) {
                row = new LinearLayout(c);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
                if (i > 0) rlp.topMargin = gap;
                grid.addView(row, rlp);
            }
            final Entry e = entries.get(i);
            FrameLayout cell = new FrameLayout(c);

            ImageView iv = new ImageView(c);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View v, Outline o) {
                    o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
                }
            });
            iv.setClipToOutline(true);
            Bitmap bmp = e.dir
                    ? folderThumb(e.label, cellW, cellH)
                    : mediaThumb(e.file, e.video, cellW, cellH);
            if (bmp != null) iv.setImageBitmap(bmp);
            cell.addView(iv, new FrameLayout.LayoutParams(-1, -1));

            View border = new View(c);
            cell.addView(border, new FrameLayout.LayoutParams(-1, -1));

            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(cellW, cellH);
            if (i % COLS > 0) clp.leftMargin = gap;
            row.addView(cell, clp);
            cells.add(cell);
            borders.add(border);
            thumbs.add(iv);
        }
    }

    private void paint() {
        int radius = Ui.dp(c, 14);
        for (int i = 0; i < borders.size(); i++) {
            boolean on = i == sel;
            if (on) {
                borders.get(i).setBackground(Ui.rounded(0x40FFFFFF, radius, Ui.dp(c, 7), 0xFFFFD60A));
                thumbs.get(i).setAlpha(1f);
            } else {
                borders.get(i).setBackground(Ui.rounded(0x55000000, radius, 0, 0));
                thumbs.get(i).setAlpha(0.85f);
            }
        }
    }

    private void ensureVisible() {
        if (sel < 0 || sel >= cells.size()) return;
        View cell = cells.get(sel);
        int[] cellLoc = new int[2]; cell.getLocationOnScreen(cellLoc);
        int[] svLoc = new int[2]; scroll.getLocationOnScreen(svLoc);
        int cellTop = cellLoc[1] - svLoc[1] + scroll.getScrollY();
        int cellBottom = cellTop + cell.getHeight();
        int visTop = scroll.getScrollY();
        int visBottom = visTop + scroll.getHeight();
        if (cellTop < visTop) scroll.smoothScrollTo(0, cellTop - Ui.dp(c, 20));
        else if (cellBottom > visBottom) scroll.smoothScrollTo(0, cellBottom - scroll.getHeight() + Ui.dp(c, 20));
    }

    private static Bitmap mediaThumb(File f, boolean video, int w, int h) {
        try {
            if (video) return videoThumb(f, w, h);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getPath(), o);
            if (o.outWidth <= 0) return brokenThumb(w, h);
            int sample = 1;
            while (o.outWidth / (sample * 2) >= w && o.outHeight / (sample * 2) >= h) sample *= 2;
            o.inJustDecodeBounds = false;
            o.inSampleSize = sample;
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bmp = BitmapFactory.decodeFile(f.getPath(), o);
            return bmp != null ? bmp : brokenThumb(w, h);
        } catch (Exception e) {
            return brokenThumb(w, h);
        }
    }

    private static Bitmap videoThumb(File f, int w, int h) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(f.getPath());
            Bitmap bmp = mmr.getFrameAtTime(0);
            if (bmp != null) {
                Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas cv = new Canvas(out);
                cv.drawColor(0xFF000000);
                float s = Math.max((float) w / bmp.getWidth(), (float) h / bmp.getHeight());
                float dw = bmp.getWidth() * s, dh = bmp.getHeight() * s;
                cv.drawBitmap(bmp, null, new RectF((w - dw) / 2f, (h - dh) / 2f, (w + dw) / 2f, (h + dh) / 2f), null);
                bmp.recycle();
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setColor(0xCC000000);
                cv.drawCircle(w / 2f, h / 2f, Math.min(w, h) * 0.15f, p);
                p.setColor(0xFFFFFFFF);
                float tri = Math.min(w, h) * 0.10f;
                Path path = new Path();
                path.moveTo(w / 2f - tri * 0.5f, h / 2f - tri);
                path.lineTo(w / 2f - tri * 0.5f, h / 2f + tri);
                path.lineTo(w / 2f + tri, h / 2f);
                path.close();
                cv.drawPath(path, p);
                return out;
            }
        } catch (Exception ignored) {
        } finally {
            try { mmr.release(); } catch (Exception ignored) {}
        }
        return brokenThumb(w, h);
    }

    private static Bitmap brokenThumb(int w, int h) {
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        cv.drawColor(0xFF2C2C2E);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF666666);
        p.setTextSize(h * 0.15f);
        p.setTextAlign(Paint.Align.CENTER);
        cv.drawText("?", w / 2f, h / 2f + h * 0.05f, p);
        return b;
    }

    private static Bitmap folderThumb(String name, int w, int h) {
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        cv.drawColor(0xFF1E3A5F);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF4A9EFF);
        float iconW = w * 0.42f, iconH = iconW * 0.75f;
        float iconX = (w - iconW) / 2f, iconY = h * 0.14f;
        RectF tab = new RectF(iconX, iconY, iconX + iconW * 0.45f, iconY + iconH * 0.28f);
        cv.drawRoundRect(tab, 6, 6, p);
        RectF body = new RectF(iconX, iconY + iconH * 0.2f, iconX + iconW, iconY + iconH);
        cv.drawRoundRect(body, 8, 8, p);
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(0xFFFFFFFF);
        tp.setTextSize(h * 0.10f);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setTypeface(Typeface.DEFAULT_BOLD);
        String display = name;
        if (display.equals("..")) display = "\u2191 subir";
        if (display.length() > 16) display = display.substring(0, 14) + "\u2026";
        cv.drawText(display, w / 2f, h * 0.90f, tp);
        return b;
    }
}
