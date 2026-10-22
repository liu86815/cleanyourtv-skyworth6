package com.gathin.tvhome;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

/** One cached glow mask; alpha needs no full-screen offscreen buffer on Android 6. */
public final class AmbientGlowView extends View {
    private final Paint tint = new Paint();
    public AmbientGlowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(LAYER_TYPE_HARDWARE, tint);
    }
    void setColor(int color) {
        // Update the compositor paint, leaving the white mask texture intact.
        tint.setColorFilter(new PorterDuffColorFilter(color | 0xFF000000, PorterDuff.Mode.SRC_IN));
        setLayerPaint(tint);
    }
    @Override public boolean hasOverlappingRendering() { return false; }
}
