package com.example.ime

import android.os.Handler
import android.os.Looper

class KeyRepeatEngine(private val onRepeat: () -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private var isRepeating = false
    private var repeatCount = 0

    private val repeatRunnable = object : Runnable {
        override fun run() {
            if (!isRepeating) return
            onRepeat()
            repeatCount++
            
            val nextDelay = when {
                repeatCount > 25 -> 10L // Turbo
                repeatCount > 10 -> 20L // Fast
                else -> 40L // Normal
            }
            handler.postDelayed(this, nextDelay)
        }
    }

    fun start() {
        if (isRepeating) return
        isRepeating = true
        repeatCount = 0
        handler.postDelayed(repeatRunnable, 400L) // Initial delay
    }

    fun stop() {
        isRepeating = false
        handler.removeCallbacks(repeatRunnable)
    }
}
