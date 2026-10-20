package com.tvhome.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** The favourites dock: configured apps, a divider, then an Apps button that opens the Play Store. */
final class DockView extends LinearLayout {
    static final class Entry {
        final String pkg, label;
        Entry(String pkg, String label) { this.pkg = pkg; this.label = label; }
    }

    private static final String PLAY_STORE = "com.android.vending";
    private static final String SETTINGS = "tvhome:settings";

    private final List<Entry> apps;
    private final float d;
    private final List<View> tiles = new ArrayList<>();

    DockView(Context ctx, List<Entry> apps) {
        super(ctx);
        this.apps = apps;
        this.d = ctx.getResources().getDisplayMetrics().density;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);
        setPadding((int) (16 * d), (int) (12 * d), (int) (16 * d), (int) (12 * d));
        GradientDrawable bar = new GradientDrawable();
        bar.setColor(0x40FFFFFF);
        bar.setCornerRadius(20 * d);
        setBackground(bar);
    }

    /** Rebuild tiles from what is installed right now. Returns the tile at index keep (or the first). */
    View refresh(int keep) {
        removeAllViews();
        tiles.clear();
        for (Entry e : apps) addTile(e, false);
        View divider = new View(getContext());
        divider.setBackgroundColor(0x80FFFFFF);
        LayoutParams dlp = new LayoutParams((int) (1.5f * d), (int) (40 * d));
        dlp.setMargins((int) (10 * d), 0, (int) (10 * d), 0);
        addView(divider, dlp);
        addTile(new Entry(PLAY_STORE, "Apps"), true);
        addTile(new Entry(SETTINGS, "Settings"), false);
        // keep focus inside the dock at both ends and at the bottom
        View first = tiles.get(0), last = tiles.get(tiles.size() - 1);
        first.setNextFocusLeftId(first.getId());
        last.setNextFocusRightId(last.getId());
        for (View t : tiles) t.setNextFocusDownId(t.getId());
        return tiles.get(Math.max(0, Math.min(keep, tiles.size() - 1)));
    }

    int indexOf(View v) { return tiles.indexOf(v); }

    private void addTile(final Entry e, boolean isAppsButton) {
        final Context ctx = getContext();
        final PackageManager pm = ctx.getPackageManager();
        int size = (int) (58 * d);

        final FrameLayout tile = new FrameLayout(ctx);
        tile.setId(View.generateViewId());
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setClipChildren(false);
        tile.setBackground(tileBg(false));
        LayoutParams lp = new LayoutParams(size, size);
        lp.setMargins((int) (5 * d), 0, (int) (5 * d), 0);
        tile.setLayoutParams(lp);
        tile.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), 13 * d); }
        });
        tile.setClipToOutline(true);

        final boolean isSettings = SETTINGS.equals(e.pkg);
        Intent launch = isAppsButton ? launchIntent(pm, PLAY_STORE)
                : isSettings ? new Intent(Settings.ACTION_SETTINGS) : launchIntent(pm, e.pkg);
        final boolean installed = launch != null;
        final int baseColor = isAppsButton ? 0x66FFFFFF : 0xCC1B1B1F;
        final Intent intent = launch;

        if (isAppsButton) {
            tile.addView(centerText(ctx, "+", 30, 0xFFFFFFFF));
            tile.setBackground(tileBg(false, 0x66FFFFFF));
        } else if (isSettings) {
            ImageView gear = new ImageView(ctx);
            gear.setImageDrawable(InputIcons.create("gear"));
            int pad = (int) (15 * d);
            gear.setPadding(pad, pad, pad, pad);
            tile.addView(gear, new FrameLayout.LayoutParams(-1, -1));
            tile.setBackground(tileBg(false, 0x66FFFFFF));
        } else if (installed) {
            try {
                Drawable icon = pm.getApplicationIcon(e.pkg);
                ImageView iv = new ImageView(ctx);
                iv.setImageDrawable(icon);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                tile.addView(iv, new FrameLayout.LayoutParams(-1, -1));
            } catch (PackageManager.NameNotFoundException ex) {
                tile.addView(centerText(ctx, e.label.substring(0, 1), 22, 0xFFFFFFFF));
            }
        } else {
            tile.addView(centerText(ctx, e.label.substring(0, 1), 22, 0x99FFFFFF)); // not installed: letter, opens Play listing
        }

        tile.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    if (installed) {
                        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    } else {
                        ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + e.pkg))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    }
                } catch (Exception ignored) { }
            }
        });
        tile.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) {
                v.animate().scaleX(has ? 1.18f : 1f).scaleY(has ? 1.18f : 1f).setDuration(120).start();
                v.setElevation(has ? 12 * d : 0);
                v.setForeground(has ? ring() : null);
            }
        });
        tiles.add(tile);
        addView(tile);
    }

    private static Intent launchIntent(PackageManager pm, String pkg) {
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        return i != null ? i : pm.getLaunchIntentForPackage(pkg);
    }

    private TextView centerText(Context ctx, String s, int sp, int color) {
        TextView t = new TextView(ctx);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        return t;
    }

    private GradientDrawable ring() {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.TRANSPARENT);
        g.setCornerRadius(13 * d);
        g.setStroke((int) (3 * d), Color.WHITE);
        return g;
    }

    private GradientDrawable tileBg(boolean focused) { return tileBg(focused, 0xCC1B1B1F); }

    private GradientDrawable tileBg(boolean focused, int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(13 * d);
        if (focused) g.setStroke((int) (2.5f * d), Color.WHITE);
        return g;
    }
}
