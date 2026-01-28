package com.kingpaging.qwelcome.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Manages cyberpunk sound effects for the app.
 * Generates synthesized beep sounds for that retro-futuristic feel.
 * 
 * Uses coroutines for proper lifecycle management and resource efficiency.
 * 
 * IMPORTANT: Call [shutdown] when the app is terminating to cancel pending jobs
 * and release resources. This can be wired to ProcessLifecycleOwner.onStop or
 * Application.onTerminate.
 */
object SoundManager {
    private var isEnabled = true
    private var isShutdown = false
    
    /** Coroutine scope for audio playback - uses SupervisorJob so failures don't cancel other sounds */
    private var supervisorJob = SupervisorJob()
    private var soundScope = CoroutineScope(Dispatchers.IO + supervisorJob)

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    /**
     * Shuts down the SoundManager, cancelling all pending audio jobs.
     * After calling this, [playBeep] and [playConfirm] will no longer queue new sounds.
     * 
     * This should be called when the app is terminating to prevent resource leaks.
     * Can be wired to ProcessLifecycleOwner callbacks or Application.onTerminate.
     * 
     * Call [restart] to re-enable sound playback after shutdown.
     */
    fun shutdown() {
        isShutdown = true
        supervisorJob.cancel()
    }
    
    /**
     * Restarts the SoundManager after a [shutdown], allowing sounds to play again.
     * Creates a fresh coroutine scope for new audio jobs.
     */
    fun restart() {
        if (isShutdown) {
            supervisorJob = SupervisorJob()
            soundScope = CoroutineScope(Dispatchers.IO + supervisorJob)
            isShutdown = false
        }
    }
    
    /**
     * Returns true if the SoundManager has been shut down and is not accepting new jobs.
     */
    fun isShutdown(): Boolean = isShutdown

    /**
     * Plays a cyberpunk-style beep (short high-frequency pulse)
     */
    fun playBeep() {
        if (!isEnabled || isShutdown) return
        
        soundScope.launch {
            try {
                playTone(frequency = 880.0, durationMs = 50, volume = 0.3f)
            } catch (e: Exception) {
                // Silently fail if audio isn't available
            }
        }
    }

    /**
     * Plays a confirmation sound (ascending two-tone)
     */
    fun playConfirm() {
        if (!isEnabled || isShutdown) return
        
        soundScope.launch {
            try {
                playTone(frequency = 660.0, durationMs = 40, volume = 0.25f)
                delay(50)
                playTone(frequency = 880.0, durationMs = 60, volume = 0.3f)
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    private suspend fun playTone(frequency: Double, durationMs: Int, volume: Float) {
        val sampleRate = 44100
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        val samples = ShortArray(numSamples)

        // Generate sine wave with fade in/out for smooth sound
        val fadeLength = (numSamples * 0.1).toInt()
        for (i in 0 until numSamples) {
            val angle = 2.0 * PI * i / (sampleRate / frequency)
            var sample = sin(angle)

            // Apply fade in/out envelope
            val envelope = when {
                i < fadeLength -> i.toFloat() / fadeLength
                i > numSamples - fadeLength -> (numSamples - i).toFloat() / fadeLength
                else -> 1f
            }
            sample *= envelope
            samples[i] = (sample * Short.MAX_VALUE * volume).toInt().toShort()
        }

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            val written = audioTrack.write(samples, 0, samples.size)
            if (written != samples.size) return
            audioTrack.play()

            // Wait for playback to complete
            delay(durationMs.toLong() + 20)
        } finally {
            // Ensure AudioTrack is always released, even if delay() throws CancellationException
            try {
                if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.stop()
                }
            } catch (_: IllegalStateException) {
                // Ignore - stop() can throw if playback never started
            }
            audioTrack.release()
        }
    }
}
