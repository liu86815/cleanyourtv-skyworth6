package com.gathin.tvhome;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.os.SystemClock;

import java.util.TimeZone;

/** One exact alarm at a time; the receiver schedules tomorrow before showing the system warning. */
final class ShutdownScheduler {
    static final String ACTION = "com.gathin.tvhome.DAILY_SHUTDOWN";
    static final String PREFS = "daily_shutdown";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_HOUR = "hour";
    static final String KEY_MINUTE = "minute";
    static final int DEFAULT_HOUR = 21;
    static final int DEFAULT_MINUTE = 30;
    private static final String KEY_DUE = "scheduled_warning";
    private static final String KEY_DUE_ELAPSED = "scheduled_warning_elapsed";
    private static final String KEY_TOKEN = "scheduled_token";
    private static final String KEY_LAST_DELIVERED = "last_delivered_warning";
    private static final String EXTRA_DUE = "warning_due";
    private static final String EXTRA_DUE_ELAPSED = "warning_due_elapsed";
    private static final String EXTRA_TOKEN = "warning_token";

    private ShutdownScheduler() {}

    static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // This build was requested with 21:30 enabled; the preference makes later changes persistent.
    static boolean isEnabled(Context context) {
        return preferences(context).getBoolean(KEY_ENABLED, true);
    }

    static int hour(Context context) {
        return preferences(context).getInt(KEY_HOUR, DEFAULT_HOUR);
    }

    static int minute(Context context) {
        return preferences(context).getInt(KEY_MINUTE, DEFAULT_MINUTE);
    }

    static synchronized void schedule(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        Intent intent = new Intent(context, ShutdownAlarmReceiver.class).setAction(ACTION);
        PendingIntent alarm = PendingIntent.getBroadcast(context, 2130, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (alarm != null) alarms.cancel(alarm);
        SharedPreferences prefs = preferences(context);
        // Invalidate queued deliveries even when the feature is being disabled.
        if (!prefs.edit().remove(KEY_DUE).remove(KEY_DUE_ELAPSED).remove(KEY_TOKEN).commit()) return;
        if (!isEnabled(context)) return;

        long now = System.currentTimeMillis();
        long due = ShutdownTiming.nextWarning(now, hour(context), minute(context), TimeZone.getDefault());
        long dueElapsed = SystemClock.elapsedRealtime() + (due - now);
        String token = java.util.UUID.randomUUID().toString();
        intent.putExtra(EXTRA_DUE, due).putExtra(EXTRA_DUE_ELAPSED, dueElapsed)
                .putExtra(EXTRA_TOKEN, token);
        if (!prefs.edit().putLong(KEY_DUE, due).putLong(KEY_DUE_ELAPSED, dueElapsed)
                .putString(KEY_TOKEN, token).commit()) return;
        alarm = PendingIntent.getBroadcast(context, 2130, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarms.cancel(alarm);
        // A shutdown reminder must not wake a TV that is already in standby.
        alarms.setExact(AlarmManager.RTC, due, alarm);
    }

    static synchronized boolean consume(Context context, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction()) || !isEnabled(context)) return false;
        SharedPreferences prefs = preferences(context);
        String token = intent.getStringExtra(EXTRA_TOKEN);
        if (token == null || !token.equals(prefs.getString(KEY_TOKEN, null))) return false;
        long due = intent.getLongExtra(EXTRA_DUE, 0);
        long dueElapsed = intent.getLongExtra(EXTRA_DUE_ELAPSED, -1);
        if (dueElapsed != prefs.getLong(KEY_DUE_ELAPSED, -2)) return false;
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (!ShutdownTiming.canDeliver(System.currentTimeMillis(), SystemClock.elapsedRealtime(),
                due, dueElapsed, prefs.getLong(KEY_DUE, 0), prefs.getLong(KEY_LAST_DELIVERED, 0),
                power != null && power.isInteractive())) return false;
        // Persist before invoking the signed system app so a duplicate broadcast cannot replay it.
        return prefs.edit().putLong(KEY_LAST_DELIVERED, due).remove(KEY_TOKEN).commit();
    }
}
