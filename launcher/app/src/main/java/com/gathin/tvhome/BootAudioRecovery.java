package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import dalvik.system.DexClassLoader;

/** One vendor mute pulse per system boot, restoring the original state without changing volume. */
final class BootAudioRecovery {
    private static final Uri PROVIDER = Uri.parse("content://com.tianci.provider");
    private static final String KEY = "SKY_CFG_TV_AUDIO_MUTE";
    private static boolean running;

    private BootAudioRecovery() { }

    static synchronized void onHomeCreated(Context context) {
        if (running || !CoocaaCompat.isSkyworth(context)) return;
        running = true;
        Context app = context.getApplicationContext();
        new Thread(() -> {
            SharedPreferences prefs = app.getSharedPreferences("boot_audio_recovery", Context.MODE_PRIVATE);
            try (BufferedReader reader = new BufferedReader(
                    new FileReader("/proc/sys/kernel/random/boot_id"))) {
                String boot = reader.readLine();
                if (boot == null || boot.equals(prefs.getString("attempt_boot", ""))) return;
                ApplicationInfo system = app.getPackageManager().getApplicationInfo("com.tianci.system", 0);
                ClassLoader loader = new DexClassLoader(system.sourceDir,
                        app.getCodeCacheDir().getAbsolutePath(), null, app.getClassLoader());
                Bundle args = new Bundle();
                args.putString("key", KEY);
                Bundle response = app.getContentResolver().call(PROVIDER,
                        "METHOD_SYSTEM_CMD_GET_CONFIG", null, args);
                if (response == null) throw new IllegalStateException("mute setting unavailable");
                response.setClassLoader(loader);
                Object data = response.getSerializable("ret");
                if (data == null) throw new IllegalStateException("mute data unavailable");
                boolean original = (Boolean) data.getClass().getMethod("isOn").invoke(data);
                // Save before the first change so reopening Home cannot start another pulse.
                if (!prefs.edit().putString("attempt_boot", boot).putString("status", "started")
                        .putBoolean("original_mute", original).commit()) return;
                try {
                    writeMute(app, data, !original);
                    SystemClock.sleep(150);
                } finally {
                    // Always restore, including when the first provider call throws.
                    writeMute(app, data, original);
                }
                prefs.edit().putString("status", "completed")
                        .putLong("completed_uptime", SystemClock.uptimeMillis()).apply();
                Log.i("TvHome.Audio", "Boot mute pulse completed; restored=" + original);
            } catch (Exception failure) {
                prefs.edit().putString("status", "failed: " + failure.getClass().getSimpleName()).apply();
                Log.e("TvHome.Audio", "Boot mute pulse failed", failure);
            } finally {
                synchronized (BootAudioRecovery.class) { running = false; }
            }
        }, "boot-audio-recovery").start();
    }

    private static void writeMute(Context context, Object data, boolean mute) throws Exception {
        data.getClass().getMethod("setOn", boolean.class).invoke(data, mute);
        Bundle args = new Bundle();
        args.putSerializable("data", (Serializable) data);
        context.getContentResolver().call(PROVIDER, "METHOD_SYSTEM_CMD_SET_CONFIG", null, args);
    }
}
