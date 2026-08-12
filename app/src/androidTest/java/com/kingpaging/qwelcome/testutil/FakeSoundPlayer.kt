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
