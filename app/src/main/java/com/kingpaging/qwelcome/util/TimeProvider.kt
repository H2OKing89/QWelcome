package com.kingpaging.qwelcome.util

import android.os.SystemClock

/**
 * Abstraction for time sources.
 *
 * Using SystemClock.elapsedRealtime() for interval timing instead of
 * System.currentTimeMillis() because it's monotonic and not affected by
 * system clock changes (user changing time, NTP sync, etc.).
 *
 * This interface allows mocking time in tests without Robolectric.
 */
interface TimeProvider {
    /**
     * Returns elapsed milliseconds since boot (including time spent in sleep).
     * This is the preferred time base for general purpose interval timing.
     */
    fun elapsedRealtime(): Long
}

/**
 * Production implementation using SystemClock.
 */
class SystemTimeProvider : TimeProvider {
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
}
