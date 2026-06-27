package com.example.ime

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.R

class FloatingKeyboardService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Use a simple View for floating keyboard
        floatingView = FrameLayout(this)
        floatingView.setBackgroundColor(android.graphics.Color.parseColor("#0A0A0A"))
        
        val dragHandle = View(this)
        dragHandle.setBackgroundColor(android.graphics.Color.parseColor("#444444"))
        val handleParams = FrameLayout.LayoutParams(120, 12, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        handleParams.topMargin = 16
        (floatingView as FrameLayout).addView(dragHandle, handleParams)
        
        val closeBtn = TextView(this)
        closeBtn.text = "X"
        closeBtn.setTextColor(android.graphics.Color.WHITE)
        val closeParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END)
        closeParams.setMargins(16, 16, 16, 16)
        (floatingView as FrameLayout).addView(closeBtn, closeParams)
        
        closeBtn.setOnClickListener {
            stopSelf()
        }
        
        // Ideally we'd mount the KeyboardView here but it requires an InputConnection
        // In a real app, this would use AccessibilityService to inject key events 
        // or communicate with the IME service.
        val text = TextView(this)
        text.text = "Floating Keyboard\n(Requires Accessibility to inject keys)"
        text.setTextColor(android.graphics.Color.WHITE)
        val textParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        (floatingView as FrameLayout).addView(text, textParams)

        params = WindowManager.LayoutParams(
            resources.displayMetrics.widthPixels - 64,
            600,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.x = 0
        params.y = 100

        windowManager.addView(floatingView, params)
        
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        
        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY - (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}
