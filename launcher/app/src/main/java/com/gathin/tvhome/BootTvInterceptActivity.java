package com.gathin.tvhome;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

/** Routes Coocaa's implicit boot-TV launch to Home without changing normal TV input behavior. */
public final class BootTvInterceptActivity extends Activity {
    private static final long BOOT_GUARD_MS = 180_000L;
    private static volatile long allowVendorWindowsUntil;
    private static final ComponentName SKY_TV = new ComponentName(
            "com.skyworth.tv", "com.skyworth.tv.activity.SkyTVMainActivity");

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Intent incoming = getIntent();
        String startType = incoming == null ? null : incoming.getStringExtra("startType");
        boolean boot = "onBoot".equals(startType) && SystemClock.uptimeMillis() < BOOT_GUARD_MS;
        Intent target;
        if (boot) {
            target = new Intent(this, MainActivity.class).setAction(Intent.ACTION_MAIN);
            Log.i("TvHome.Coocaa", "Intercepted boot TV launch at " + SystemClock.uptimeMillis());
        } else {
            // A deliberate input switch can briefly use a movie-platform splash window. Let that
            // flow pass the cold-boot window guard; process-local state automatically resets on reboot.
            allowVendorWindowsUntil = SystemClock.uptimeMillis() + 15_000L;
            target = incoming == null ? new Intent() : new Intent(incoming);
            target.setComponent(SKY_TV);
            Log.i("TvHome.Coocaa", "Forwarding normal TV launch type=" + startType);
        }
        // End the no-display dispatcher first. On this Android 6 build, finishing it after a
        // cold NEW_TASK launch can bring the underlying Home task back over the new TV task.
        target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            finish();
            startActivity(target);
        } catch (RuntimeException failure) {
            Log.e("TvHome.Coocaa", "Cannot route TV launch", failure);
            if (!boot) CoocaaCompat.home(this);
        }
    }

    static boolean vendorWindowsAllowed(long uptime) {
        return uptime < allowVendorWindowsUntil;
    }

    static void clearVendorWindowAllowance() {
        allowVendorWindowsUntil = 0L;
    }
}
