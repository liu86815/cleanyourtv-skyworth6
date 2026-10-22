package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Vector backdrop behind the real app icon, for apps without a TV banner. */
final class TileArtwork extends Drawable {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path wave = new Path();
    private final boolean music;

    TileArtwork(String pkg) { music = "com.tencent.qqmusictv".equals(pkg); }

    @Override protected void onBoundsChange(Rect b) {
        fill.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom,
                music ? new int[]{0xFF073B3D, 0xFF087A72, 0xFF163E63}
                      : new int[]{0xFF20284C, 0xFF3A4A99, 0xFF165779},
                null, Shader.TileMode.CLAMP));
        sheen.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom,
                new int[]{0x1876F0E6, 0x387EB5FF, 0x0074D4FF}, null, Shader.TileMode.CLAMP));
        float w = b.width(), h = b.height();
        wave.reset();
        wave.moveTo(b.left - .1f * w, b.top + .82f * h);
        wave.cubicTo(b.left + .25f * w, b.top + .12f * h,
                b.left + .66f * w, b.top + 1.2f * h, b.right + .1f * w, b.top + .25f * h);
        wave.lineTo(b.right + .1f * w, b.bottom);
        wave.lineTo(b.left - .1f * w, b.bottom);
        wave.close();
    }

    @Override public void draw(Canvas canvas) {
        canvas.drawRect(getBounds(), fill);
        canvas.drawPath(wave, sheen);
    }

    @Override public void setAlpha(int alpha) { fill.setAlpha(alpha); sheen.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { fill.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
