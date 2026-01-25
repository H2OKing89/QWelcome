package com.example.allowelcome.util

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
 */
object SoundManager {
    private var isEnabled = true
    
    /** Coroutine scope for audio playback - uses SupervisorJob so failures don't cancel other sounds */
    private val soundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    /**
     * Plays a cyberpunk-style beep (short high-frequency pulse)
     */
    fun playBeep() {
        if (!isEnabled) return
        
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
        if (!isEnabled) return
        
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

        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
        
        // Wait for playback to complete then release
        delay(durationMs.toLong() + 20)
        audioTrack.stop()
        audioTrack.release()
    }
}
