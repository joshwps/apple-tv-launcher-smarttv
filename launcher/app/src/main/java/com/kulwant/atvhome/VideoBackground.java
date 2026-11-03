package com.kulwant.atvhome;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.File;

final class VideoBackground implements SurfaceHolder.Callback {

    private static final String TAG = "VideoBackground";

    private final SurfaceView view;
    private final File file;
    private final String url;
    private MediaPlayer player;
    private boolean wantPlay;

    VideoBackground(Context c, File file) {
        this.file = file;
        this.url = null;
        view = new SurfaceView(c);
        view.getHolder().addCallback(this);
    }

    VideoBackground(Context c, String url) {
        this.file = null;
        this.url = url;
        view = new SurfaceView(c);
        view.getHolder().addCallback(this);
    }

    View view() { return view; }

    void resume() {
        wantPlay = true;
        if (view.getHolder().getSurface() != null) start();
    }

    void pause() {
        wantPlay = false;
        release();
    }

    private void start() {
        release();
        if (!wantPlay) return;
        if (file == null && (url == null || url.isEmpty())) return;

        try {
            player = new MediaPlayer();
            player.setDisplay(view.getHolder());
            if (url != null && !url.isEmpty()) {
                Log.i(TAG, "Opening URL: " + url);
                player.setDataSource(url);
            } else {
                Log.i(TAG, "Opening file: " + file.getPath());
                player.setDataSource(file.getPath());
            }
            player.setLooping(true);
            player.setVolume(0f, 0f);
            player.setOnPreparedListener(m -> {
                Log.i(TAG, "Prepared. Starting playback.");
                try {
                    m.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                    m.start();
                } catch (Exception e) {
                    Log.e(TAG, "start() failed", e);
                }
            });
            player.setOnErrorListener((m, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + " extra=" + extra
                        + " url=" + url + " file=" + (file != null ? file.getPath() : "null"));
                release();
                return true;
            });
            player.setOnInfoListener((m, what, extra) -> {
                Log.i(TAG, "Info: what=" + what + " extra=" + extra);
                return false;
            });
            player.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "setDataSource failed for url=" + url + " file="
                    + (file != null ? file.getPath() : "null"), e);
            release();
        }
    }

    private void release() {
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            try { player.reset(); } catch (Exception ignored) {}
            player.release();
            player = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "Surface created");
        if (wantPlay) start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "Surface changed: " + width + "x" + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "Surface destroyed");
        release();
    }
}
