package com.gathin.tvhome;

import org.junit.Test;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class ShutdownTimingTest {
    private static final TimeZone ZONE = TimeZone.getTimeZone("Asia/Shanghai");

    private long time(int day, int hour, int minute, int second) {
        Calendar c = Calendar.getInstance(ZONE);
        c.clear();
        c.set(2026, Calendar.OCTOBER, day, hour, minute, second);
        return c.getTimeInMillis();
    }

    @Test public void schedulesWarningOneMinuteBeforeShutdown() {
        assertEquals(time(22, 21, 29, 0),
                ShutdownTiming.nextWarning(time(22, 20, 0, 0), 21, 30, ZONE));
    }

    @Test public void powerOnAfterDeadlineSchedulesTomorrowWithoutCatchup() {
        assertEquals(time(23, 21, 29, 0),
                ShutdownTiming.nextWarning(time(22, 22, 0, 0), 21, 30, ZONE));
    }

    @Test public void enteringDuringCountdownDoesNotReplayToday() {
        assertEquals(time(23, 21, 29, 0),
                ShutdownTiming.nextWarning(time(22, 21, 29, 0), 21, 30, ZONE));
    }

    @Test public void midnightShutdownHasPreviousDayWarning() {
        assertEquals(time(22, 23, 59, 0),
                ShutdownTiming.nextWarning(time(22, 23, 0, 0), 0, 0, ZONE));
    }

    @Test public void dailyScheduleUsesCalendarAcrossDaylightSaving() {
        TimeZone ny = TimeZone.getTimeZone("America/New_York");
        Calendar c = Calendar.getInstance(ny);
        c.clear();
        c.set(2026, Calendar.MARCH, 7, 22, 0, 0);
        long next = ShutdownTiming.nextWarning(c.getTimeInMillis(), 21, 30, ny);
        c.setTimeInMillis(next);
        assertEquals(8, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(21, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(29, c.get(Calendar.MINUTE));
    }

    private boolean delivery(long wallDelay, long elapsedDelay, long last, boolean awake) {
        return ShutdownTiming.canDeliver(1_000_000 + wallDelay, 500_000 + elapsedDelay,
                1_000_000, 500_000, 1_000_000, last, awake);
    }

    @Test public void acceptsAnOnTimeInteractiveDelivery() {
        assertTrue(delivery(0, 0, 0, true));
        assertTrue(delivery(15000, 15000, 0, true));
    }

    @Test public void skipsLateWakeupAndNeverRunsAfterShutdownTime() {
        assertFalse(delivery(15001, 15001, 0, true));
        assertFalse(delivery(61000, 61000, 0, true));
        assertFalse(delivery(3600000, 3600000, 0, true));
    }

    @Test public void skipsSleepingDuplicateAndEarlyDeliveries() {
        assertFalse(delivery(0, 0, 0, false));
        assertFalse(delivery(0, 0, 1_000_000, true));
        assertFalse(delivery(-1, -1, 0, true));
    }

    @Test public void skipsOldBootAndClockCorrections() {
        assertFalse(delivery(0, -400000, 0, true));
        assertFalse(delivery(0, 60000, 0, true));
        assertFalse(delivery(60000, 0, 0, true));
    }

    @Test public void skipsMissingOrSupersededOccurrence() {
        assertFalse(ShutdownTiming.canDeliver(1000000, 500000,
                1000000, 500000, 2000000, 0, true));
        assertFalse(ShutdownTiming.canDeliver(0, 0, 0, 0, 0, 0, true));
    }
}
