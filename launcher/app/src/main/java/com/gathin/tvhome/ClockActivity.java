package com.gathin.tvhome;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;

/** Home-only screen saver; no audio focus, media commands, wake-up alarms or global power settings. */
public final class ClockActivity extends Activity {
    private ClockScene scene;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.dream_clock);
        scene = new ClockScene(getWindow().getDecorView());
    }
    @Override protected void onResume() { super.onResume(); scene.start(); }
    @Override protected void onPause() {
        scene.stop();
        super.onPause();
        if (!isFinishing()) finish();
    }
    @Override protected void onDestroy() { scene.stop(); super.onDestroy(); }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_POWER) return super.dispatchKeyEvent(event);
        if (event.getAction() == KeyEvent.ACTION_DOWN) { finish(); overridePendingTransition(0, 0); }
        return true;
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) { finish(); overridePendingTransition(0, 0); }
        return true;
    }
}
