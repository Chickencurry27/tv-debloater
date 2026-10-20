package com.tvhome.launcher;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Centered row of HDMI pills, built from the TV's own input list. */
final class InputsRow extends LinearLayout {
    // Custom names and icons, keyed by the TV's own label for the port. Edit and rebuild to change.
    private static final java.util.Map<String, String[]> CUSTOM = new java.util.HashMap<>();
    static {
        CUSTOM.put("HDMI 1", new String[]{"Xbox", "xbox"});
        CUSTOM.put("HDMI 2", new String[]{"TV", "tv"});
        CUSTOM.put("HDMI 3", new String[]{"SNES", "snes"});
    }

    InputsRow(Context ctx) {
        super(ctx);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        setClipChildren(false);
    }

    /** Rebuild the pills; returns how many there are. */
    int refresh() {
        removeAllViews();
        final Context ctx = getContext();
        final TvInputManager tim = (TvInputManager) ctx.getSystemService(Context.TV_INPUT_SERVICE);
        List<TvInputInfo> inputs = new ArrayList<>();
        if (tim != null) {
            for (TvInputInfo i : tim.getTvInputList()) {
                // hardware HDMI ports only (id ends in /HW<n>); CEC child devices end in /HDMI<n>
                if (i.getType() == TvInputInfo.TYPE_HDMI && i.isPassthroughInput() && i.getId().matches(".*/HW\\d+")) {
                    inputs.add(i);
                }
            }
        }
        Collections.sort(inputs, new Comparator<TvInputInfo>() {
            @Override public int compare(TvInputInfo a, TvInputInfo b) { return a.getId().compareTo(b.getId()); }
        });
        for (final TvInputInfo info : inputs) {
            CharSequence custom = info.loadCustomLabel(ctx);
            CharSequence label = custom != null && custom.length() > 0 ? custom : info.loadLabel(ctx);
            final int state = tim.getInputState(info.getId());
            String[] cfg = CUSTOM.get(label.toString());
            addView(pill(cfg != null ? cfg[0] : label.toString(), cfg != null ? cfg[1] : null,
                    state == TvInputManager.INPUT_STATE_DISCONNECTED, new OnClickListener() {
                @Override public void onClick(View v) {
                    Intent i = new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(info.getId()));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try { ctx.startActivity(i); } catch (Exception ignored) { }
                }
            }));
        }
        // keep focus inside the row at both ends instead of jumping to the dock
        int n = getChildCount();
        if (n > 0) {
            View first = getChildAt(0), last = getChildAt(n - 1);
            first.setNextFocusLeftId(first.getId());
            last.setNextFocusRightId(last.getId());
            for (int c = 0; c < n; c++) getChildAt(c).setNextFocusUpId(getChildAt(c).getId());
        }
        return inputs.size();
    }

    private TextView pill(final String label, String iconKey, boolean dim, OnClickListener click) {
        final Context ctx = getContext();
        final float d = ctx.getResources().getDisplayMetrics().density;
        final TextView t = new TextView(ctx);
        t.setText(label);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        t.setTextColor(Color.WHITE);
        t.setGravity(Gravity.CENTER);
        t.setPadding((int) (iconKey != null ? 16 * d : 22 * d), (int) (14 * d), (int) (22 * d), (int) (14 * d));
        if (iconKey != null) {
            int sz = (int) (24 * d);
            android.graphics.drawable.Drawable ic = InputIcons.create(iconKey);
            ic.setBounds(0, 0, sz, sz);
            t.setCompoundDrawables(ic, null, null, null);
            t.setCompoundDrawablePadding((int) (10 * d));
        }
        t.setId(View.generateViewId());
        t.setFocusable(true);
        t.setClickable(true);
        t.setOnClickListener(click);
        t.setAlpha(dim ? 0.6f : 1f);
        t.setBackground(shape(0x80000000));
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.setMargins((int) (8 * d), 0, (int) (8 * d), 0);
        t.setLayoutParams(lp);
        t.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) {
                t.setBackground(shape(has ? 0xFFFFFFFF : 0x80000000));
                t.setTextColor(has ? 0xFF111111 : Color.WHITE);
            }
        });
        return t;
    }

    private static GradientDrawable shape(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(1000f);
        return g;
    }
}
