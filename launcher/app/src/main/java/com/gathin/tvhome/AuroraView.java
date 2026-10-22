package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

/** Static, small native texture: no timer, video, blur pass or per-frame rendering. */
public final class AuroraView extends View {
    static final String PREFERENCES = "background_choice";
    static final String PALETTE = "palette";
    static final int COUNT = 6;
    private int palette = -1;

    public AuroraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPalette(selected(context));
    }

    static int selected(Context context) {
        int value = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getInt(PALETTE, 0);
        return value >= 0 && value < COUNT ? value : 0;
    }

    void setPalette(int value) {
        value = value >= 0 && value < COUNT ? value : 0;
        if (palette == value) return;
        palette = value;
        final int w = 720, h = 405;
        Bitmap texture = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        texture.setDensity(Bitmap.DENSITY_NONE);
        Canvas canvas = new Canvas(texture);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawColor(0xFF020306);
        // Soft, diffuse aurora bands, baked once; no animated blur or sharp ribbon outlines.
        canvas.save();
        canvas.scale(1f, .52f, w * .5f, h * .28f);
        glow(canvas, paint, w * .28f, h * .22f, w * .59f, colors(0xE5009DBA)[0]);
        glow(canvas, paint, w * .77f, h * .49f, w * .49f, colors(0xD76536B8)[0]);
        glow(canvas, paint, w * .43f, h * .47f, w * .43f, colors(0xA32450A3)[0]);
        canvas.restore();
        // Black upper edge keeps the clock legible; the lower half stays quiet behind the dock.
        paint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xB3000000, 0x00000000, 0xEE000000, 0xFF000000},
                new float[]{0, .27f, .78f, 1}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        BitmapDrawable background = new BitmapDrawable(getResources(), texture);
        background.setGravity(Gravity.FILL);
        background.setFilterBitmap(true);
        setBackground(background);
    }

    private int[] colors(int... values) {
        if (palette == 0) return values;
        final float[] hues = {0, -60, -115, 120, 55, 0};
        final float[] saturation = {1, 1, .85f, 1, .7f, 0};
        final float[] brightness = {1, 1, .85f, .9f, 1, .7f};
        float[] hsv = new float[3];
        for (int i = 0; i < values.length; i++) {
            Color.colorToHSV(values[i], hsv);
            hsv[0] = (hsv[0] + hues[palette] + 360) % 360;
            hsv[1] *= saturation[palette];
            hsv[2] *= brightness[palette];
            values[i] = Color.HSVToColor(Color.alpha(values[i]), hsv);
        }
        return values;
    }

    private static void glow(Canvas canvas, Paint paint, float x, float y, float radius, int color) {
        paint.setShader(new RadialGradient(x, y, radius,
                new int[]{color, color & 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        canvas.drawCircle(x, y, radius, paint);
    }

    @Override public boolean hasOverlappingRendering() { return false; }
}
