package com.kingpaging.qwelcome.testutil

import com.kingpaging.qwelcome.util.TimeProvider

/**
 * Fake TimeProvider for testing.
 * Allows manual control of elapsed time.
 */
class FakeTimeProvider(private var currentTime: Long = 0L) : TimeProvider {
    override fun elapsedRealtime(): Long = currentTime

    /**
     * Advance time by the specified amount.
     */
    fun advanceBy(millis: Long) {
        currentTime += millis
    }

    /**
     * Set the current time to a specific value.
     */
    fun setTime(millis: Long) {
        currentTime = millis
    }
}
