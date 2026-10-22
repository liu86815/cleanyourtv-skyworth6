package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import java.util.function.BooleanSupplier;

/** One delayed callback, owned by the visible Home activity; never runs over another app. */
final class HomeIdleClock {
    static final String PREFS = "home_clock";
    static final String MINUTES = "minutes";
    static String status = "not_created";
    static long dueAt;
    private final Activity owner;
    private final BooleanSupplier unobstructed;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean resumed;
    private final Runnable due = this::show;

    HomeIdleClock(Activity owner, BooleanSupplier unobstructed) {
        this.owner = owner;
        this.unobstructed = unobstructed;
    }
    static int minutes(Context c) {
        int value = c.getSharedPreferences(PREFS, 0).getInt(MINUTES, 5);
        return value == 0 || value == 2 || value == 5 || value == 10 || value == 15 ? value : 5;
    }
    void resume() { resumed = true; reset(); }
    void pause() { resumed = false; handler.removeCallbacks(due); status = "paused"; dueAt = 0; }
    void reset() {
        handler.removeCallbacks(due);
        int minutes = minutes(owner);
        status = "waiting:resumed=" + resumed + ",focus=" + owner.hasWindowFocus() + ",minutes=" + minutes;
        dueAt = 0;
        // OEMs may restore focus after onResume without another focus callback. Arm now;
        // show() still requires a focused, resumed Home before it can open the clock.
        if (resumed && minutes > 0) {
            status = "armed";
            dueAt = SystemClock.elapsedRealtime() + minutes * 60_000L;
            handler.postDelayed(due, minutes * 60_000L);
        }
    }
    private void show() {
        if (!resumed || owner.isFinishing() || !owner.hasWindowFocus()) { status = "not_foreground"; return; }
        PowerManager power = (PowerManager)owner.getSystemService(Context.POWER_SERVICE);
        if (power == null || !power.isInteractive()) { status = "screen_off"; return; }
        if (!unobstructed.getAsBoolean()) { reset(); status = "blocked_by_home_panel"; return; }
        status = "showing";
        owner.startActivity(new Intent(owner, ClockActivity.class));
        owner.overridePendingTransition(0, 0);
    }
}
