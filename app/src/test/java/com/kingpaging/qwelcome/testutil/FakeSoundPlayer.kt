package com.kingpaging.qwelcome.testutil

import com.kingpaging.qwelcome.util.SoundPlayer

/**
 * Test-only [SoundPlayer] that records calls for verification.
 */
class FakeSoundPlayer : SoundPlayer {
    val beepCount get() = beepCalls
    val confirmCount get() = confirmCalls

    private var beepCalls = 0
    private var confirmCalls = 0

    override fun playBeep() {
        beepCalls++
    }

    override fun playConfirm() {
        confirmCalls++
    }

    fun reset() {
        beepCalls = 0
        confirmCalls = 0
    }
}
