package com.kingpaging.qwelcome.util

/**
 * Abstraction for sound playback, enabling testability and decoupling
 * UI composables from the concrete [SoundManager] singleton.
 *
 * Provide via [com.kingpaging.qwelcome.di.LocalSoundPlayer] and access
 * with `val soundPlayer = LocalSoundPlayer.current` in composables.
 */
interface SoundPlayer {
    /** Plays a short cyberpunk-style error/attention beep. */
    fun playBeep()

    /** Plays an ascending two-tone confirmation sound. */
    fun playConfirm()
}
