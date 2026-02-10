package com.kingpaging.qwelcome.testutil

import com.kingpaging.qwelcome.util.SoundPlayer

/**
 * Keep this in sync with:
 * `app/src/test/java/com/kingpaging/qwelcome/testutil/FakeSoundPlayer.kt`
 *
 * Android instrumentation tests cannot use the unit-test source set directly,
 * so this duplicate exists intentionally until a shared test-fixtures source
 * set is introduced.
 */
class FakeSoundPlayer : SoundPlayer {
    val beepCount get() = _beepCalls
    val confirmCount get() = _confirmCalls

    private var _beepCalls = 0
    private var _confirmCalls = 0

    override fun playBeep() {
        _beepCalls++
    }

    override fun playConfirm() {
        _confirmCalls++
    }

    fun reset() {
        _beepCalls = 0
        _confirmCalls = 0
    }
}
