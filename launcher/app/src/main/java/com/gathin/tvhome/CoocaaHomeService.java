package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/** Handles Home before the stock launcher; stock-home window events remain a fallback. */
public final class CoocaaHomeService extends AccessibilityService {
    /** Coocaa may open an intermediate loading window after our early boot launch. */
    private static final long BOOT_WINDOW_GUARD_MS = 180_000L;
    private int homeKeys;
    private int stockFallbacks;
    private int bootWindowFallbacks;
    private int launches;
    private long lastHomeKey;
    private long lastLaunch;
    private String lastStockWindow = "";
    private final Handler handler = new Handler();
    private final Runnable openHome = () -> {
        try {
            launches++;
            lastLaunch = android.os.SystemClock.uptimeMillis();
            Log.i("TvHome.Coocaa", "Opening home at " + android.os.SystemClock.uptimeMillis());
            CoocaaCompat.home(this);
        }
        catch (RuntimeException failure) { Log.e("TvHome.Coocaa", "Home return failed", failure); }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i("TvHome.Coocaa", "Home return service connected");
        // Accessibility is restored early in boot. Open our Home here instead of waiting for Coocaa's
        // heavy movie-platform home to finish drawing and emit a window event. The stock-window path below
        // remains a fallback for firmware that binds accessibility late.
        handler.removeCallbacks(openHome);
        handler.post(openHome);
    }

    @Override protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_HOME) return false;
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            homeKeys++;
            lastHomeKey = event.getEventTime();
            handler.removeCallbacks(openHome);
            // Return the filter result before a possibly slow ActivityManager binder call.
            // Otherwise Android's key-filter timeout can pass Home to the stock launcher.
            Log.i("TvHome.Coocaa", "Home key at " + event.getEventTime());
            handler.post(openHome);
        }
        return true;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        CharSequence className = event.getClassName();
        String name = className == null ? "<window>" : className.toString();
        lastStockWindow = name;
        if ("com.skyworth.tv.activity.SkyTVMainActivity".equals(name)) {
            BootTvInterceptActivity.clearVendorWindowAllowance();
            return;
        }
        long uptime = android.os.SystemClock.uptimeMillis();
        boolean vendorWindowsAllowed = BootTvInterceptActivity.vendorWindowsAllowed(uptime);
        boolean stockHome = "com.coocaa.homepage.vast.HomePageActivity".equals(name)
                || "com.ccos.cooui.movieplatform.MainActivity".equals(name)
                || (!vendorWindowsAllowed
                        && "com.ccos.cooui.movieplatform.SplashDialog".equals(name));
        // These are user-requested overlays hosted by the same package as the stock launcher. In
        // particular, Accessibility reports SourceDialog (not SourceDialogActivity), so a package-wide
        // cold-boot rule must explicitly leave it alone.
        boolean userOverlay = name.startsWith("com.ccos.cooui.homepage.tvsource.")
                || name.contains("SleepTimerCoundDown");
        // During cold boot the movie-platform package creates a loading window after this service has
        // already opened our Home. TYPE_WINDOWS_CHANGED is delivered as that window is attached, earlier
        // than the old activity-state callback which arrived only after "Desktop loading" had finished.
        // Restrict the broad match to early system uptime, so Inputs and other Coocaa screens work normally.
        boolean moviePlatformBootWindow = !userOverlay
                && !vendorWindowsAllowed
                && uptime < BOOT_WINDOW_GUARD_MS
                && (name.startsWith("com.coocaa.") || name.startsWith("com.ccos."));
        if (!stockHome && !moviePlatformBootWindow) return;
        handler.removeCallbacks(openHome);
        stockFallbacks++;
        if (moviePlatformBootWindow && !stockHome) bootWindowFallbacks++;
        Log.i("TvHome.Coocaa", "Stock window " + name + " intercepted at "
                + android.os.SystemClock.uptimeMillis());
        handler.post(openHome);
    }

    @Override public void onInterrupt() { }

    @Override protected void dump(java.io.FileDescriptor fd, java.io.PrintWriter writer, String[] args) {
        writer.println("Home keys=" + homeKeys + " stock fallbacks=" + stockFallbacks
                + " boot windows=" + bootWindowFallbacks + " launches=" + launches
                + " last key=" + lastHomeKey + " last launch=" + lastLaunch
                + " last stock window=" + lastStockWindow);
    }
    @Override public void onDestroy() {
        handler.removeCallbacks(openHome);
        super.onDestroy();
    }
}
