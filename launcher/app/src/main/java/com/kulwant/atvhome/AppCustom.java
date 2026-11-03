package com.kulwant.atvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Per-app overrides: a custom name and a custom banner image (set from the TV menu or the phone page). */
final class AppCustom {

    /** Last time something changed; the phone server uses it to refresh its cached app list. */
    static volatile long changedAt;

    private AppCustom() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("custom", Context.MODE_PRIVATE);
    }

    private static boolean valid(String pkg) {
        return pkg != null && pkg.length() <= 200 && pkg.matches("[A-Za-z0-9._: -]+");
    }

    static File iconFile(Context c, String pkg) {
        if (!valid(pkg)) return null;
        File f = new File(new File(c.getFilesDir(), "icons"), pkg + ".png");
        return f.isFile() ? f : null;
    }

    static String label(Context c, String pkg, String def) {
        String s = p(c).getString("label_" + pkg, "");
        return s.isEmpty() ? def : s;
    }

    static boolean hasCustom(Context c, String pkg) {
        return !p(c).getString("label_" + pkg, "").isEmpty() || iconFile(c, pkg) != null;
    }

    static boolean setLabel(Context c, String pkg, String label) {
        if (!valid(pkg)) return false;
        String s = label == null ? "" : label.trim();
        if (s.length() > 40) s = s.substring(0, 40);
        SharedPreferences.Editor e = p(c).edit();
        if (s.isEmpty()) e.remove("label_" + pkg);
        else e.putString("label_" + pkg, s);
        e.apply();
        changed();
        return true;
    }

    /** Saves an image (any format Android can decode) as the banner of an app; false if it is not an image. */
    static boolean setIcon(Context c, String pkg, byte[] img) throws IOException {
        if (!valid(pkg) || img == null || img.length == 0 || img.length > 4 * 1024 * 1024) return false;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(img, 0, img.length, o);
        if (o.outWidth <= 0 || o.outHeight <= 0) return false;
        File dir = new File(c.getFilesDir(), "icons");
        if (!dir.isDirectory()) dir.mkdirs();
        File tmp = new File(dir, pkg + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(img);
        }
        if (!tmp.renameTo(new File(dir, pkg + ".png"))) {
            tmp.delete();
            return false;
        }
        changed();
        return true;
    }

    /** Uses a picture from the TV's storage as the banner of an app (cropped to 16:9). Slow: call off the main thread. */
    static boolean setIconFromFile(Context c, String pkg, File src) {
        if (!valid(pkg)) return false;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(src.getPath(), o);
        if (o.outWidth <= 0 || o.outHeight <= 0) return false;
        int sample = 1;
        while (o.outWidth / (sample * 2) >= 640 && o.outHeight / (sample * 2) >= 360) sample *= 2;
        o.inJustDecodeBounds = false;
        o.inSampleSize = sample;
        Bitmap bmp = BitmapFactory.decodeFile(src.getPath(), o);
        if (bmp == null) return false;
        Bitmap out = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888);
        float s = Math.max(640f / bmp.getWidth(), 360f / bmp.getHeight());
        float dw = bmp.getWidth() * s, dh = bmp.getHeight() * s;
        new Canvas(out).drawBitmap(bmp, null, new RectF((640 - dw) / 2f, (360 - dh) / 2f, (640 + dw) / 2f, (360 + dh) / 2f),
                new Paint(Paint.FILTER_BITMAP_FLAG));
        bmp.recycle();
        File dir = new File(c.getFilesDir(), "icons");
        if (!dir.isDirectory()) dir.mkdirs();
        File tmp = new File(dir, pkg + ".tmp");
        boolean ok;
        try (FileOutputStream fo = new FileOutputStream(tmp)) {
            ok = out.compress(Bitmap.CompressFormat.PNG, 100, fo);
        } catch (IOException e) {
            ok = false;
        }
        out.recycle();
        if (!ok || !tmp.renameTo(new File(dir, pkg + ".png"))) {
            tmp.delete();
            return false;
        }
        changed();
        return true;
    }

    static void reset(Context c, String pkg) {
        if (!valid(pkg)) return;
        p(c).edit().remove("label_" + pkg).apply();
        File f = iconFile(c, pkg);
        if (f != null) f.delete();
        changed();
    }

    /** The custom banner, cropped to w x h, or null when the app has none. */
    static Bitmap icon(Context c, String pkg, int w, int h) {
        File f = iconFile(c, pkg);
        if (f == null) return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), o);
        if (o.outWidth <= 0 || o.outHeight <= 0) return null;
        int sample = 1;
        while (o.outWidth / (sample * 2) >= w && o.outHeight / (sample * 2) >= h) sample *= 2;
        o.inJustDecodeBounds = false;
        o.inSampleSize = sample;
        Bitmap src = BitmapFactory.decodeFile(f.getPath(), o);
        if (src == null) return null;
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(out);
        cv.drawColor(0xFF1E1E21);
        float s = Math.max((float) w / src.getWidth(), (float) h / src.getHeight());
        float dw = src.getWidth() * s, dh = src.getHeight() * s;
        cv.drawBitmap(src, null, new RectF((w - dw) / 2f, (h - dh) / 2f, (w + dw) / 2f, (h + dh) / 2f),
                new Paint(Paint.FILTER_BITMAP_FLAG));
        src.recycle();
        return out;
    }

    private static void changed() {
        changedAt = System.currentTimeMillis();
        Wallpapers.notifyAppsChanged();
    }
}
