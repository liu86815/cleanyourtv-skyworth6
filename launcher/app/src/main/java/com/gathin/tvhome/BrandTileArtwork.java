package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** A consistent, static brand field behind each app's real icon and launcher-owned label. */
final class BrandTileArtwork extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int accent;

    BrandTileArtwork(int accent) { this.accent = accent; }

    static int accent(String pkg, int sampled) {
        switch (pkg) {
            case "net.dasoft.myplayer": return 0xFF414D91;
            case "com.tencent.qqmusictv": return 0xFF07856C;
            case "com.tianci.de": return 0xFF2261AE;
            case "org.xbmc.kodi": return 0xFF167D9C;
            case "com.tianci.localmedia": return 0xFF68509C;
            default:
                float[] hsv = new float[3];
                Color.colorToHSV(sampled, hsv);
                if (hsv[1] < .15f) hsv[0] = 220f;
                hsv[1] = Math.max(.35f, Math.min(.65f, hsv[1]));
                hsv[2] = .55f;
                return Color.HSVToColor(hsv);
        }
    }

    @Override protected void onBoundsChange(Rect b) {
        float[] hsv = new float[3];
        Color.colorToHSV(accent, hsv);
        hsv[2] = Math.min(.8f, hsv[2] * 1.12f);
        int top = Color.HSVToColor(hsv);
        hsv[2] *= .72f;
        paint.setShader(new LinearGradient(0, b.top, 0, b.bottom,
                top, Color.HSVToColor(hsv), Shader.TileMode.CLAMP));
    }
    @Override public void draw(Canvas canvas) { canvas.drawRect(getBounds(), paint); }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
