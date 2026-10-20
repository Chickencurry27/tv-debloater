package com.tvhome.launcher;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends Activity {
    // Dock order. Edit and rebuild (launcher/build-and-install.sh) to change.
    private static final DockView.Entry[] DOCK = {
        new DockView.Entry("com.google.android.youtube.tv", "YouTube"),
        new DockView.Entry("com.netflix.ninja", "Netflix"),
        new DockView.Entry("com.amazon.amazonvideo.livingroom", "Prime Video"),
        new DockView.Entry("com.formulaone.production", "F1 TV"),
        new DockView.Entry("com.wbd.hbomax", "HBO Max"),
        new DockView.Entry("com.apple.atve.androidtv.appletv", "Apple TV"),
        new DockView.Entry("org.jellyfin.androidtv", "Jellyfin"),
        new DockView.Entry("com.skyshowtime.skyshowtime.google", "SkyShowtime"),
        new DockView.Entry("nl.uitzendinggemist", "NPO Start"),
        new DockView.Entry("nl.rtl.videoland.v2", "Videoland"),
    };

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("EEEE d MMMM", Locale.ENGLISH);
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.ENGLISH);

    private TextView greeting, date, clock;
    private DockView dock;
    private InputsRow inputs;
    private WallpaperRotator wallpaper;
    private int lastDockIndex = 0;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        final float d = getResources().getDisplayMetrics().density;

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF101418);
        ImageView back = fill(), front = fill();
        root.addView(front);
        root.addView(back);
        back.setAlpha(0f);
        wallpaper = new WallpaperRotator(this, front, back);

        View scrim = new View(this); // keeps white text readable on bright photos
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x88000000, 0x00000000, 0x00000000, 0xAA000000});
        scrim.setBackground(g);
        root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        greeting = text(28, true);
        date = text(16, false);
        top.addView(greeting);
        top.addView(date);
        FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        tl.setMargins((int) (48 * d), (int) (40 * d), 0, 0);
        root.addView(top, tl);

        clock = text(36, false);
        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        cl.setMargins(0, (int) (36 * d), (int) (48 * d), 0);
        root.addView(clock, cl);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setGravity(Gravity.CENTER_HORIZONTAL);
        bottom.setClipChildren(false);
        inputs = new InputsRow(this);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(-2, -2);
        ilp.setMargins(0, 0, 0, (int) (14 * d));
        bottom.addView(inputs, ilp);

        List<DockView.Entry> entries = new ArrayList<>();
        for (DockView.Entry e : DOCK) entries.add(e);
        dock = new DockView(this, entries);
        bottom.addView(dock, new LinearLayout.LayoutParams(-2, -2));

        FrameLayout.LayoutParams bl = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        bl.setMargins(0, 0, 0, (int) (36 * d));
        root.addView(bottom, bl);
        setContentView(root);
    }

    @Override protected void onResume() {
        super.onResume();
        // re-scan every time we come back so newly installed or removed apps show up
        View focus = dock.refresh(lastDockIndex);
        inputs.refresh();
        focus.requestFocus();
        wallpaper.start();
        tickClock.run();
    }

    @Override protected void onPause() {
        View f = getCurrentFocus();
        int i = dock.indexOf(f);
        if (i >= 0) lastDockIndex = i;
        wallpaper.stop();
        ui.removeCallbacks(tickClock);
        super.onPause();
    }

    @Override public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_MENU || keyCode == android.view.KeyEvent.KEYCODE_SETTINGS) {
            try {
                startActivity(new android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception ignored) { }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override public void onBackPressed() {
        // this is the home screen: Back does nothing
    }

    private final Runnable tickClock = new Runnable() {
        @Override public void run() {
            Date now = new Date();
            clock.setText(timeFmt.format(now));
            date.setText(dateFmt.format(now));
            greeting.setText(greetingFor(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)));
            ui.postDelayed(this, 60000 - (now.getTime() % 60000) + 50);
        }
    };

    private static String greetingFor(int hour) {
        if (hour < 5) return "Good night";
        if (hour < 12) return "Good morning";
        if (hour < 18) return "Good afternoon";
        return "Good evening";
    }

    private ImageView fill() {
        ImageView v = new ImageView(this);
        v.setScaleType(ImageView.ScaleType.CENTER_CROP);
        v.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        return v;
    }

    private TextView text(int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(Color.WHITE);
        t.setShadowLayer(6f, 0f, 1f, 0x99000000);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        else t.setTypeface(android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL));
        return t;
    }
}
