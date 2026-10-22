package com.gathin.tvhome;

import java.util.Calendar;
import java.util.TimeZone;

/** Pure timing rules: an expired warning must never be replayed on the next power-on. */
final class ShutdownTiming {
    static final long WARNING_MILLIS = 60_000L;
    static final long MAX_DELIVERY_DELAY_MILLIS = 15_000L;

    private ShutdownTiming() { }

    static long nextWarning(long now, int hour, int minute, TimeZone zone) {
        Calendar deadline = Calendar.getInstance(zone);
        deadline.setTimeInMillis(now);
        deadline.set(Calendar.HOUR_OF_DAY, hour);
        deadline.set(Calendar.MINUTE, minute);
        deadline.set(Calendar.SECOND, 0);
        deadline.set(Calendar.MILLISECOND, 0);
        if (deadline.getTimeInMillis() - WARNING_MILLIS <= now) {
            deadline.add(Calendar.DAY_OF_YEAR, 1);
        }
        return deadline.getTimeInMillis() - WARNING_MILLIS;
    }

    static boolean canDeliver(long now, long elapsed, long due, long dueElapsed,
            long storedDue, long lastDeliveredDue, boolean interactive) {
        if (!interactive || due <= 0 || due != storedDue || due <= lastDeliveredDue) return false;
        long wallDelay = now - due;
        long elapsedDelay = elapsed - dueElapsed;
        // Both clocks must agree. This rejects early broadcasts, delayed wakeup delivery,
        // rebooted sessions and wall-clock corrections that invalidate the original alarm.
        return wallDelay >= 0 && wallDelay <= MAX_DELIVERY_DELAY_MILLIS
                && elapsedDelay >= 0 && elapsedDelay <= MAX_DELIVERY_DELAY_MILLIS;
    }
}
