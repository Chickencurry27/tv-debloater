package com.tvhome.launcher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

/** Small icons for named HDMI inputs, drawn in code so no logo files are bundled. */
final class InputIcons {
    private InputIcons() { }

    static Drawable create(final String key) {
        return new Drawable() {
            private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override public void draw(Canvas c) {
                float w = getBounds().width(), h = getBounds().height();
                c.save();
                c.translate(getBounds().left, getBounds().top);
                switch (key) {
                    case "xbox": xbox(c, w, h); break;
                    case "snes": snes(c, w, h); break;
                    case "tv": tv(c, w, h); break;
                    case "gear": gear(c, w, h); break;
                    default: tile(c, w, h, key.substring(0, 1).toUpperCase(), 0xFF5B3FD0); break;
                }
                c.restore();
            }

            private void xbox(Canvas c, float w, float h) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFF107C10);
                c.drawCircle(w / 2, h / 2, w / 2, p);
                p.setStyle(Paint.Style.STROKE);
                p.setColor(0xFFFFFFFF);
                p.setStrokeWidth(w * 0.13f);
                p.setStrokeCap(Paint.Cap.ROUND);
                float a = w * 0.3f, b = w * 0.7f;
                c.drawLine(a, a, b, b, p);
                c.drawLine(b, a, a, b, p);
            }

            private void gear(Canvas c, float w, float h) {
                p.setStyle(Paint.Style.STROKE);
                p.setColor(0xFFFFFFFF);
                p.setStrokeCap(Paint.Cap.ROUND);
                p.setStrokeWidth(w * 0.12f);
                c.drawCircle(w / 2, h / 2, w * 0.24f, p);
                p.setStrokeWidth(w * 0.14f);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i;
                    float cs = (float) Math.cos(a), sn = (float) Math.sin(a);
                    c.drawLine(w / 2 + cs * w * 0.34f, h / 2 + sn * w * 0.34f, w / 2 + cs * w * 0.44f, h / 2 + sn * w * 0.44f, p);
                }
            }

            private void tv(Canvas c, float w, float h) {
                // flat-screen TV: rounded screen on a short stand
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFF3A4A5C);
                c.drawRoundRect(new RectF(0, h * 0.08f, w, h * 0.78f), w * 0.12f, w * 0.12f, p);
                p.setColor(0xFF8FC8F5);
                c.drawRoundRect(new RectF(w * 0.1f, h * 0.18f, w * 0.9f, h * 0.68f), w * 0.06f, w * 0.06f, p);
                p.setColor(0xFF3A4A5C);
                c.drawRect(w * 0.4f, h * 0.78f, w * 0.6f, h * 0.9f, p);
                c.drawRoundRect(new RectF(w * 0.25f, h * 0.88f, w * 0.75f, h * 0.98f), w * 0.05f, w * 0.05f, p);
            }

            private void snes(Canvas c, float w, float h) {
                // the four face buttons of the SNES pad: X blue (top), Y green (left), A red (right), B yellow (bottom)
                p.setStyle(Paint.Style.FILL);
                float r = w * 0.2f, cx = w / 2, cy = h / 2, o = w * 0.28f;
                p.setColor(0xFF2F6FDB); c.drawCircle(cx, cy - o, r, p);
                p.setColor(0xFF3DAA3D); c.drawCircle(cx - o, cy, r, p);
                p.setColor(0xFFD93B3B); c.drawCircle(cx + o, cy, r, p);
                p.setColor(0xFFF2C230); c.drawCircle(cx, cy + o, r, p);
            }

            private void tile(Canvas c, float w, float h, String letter, int color) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(color);
                c.drawRoundRect(new RectF(0, 0, w, h), w * 0.25f, w * 0.25f, p);
                p.setColor(0xFFFFFFFF);
                p.setTextAlign(Paint.Align.CENTER);
                p.setTypeface(Typeface.DEFAULT_BOLD);
                p.setTextSize(h * 0.68f);
                c.drawText(letter, w / 2, h / 2 - (p.ascent() + p.descent()) / 2, p);
            }

            @Override public void setAlpha(int a) { p.setAlpha(a); }
            @Override public void setColorFilter(ColorFilter f) { p.setColorFilter(f); }
            @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        };
    }
}
