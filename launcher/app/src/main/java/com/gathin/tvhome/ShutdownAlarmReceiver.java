package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Hands shutdown to the TV's signed settings app, which shows its cancellable one-minute warning. */
public final class ShutdownAlarmReceiver extends BroadcastReceiver {
    private static final String COOCAA_COUNTDOWN = "com.skyworth.broadcast.sleeptimer.countdown";
    private static final String COOCAA_SETTINGS = "com.tianci.setting";

    @Override public void onReceive(Context context, Intent intent) {
        // Validate the delivered occurrence before schedule() replaces its identity with tomorrow's.
        boolean showWarning = ShutdownScheduler.consume(context, intent);
        ShutdownScheduler.schedule(context);
        if (showWarning) {
            context.sendBroadcast(new Intent(COOCAA_COUNTDOWN).setPackage(COOCAA_SETTINGS));
        }
    }
}
