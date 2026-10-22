package com.gathin.tvhome;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import java.util.Random;

/** Original clock, minute-by-minute drift and eight-second colour transition, shared by both hosts. */
final class ClockScene {
    private static final int[] PALETTE = {0xFF2B5CFF, 0xFF00A39A, 0xFF8A3FFC, 0xFFE0661B, 0xFFD6336C};
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final View content;
    private final GradientDrawable glow = new GradientDrawable();
    private ValueAnimator colour;
    private int index, current;
    private boolean active;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!active) return;
            content.animate().alpha(0f).setDuration(900).withEndAction(() -> {
                if (!active) return;
                place();
                content.animate().alpha(1f).setDuration(1200).start();
            }).start();
            colour = ValueAnimator.ofObject(new ArgbEvaluator(), current, PALETTE[index = (index + 1) % PALETTE.length]);
            colour.setDuration(8000);
            colour.addUpdateListener(a -> tint((int)a.getAnimatedValue()));
            colour.start();
            handler.postDelayed(this, 60_000);
        }
    };
    ClockScene(View root) {
        content = root.findViewById(R.id.dreamContent);
        View ambient = root.findViewById(R.id.dreamAmbient);
        glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        ambient.setBackground(glow);
        ambient.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> glow.setGradientRadius((r - l) * .55f));
        index = random.nextInt(PALETTE.length);
        tint(PALETTE[index]);
    }
    void start() {
        stop();
        active = true;
        content.setAlpha(0f);
        content.post(() -> {
            if (!active) return;
            place();
            content.animate().alpha(1f).setDuration(1200).start();
        });
        handler.postDelayed(tick, 60_000);
    }
    void stop() {
        active = false;
        handler.removeCallbacks(tick);
        content.animate().cancel();
        if (colour != null) { colour.cancel(); colour = null; }
    }
    private void place() {
        View parent = (View)content.getParent();
        int maxX = Math.max(0, parent.getWidth() - content.getWidth());
        int maxY = Math.max(0, parent.getHeight() - content.getHeight());
        float x = maxX * (.1f + .8f * random.nextFloat());
        float y = maxY * (.1f + .8f * random.nextFloat());
        content.setX(x); content.setY(y);
        if (parent.getWidth() > 0 && parent.getHeight() > 0)
            glow.setGradientCenter((x + content.getWidth()/2f)/parent.getWidth(),
                    (y + content.getHeight()/2f)/parent.getHeight());
    }
    private void tint(int color) {
        current = color;
        int rgb = color & 0xFFFFFF;
        glow.setColors(new int[]{0x40000000 | rgb, 0x14000000 | rgb, rgb});
    }
}
