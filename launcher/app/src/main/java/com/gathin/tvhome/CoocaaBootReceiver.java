package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

/** Starts Home from whichever boot broadcast this Coocaa firmware delivers first. */
public final class CoocaaBootReceiver extends BroadcastReceiver {
    static final String COMPONENT = "com.gathin.tvhome/.CoocaaHomeService";

    static boolean selected(Context context) {
        String enabled = Settings.Secure.getString(context.getContentResolver(), "enabled_accessibility_services");
        return enabled != null && (enabled.contains(COMPONENT)
                || enabled.contains("com.gathin.tvhome/com.gathin.tvhome.CoocaaHomeService"));
    }

    @Override public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (!("android.intent.action.BOOT_COMPLETED.TC".equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)) || !selected(context)) return;
        try {
            CoocaaCompat.home(context);
            Log.i("TvHome.Coocaa", "Started home from Coocaa boot broadcast");
        } catch (RuntimeException failure) {
            Log.e("TvHome.Coocaa", "Cannot start home", failure);
        }
    }
}
