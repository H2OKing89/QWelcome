package com.kingpaging.qwelcome.testutil

import com.kingpaging.qwelcome.util.SoundPlayer

/**
 * Test-only [SoundPlayer] that records calls for verification.
 */
class FakeSoundPlayer : SoundPlayer {
    val beepCount get() = _beepCalls
    val confirmCount get() = _confirmCalls

    private var _beepCalls = 0
    private var _confirmCalls = 0

    override fun playBeep() { _beepCalls++ }
    override fun playConfirm() { _confirmCalls++ }

    fun reset() {
        _beepCalls = 0
        _confirmCalls = 0
    }
}
