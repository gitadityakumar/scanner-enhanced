package com.adi100x.scanner.scanner

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object SoundFeedback {

    fun triggerHaptic(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (!vibrator.hasVibrator()) return
        val effect = VibrationEffect.createOneShot(45L, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(effect)
    }

    suspend fun playScanChime() = withContext(Dispatchers.Default) {
        runCatching {
            val sampleRate = 44100
            val durationMs = 170
            val numSamples = (sampleRate * durationMs) / 1000
            val samples = ShortArray(numSamples)

            // Crisp Paytm-style ascending dual chime: C6 (1046.5Hz) -> E6 (1318.5Hz)
            val split = (numSamples * 0.42).toInt()
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val (freq, localT) = if (i < split) {
                    1046.5 to t
                } else {
                    1318.5 to ((i - split).toDouble() / sampleRate)
                }
                val env = exp(-localT * 22.0) * (1.0 - exp(-localT * 400.0))
                val sample = sin(2.0 * PI * freq * localT) * env * 0.45
                samples[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(maxOf(minBuf, samples.size * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()
            kotlinx.coroutines.delay(durationMs.toLong() + 60L)
            track.stop()
            track.release()
        }
    }
}
