package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Lightweight generated backdrop for an app that supplies an icon but no Android TV banner. */
final class IconTileArtwork extends Drawable {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path ribbon = new Path();
    private final int seed;
    private final int top;
    private final int middle;
    private final int bottom;

    IconTileArtwork(int sampled, int seed) {
        this.seed = seed;
        float[] hsv = new float[3];
        Color.colorToHSV(sampled, hsv);
        // White and grey icons still receive a restrained blue-slate backdrop.
        if (hsv[1] < .18f) { hsv[0] = 210f + Math.abs(seed % 30); hsv[1] = .46f; }
        hsv[1] = Math.max(.42f, Math.min(.72f, hsv[1]));
        hsv[2] = .46f;
        top = Color.HSVToColor(hsv);
        hsv[2] = .32f;
        middle = Color.HSVToColor(hsv);
        hsv[0] = (hsv[0] + (seed < 0 ? 24f : -24f) + 360f) % 360f;
        hsv[1] = Math.max(.30f, hsv[1] - .12f);
        hsv[2] = .20f;
        bottom = Color.HSVToColor(hsv);
    }

    @Override protected void onBoundsChange(Rect b) {
        fill.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom,
                new int[]{top, middle, bottom}, new float[]{0f, .52f, 1f}, Shader.TileMode.CLAMP));
        glow.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom,
                new int[]{0x32FFFFFF, 0x0FFFFFFF, 0x00101018}, null, Shader.TileMode.CLAMP));
        float w = b.width(), h = b.height();
        float bend = (Math.abs(seed) % 21) / 100f;
        ribbon.reset();
        ribbon.moveTo(b.left - .08f * w, b.top + (.62f + bend) * h);
        ribbon.cubicTo(b.left + .24f * w, b.top + .38f * h,
                b.left + .67f * w, b.top + .88f * h, b.right + .08f * w, b.top + .48f * h);
        ribbon.lineTo(b.right + .08f * w, b.bottom);
        ribbon.lineTo(b.left - .08f * w, b.bottom);
        ribbon.close();
    }

    @Override public void draw(Canvas canvas) {
        canvas.drawRect(getBounds(), fill);
        canvas.drawPath(ribbon, glow);
    }

    @Override public void setAlpha(int alpha) { fill.setAlpha(alpha); glow.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) {
        fill.setColorFilter(filter); glow.setColorFilter(filter); invalidateSelf();
    }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
