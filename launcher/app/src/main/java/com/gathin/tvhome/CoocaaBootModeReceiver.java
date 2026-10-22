package com.gathin.tvhome;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.io.Serializable;

import dalvik.system.DexClassLoader;

/** ADB-only switch for the Skyworth boot queue's official boot_start_home_page setting. */
public final class CoocaaBootModeReceiver extends BroadcastReceiver {
    public static final String ACTION_DIRECT_BOOT = "com.gathin.tvhome.DIRECT_BOOT";
    public static final String ACTION_RESTORE = "com.gathin.tvhome.RESTORE_STOCK_BOOT";
    public static final String ACTION_DIAGNOSTICS = "com.gathin.tvhome.VENDOR_DIAGNOSTICS";

    private static final String TAG = "TvHome.BootMode";
    private static final String SYSTEM_PACKAGE = "com.tianci.system";
    private static final String SET_DATA_CLASS = "com.tianci.system.data.TCEnumSetData";
    private static final String SETTING_NAME = "SKY_SYSTEM_ENV_BOOT_START_HOME_PAGE";
    private static final Uri SYSTEM_PROVIDER = Uri.parse("content://com.tianci.provider");

    @Override public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (!ACTION_DIRECT_BOOT.equals(action) && !ACTION_RESTORE.equals(action)
                && !ACTION_DIAGNOSTICS.equals(action)) return;

        PendingResult pending = goAsync();
        Context app = context.getApplicationContext();
        new Thread(() -> {
            boolean direct = ACTION_DIRECT_BOOT.equals(action);
            boolean success = false;
            String detail;
            try {
                if (ACTION_DIAGNOSTICS.equals(action)) {
                    pending.setResultCode(Activity.RESULT_OK);
                    pending.setResultData(readDiagnostics(app));
                    pending.finish();
                    return;
                }
                // Index 0 is HOMEPAGE; index 1 is SOURCE. The latter makes the vendor boot queue
                // skip its stock homepage. Our early CoocaaBootReceiver still starts this launcher.
                setVendorBootOption(app, direct ? 1 : 0);
                success = true;
                detail = direct ? "stock homepage startup disabled" : "stock homepage startup restored";
            } catch (Exception failure) {
                detail = failure.getClass().getSimpleName() + ": " + failure.getMessage();
                Log.e(TAG, "Cannot change Coocaa boot mode", failure);
            }

            app.getSharedPreferences("coocaa_boot_mode_status", Context.MODE_PRIVATE).edit()
                    .putString("action", action)
                    .putBoolean("success", success)
                    .putString("detail", detail)
                    .putLong("finished_at", System.currentTimeMillis())
                    .apply();
            pending.setResultCode(success ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
            pending.setResultData(detail);
            Log.i(TAG, action + " success=" + success + " detail=" + detail);
            pending.finish();
        }, "coocaa-boot-mode").start();
    }

    private static String readDiagnostics(Context context) throws Exception {
        ApplicationInfo system = context.getPackageManager().getApplicationInfo(SYSTEM_PACKAGE, 0);
        ClassLoader loader = new DexClassLoader(system.sourceDir,
                context.getCodeCacheDir().getAbsolutePath(), null, context.getClassLoader());
        StringBuilder result = new StringBuilder();
        result.append("clock_minutes=").append(HomeIdleClock.minutes(context))
                .append(",clock_state=").append(HomeIdleClock.status)
                .append(",clock_due_ms=").append(HomeIdleClock.dueAt == 0 ? 0 :
                        HomeIdleClock.dueAt - android.os.SystemClock.elapsedRealtime()).append(';');
        Config visual = Config.load(context);
        result.append("motion=").append(visual.animations).append(':')
                .append(visual.advancedEffects).append(':').append(visual.reducedMotion).append(';');
        android.content.SharedPreferences audio = context.getSharedPreferences(
                "boot_audio_recovery", Context.MODE_PRIVATE);
        result.append("boot_audio=").append(audio.getString("status", "not_run"))
                .append(",original_mute=").append(audio.getBoolean("original_mute", false)).append(';');
        result.append("shutdown_enabled=").append(ShutdownScheduler.isEnabled(context))
                .append(",warning_due=").append(ShutdownScheduler.preferences(context)
                        .getLong("scheduled_warning", 0)).append(';');
        for (String key : new String[]{"SKY_CFG_TV_AUDIO_MUTE",
                "SKY_CFG_TV_IS_SUPPORT_STR_STANDBY", "SKY_CFG_TV_STANDBY_MODE_QUICK",
                "SKY_CFG_TV_STR_STATE"}) {
            result.append(key).append('=');
            try {
                Bundle args = new Bundle();
                args.putString("key", key);
                Bundle response = context.getContentResolver().call(SYSTEM_PROVIDER,
                        "METHOD_SYSTEM_CMD_GET_CONFIG", null, args);
                if (response == null) { result.append("null;"); continue; }
                response.setClassLoader(loader);
                Object data = response.getSerializable("ret");
                result.append(String.valueOf(data)).append(';');
            } catch (Exception failure) {
                result.append(failure.getClass().getSimpleName()).append(';');
            }
        }
        return result.toString();
    }

    /**
     * Uses the exact vendor data class from the installed system service, then calls the same
     * provider method as the TV's Settings screen. Loading the class from the original APK avoids
     * copying or modifying any Coocaa code and preserves its Java-serialization identity.
     */
    private static void setVendorBootOption(Context context, int index) throws Exception {
        ApplicationInfo system = context.getPackageManager().getApplicationInfo(SYSTEM_PACKAGE, 0);
        File optimized = new File(context.getCodeCacheDir(), "tianci-sdk");
        if (!optimized.exists() && !optimized.mkdirs()) {
            throw new IllegalStateException("cannot create vendor code cache");
        }
        ClassLoader loader = new DexClassLoader(
                system.sourceDir, optimized.getAbsolutePath(), null, context.getClassLoader());
        Class<?> dataClass = loader.loadClass(SET_DATA_CLASS);
        Object data = dataClass.getDeclaredConstructor().newInstance();
        dataClass.getMethod("setName", String.class).invoke(data, SETTING_NAME);
        dataClass.getMethod("setCurrentIndex", int.class).invoke(data, index);

        Bundle extras = new Bundle();
        extras.putSerializable("data", (Serializable) data);
        // A successful call may return a Bundle containing another vendor Serializable. Do not
        // unpack it in this process; completion without an exception means the provider accepted it.
        context.getContentResolver().call(
                SYSTEM_PROVIDER, "METHOD_SYSTEM_CMD_SET_CONFIG", null, extras);
    }
}
