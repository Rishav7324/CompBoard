package com.example.ime

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class FeedbackEngine(context: Context) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun performHapticFeedback(level: Int = 1) {
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (level) {
                1 -> VibrationEffect.createOneShot(5, 50)
                2 -> VibrationEffect.createOneShot(20, 128)
                3 -> VibrationEffect.createOneShot(40, 255)
                else -> VibrationEffect.createOneShot(5, 50)
            }
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val duration = when (level) {
                1 -> 5L
                2 -> 20L
                3 -> 40L
                else -> 5L
            }
            vibrator.vibrate(duration)
        }
    }
    
    fun performModifierLockHaptic() {
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 20, 100, 20)
            val amplitudes = intArrayOf(0, 150, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 20, 100, 20), -1)
        }
    }
}
