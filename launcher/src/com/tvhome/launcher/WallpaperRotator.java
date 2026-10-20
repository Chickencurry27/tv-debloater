package com.tvhome.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Crossfades through the bundled nature photos on a timer. Only runs while the home screen is visible. */
final class WallpaperRotator {
    static final long INTERVAL_MS = 10 * 60 * 1000L;
    private static final long FADE_MS = 1500;

    private final Context ctx;
    private final ImageView front, back;
    private final SharedPreferences prefs;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final List<String> files = new ArrayList<>();
    private boolean first = true;

    WallpaperRotator(Context ctx, ImageView front, ImageView back) {
        this.ctx = ctx;
        this.front = front;
        this.back = back;
        this.prefs = ctx.getSharedPreferences("wallpaper", Context.MODE_PRIVATE);
        try {
            String[] names = ctx.getAssets().list("wallpapers");
            if (names != null) {
                files.addAll(Arrays.asList(names));
                Collections.sort(files);
                // fixed shuffle per install so the order is not alphabetical but stable across restarts
                Collections.shuffle(files, new Random(prefs.getLong("seed", seed())));
            }
        } catch (Exception ignored) {
        }
    }

    private long seed() {
        long s = System.currentTimeMillis();
        prefs.edit().putLong("seed", s).apply();
        return s;
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            advance(System.currentTimeMillis());
            ui.postDelayed(this, INTERVAL_MS);
        }
    };

    /** Show the current photo (advancing if the interval has passed) and schedule the next change. */
    void start() {
        if (files.isEmpty()) return;
        long now = System.currentTimeMillis();
        long last = prefs.getLong("t", 0);
        ui.removeCallbacks(tick);
        if (last == 0 || now - last >= INTERVAL_MS || now < last) {
            advance(now);
            ui.postDelayed(tick, INTERVAL_MS);
        } else {
            show(prefs.getInt("i", 0) % files.size());
            ui.postDelayed(tick, INTERVAL_MS - (now - last));
        }
    }

    void stop() {
        ui.removeCallbacks(tick);
    }

    private void advance(long now) {
        int i = (prefs.getInt("i", -1) + 1) % files.size();
        prefs.edit().putInt("i", i).putLong("t", now).apply();
        show(i);
    }

    private void show(final int index) {
        final String name = files.get(index);
        new Thread(() -> {
            final Bitmap bmp = decode(name);
            if (bmp == null) return;
            ui.post(() -> display(bmp));
        }).start();
    }

    private Bitmap decode(String name) {
        try (InputStream in = ctx.getAssets().open("wallpapers/" + name)) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inPreferredConfig = Bitmap.Config.RGB_565; // half the memory of ARGB_8888
            return BitmapFactory.decodeStream(in, null, o);
        } catch (Exception e) {
            return null;
        }
    }

    private void display(final Bitmap bmp) {
        if (first) {
            first = false;
            front.setImageBitmap(bmp);
            return;
        }
        back.animate().cancel();
        back.setImageBitmap(bmp);
        back.setAlpha(0f);
        back.animate().alpha(1f).setDuration(FADE_MS).withEndAction(() -> {
            front.setImageBitmap(bmp);
            back.setAlpha(0f);
            back.setImageDrawable(null);
        }).start();
    }
}
