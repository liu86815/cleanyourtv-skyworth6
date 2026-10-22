package com.gathin.tvhome;

import android.service.dreams.DreamService;

/** System screensaver host for the same drift and colour animation used by Home's idle clock. */
public class ClockDream extends DreamService {
    private ClockScene scene;
    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);
        setContentView(R.layout.dream_clock);
        scene = new ClockScene(findViewById(R.id.dreamContent).getRootView());
    }
    @Override public void onDreamingStarted() {
        super.onDreamingStarted();
        scene.start();
    }
    @Override public void onDreamingStopped() {
        if (scene != null) scene.stop();
        super.onDreamingStopped();
    }
    @Override public void onDetachedFromWindow() {
        if (scene != null) scene.stop();
        super.onDetachedFromWindow();
    }
}
