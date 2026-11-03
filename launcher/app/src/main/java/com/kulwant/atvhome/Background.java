package com.kulwant.atvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;

/**
 * What is behind the home screen: the rotating photos (see Wallpapers), one colour, a gradient,
 * a looping local video or a looping online video (VIDEO_URL). Chosen from the TV menu or the
 * phone page; HomeActivity rebuilds itself when it changes.
 */
final class Background {

    static final String PHOTOS = "photos", COLOR = "color", GRADIENT = "gradient",
            VIDEO = "video", IMAGE = "image", VIDEO_URL = "video_url";

    private static final long MAX_VIDEO_BYTES = 100L * 1024 * 1024;
    private static final String[] VIDEO_EXT = {"mp4", "mkv", "webm", "mov", "m4v", "3gp"};

    private Background() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("bg", Context.MODE_PRIVATE);
    }

    /** Local background video (uploaded from the phone page or imported). */
    static File videoFile(Context c) {
        File f = new File(new File(c.getFilesDir(), "bg"), "background.mp4");
        return f.isFile() && f.length() > 0 ? f : null;
    }

    /** Local background image (imported from the storage picker). */
    static File imageFile(Context c) {
        File f = new File(new File(c.getFilesDir(), "bg"), "background.jpg");
        return f.isFile() && f.length() > 0 ? f : null;
    }

    /** URL of the online background video (VIDEO_URL mode). */
    static String videoUrl(Context c) {
        String u = p(c).getString("video_url", "");
        return u.isEmpty() ? null : u;
    }

    static String mode(Context c) {
        String m = p(c).getString("mode", PHOTOS);
        if (VIDEO.equals(m)) return videoFile(c) == null ? PHOTOS : VIDEO;
        if (VIDEO_URL.equals(m)) return videoUrl(c) == null ? PHOTOS : VIDEO_URL;
        if (IMAGE.equals(m)) return imageFile(c) == null ? PHOTOS : IMAGE;
        if (COLOR.equals(m) || GRADIENT.equals(m)) return m;
        return PHOTOS;
    }

    /** The dark overlay over photos, pictures and video is fixed (100 = as designed). */
    static int dim(Context c) {
        return 100;
    }

    /** 0 top to bottom, 1 left to right, 2 top-left to bottom-right, 3 bottom-left to top-right. */
    static int dir(Context c) {
        return Math.max(0, Math.min(3, p(c).getInt("dir", 0)));
    }

    private static String hex(Context c, String key, String def) {
        String s = p(c).getString(key, "");
        return s.matches("#[0-9a-fA-F]{6}") ? s : def;
    }

    static GradientDrawable drawable(Context c) {
        if (COLOR.equals(mode(c))) {
            GradientDrawable g = new GradientDrawable();
            g.setColor(Color.parseColor(hex(c, "c1", "#0f2027")));
            return g;
        }
        GradientDrawable.Orientation[] o = {
                GradientDrawable.Orientation.TOP_BOTTOM, GradientDrawable.Orientation.LEFT_RIGHT,
                GradientDrawable.Orientation.TL_BR, GradientDrawable.Orientation.BL_TR};
        int c1 = Color.parseColor(hex(c, "c1", "#0f2027"));
        int c2 = Color.parseColor(hex(c, "c2", "#2c5364"));
        String s3 = hex(c, "c3", "");
        int[] cols = s3.isEmpty() ? new int[]{c1, c2} : new int[]{c1, c2, Color.parseColor(s3)};
        return new GradientDrawable(o[dir(c)], cols);
    }

    /** Saves the values present in v (mode, c1, c2, c3, dir, video_url); returns an error or null. */
    static String apply(Context c, Map<String, String> v) {
        SharedPreferences.Editor e = p(c).edit();
        String mode = v.get("mode");
        if (mode != null) {
            if (!PHOTOS.equals(mode) && !COLOR.equals(mode) && !GRADIENT.equals(mode)
                    && !VIDEO.equals(mode) && !IMAGE.equals(mode) && !VIDEO_URL.equals(mode)) {
                return "modo inv\u00e1lido";
            }
            if (VIDEO_URL.equals(mode)) {
                String u = v.get("video_url");
                if (u == null || u.trim().isEmpty()) return "URL do v\u00eddeo vazia";
                if (!u.startsWith("http://") && !u.startsWith("https://")) {
                    return "URL deve come\u00e7ar com http:// ou https://";
                }
                e.putString("video_url", u.trim());
            }
            if (IMAGE.equals(mode) && imageFile(c) == null) return "escolha uma imagem primeiro";
            if (VIDEO.equals(mode) && videoFile(c) == null) return "envie um v\u00eddeo primeiro";
            e.putString("mode", mode);
        }
        for (String k : new String[]{"c1", "c2", "c3"}) {
            String s = v.get(k);
            if (s == null) continue;
            if (!s.isEmpty() && !s.matches("#[0-9a-fA-F]{6}")) return "cor inv\u00e1lida";
            e.putString(k, s);
        }
        String d = v.get("dir");
        if (d != null) {
            int n = num(d);
            if (n < 0 || n > 3) return "dire\u00e7\u00e3o inv\u00e1lida";
            e.putInt("dir", n);
        }
        e.apply();
        Wallpapers.notifyBackgroundChanged();
        return null;
    }

    private static int num(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    /**
     * Uses a picture or a video from the TV's storage as the background. Returns an error or null.
     * Slow (copies and decodes the file): call it off the main thread.
     */
    static String importFile(Context c, File src) {
        File dir = new File(c.getFilesDir(), "bg");
        if (!dir.isDirectory()) dir.mkdirs();
        String n = src.getName().toLowerCase(Locale.US);
        boolean video = false;
        for (String e : VIDEO_EXT) if (n.endsWith("." + e)) video = true;
        return video ? importVideo(c, src, dir) : importImage(c, src, dir);
    }

    private static String importImage(Context c, File src, File dir) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(src.getPath(), o);
        if (o.outWidth <= 0 || o.outHeight <= 0) return "n\u00e3o parece ser uma imagem";
        int sample = 1;
        while (o.outWidth / (sample * 2) >= 1920 && o.outHeight / (sample * 2) >= 1080) sample *= 2;
        o.inJustDecodeBounds = false;
        o.inSampleSize = sample;
        Bitmap bmp = BitmapFactory.decodeFile(src.getPath(), o);
        if (bmp == null) return "n\u00e3o consegui abrir a imagem";
        File tmp = new File(dir, "image.tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
        } catch (IOException e) {
            tmp.delete();
            return "n\u00e3o consegui salvar a imagem";
        } finally {
            bmp.recycle();
        }
        if (!tmp.renameTo(new File(dir, "background.jpg"))) {
            tmp.delete();
            return "n\u00e3o consegui salvar a imagem";
        }
        return apply(c, java.util.Collections.singletonMap("mode", IMAGE));
    }

    private static String importVideo(Context c, File src, File dir) {
        if (src.length() > MAX_VIDEO_BYTES) return "v\u00eddeo grande demais (m\u00e1ximo 100 MB)";
        File tmp = new File(dir, "upload.tmp");
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(tmp)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } catch (IOException e) {
            tmp.delete();
            return "n\u00e3o consegui copiar o v\u00eddeo";
        }
        boolean ok = false;
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(tmp.getPath());
            ok = "yes".equals(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO));
        } catch (Exception e) {
            ok = false;
        } finally {
            try { mmr.release(); } catch (Exception ignored) {}
        }
        if (!ok) {
            tmp.delete();
            return "n\u00e3o parece ser um v\u00eddeo";
        }
        if (!tmp.renameTo(new File(dir, "background.mp4"))) {
            tmp.delete();
            return "n\u00e3o consegui salvar o v\u00eddeo";
        }
        return apply(c, java.util.Collections.singletonMap("mode", VIDEO));
    }

    static void deleteVideo(Context c) {
        File f = videoFile(c);
        if (f != null) f.delete();
        if (VIDEO.equals(p(c).getString("mode", PHOTOS))) p(c).edit().putString("mode", PHOTOS).apply();
        Wallpapers.notifyBackgroundChanged();
    }

    static JSONObject toJson(Context c) throws Exception {
        File v = videoFile(c);
        String u = videoUrl(c);
        return new JSONObject().put("ok", true)
                .put("mode", mode(c))
                .put("c1", hex(c, "c1", "#0f2027"))
                .put("c2", hex(c, "c2", "#2c5364"))
                .put("c3", hex(c, "c3", ""))
                .put("dir", dir(c))
                .put("dim", dim(c))
                .put("hasVideo", v != null)
                .put("hasImage", imageFile(c) != null)
                .put("hasVideoUrl", u != null)
                .put("videoUrl", u == null ? "" : u)
                .put("videoMb", v == null ? 0 : Math.round(v.length() / 104857.6) / 10.0);
    }
}
