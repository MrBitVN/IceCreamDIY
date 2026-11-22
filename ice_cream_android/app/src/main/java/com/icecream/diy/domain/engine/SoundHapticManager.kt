package com.icecream.diy.domain.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.sin

class SoundHapticManager(private val context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var audioTrack: AudioTrack? = null
    private var isAudioPlaying = false
    private var lastVibrateTime = 0L

    var isSoundEnabled: Boolean = true
        set(value) {
            field = value
            if (!value && isAudioPlaying) {
                stopPouringFeedback()
            }
        }

    var isHapticsEnabled: Boolean = true

    init {
        initSynthesizedAudio()
    }

    private fun initSynthesizedAudio() {
        try {
            val sampleRate = 44100
            val durationSec = 1.0
            val numSamples = (durationSec * sampleRate).toInt()
            val samples = ShortArray(numSamples)

            // Synthesize soft pleasant mechanical hum (160 Hz tone + harmonics)
            for (i in 0 until numSamples) {
                val time = i.toDouble() / sampleRate
                val fundamental = sin(2.0 * Math.PI * 160.0 * time) * 0.4
                val harmonic = sin(2.0 * Math.PI * 320.0 * time) * 0.15
                val softHiss = (Math.random() - 0.5) * 0.05
                val sample = ((fundamental + harmonic + softHiss) * 32767.0).toInt().coerceIn(-32768, 32767)
                samples[i] = sample.toShort()
            }

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
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

            audioTrack?.write(samples, 0, samples.size)
            audioTrack?.setLoopPoints(0, samples.size, -1)
        } catch (_: Exception) {
        }
    }

    fun startPouringFeedback() {
        if (isSoundEnabled && !isAudioPlaying) {
            try {
                audioTrack?.play()
                isAudioPlaying = true
            } catch (_: Exception) {
            }
        }
        triggerHapticPulse()
    }

    fun triggerHapticPulse() {
        if (!isHapticsEnabled) return
        val now = System.currentTimeMillis()
        if (now - lastVibrateTime > 75) {
            lastVibrateTime = now
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(25, 45))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(25)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun stopPouringFeedback() {
        if (isAudioPlaying) {
            try {
                audioTrack?.pause()
                isAudioPlaying = false
            } catch (_: Exception) {
            }
        }
    }

    fun playSuccessFeedback() {
        if (isHapticsEnabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 80), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(100)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun release() {
        stopPouringFeedback()
        try {
            audioTrack?.release()
            audioTrack = null
        } catch (_: Exception) {
        }
    }
}
