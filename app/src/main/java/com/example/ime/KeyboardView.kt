package com.example.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.KeyEvent

class KeyboardView(context: Context) : View(context) {

    private var dispatcher: KeyEventDispatcher? = null
    private var gestureDetector: GestureDetector? = null
    
    private var keyboardWidth = 0
    private var keyboardHeight = 0
    private var keyMargin = 0f
    private val keys = mutableListOf<Key>()
    
    // Theme Colors
    private val colorBackground = Color.parseColor("#0A0A0A")
    private val colorSurface = Color.parseColor("#111111")
    private val colorKeyDefault = Color.parseColor("#1E1E1E")
    private val colorKeyModifier = Color.parseColor("#2A1F3D")
    private val colorKeyActive = Color.parseColor("#5C3DDD")
    private val colorKeyAccent = Color.parseColor("#7B5CF6")
    private val colorKeyDanger = Color.parseColor("#C0392B")
    private val colorTextPrimary = Color.parseColor("#F5F5F5")
    private val colorTextSecondary = Color.parseColor("#888888")
    private val colorBorder = Color.parseColor("#2A2A2A")
    private val colorTextAccent = Color.parseColor("#A78BFA")

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorSurface }
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = colorBorder
    }
    
    private val textPaintPrimary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorTextPrimary
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.MONOSPACE
    }
    private val textPaintSecondary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorTextSecondary
        textAlign = Paint.Align.RIGHT
        typeface = android.graphics.Typeface.MONOSPACE
    }
    
    fun setDispatcher(d: KeyEventDispatcher) {
        this.dispatcher = d
        if (context is CompBoardInputMethodService) {
            val handler = GestureHandler(d, context as CompBoardInputMethodService)
            gestureDetector = GestureDetector(context, handler)
        }
    }
    
    fun updateForEditorInfo(info: android.view.inputmethod.EditorInfo?) {
        // Adapt layout based on input type
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return
        keyboardWidth = w
        keyboardHeight = h
        keyMargin = w * 0.008f // ~3dp gap
        
        textPaintPrimary.textSize = w * 0.035f
        textPaintSecondary.textSize = w * 0.025f
        
        generateLayout()
    }
    
    private fun generateLayout() {
        keys.clear()
        val numCols = 15f
        val baseWidth = (keyboardWidth - keyMargin * (numCols + 1)) / numCols
        val rowHeights = listOf(
            keyboardHeight * 0.1f,  // Status Bar
            keyboardHeight * 0.12f, // F-Row
            keyboardHeight * 0.15f, // Num Row
            keyboardHeight * 0.15f, // QWERTY
            keyboardHeight * 0.16f, // Home
            keyboardHeight * 0.15f, // Shift
            keyboardHeight * 0.17f  // Bottom
        )
        
        var currentY = 0f
        
        // Status Bar (Top)
        currentY += rowHeights[0]
        
        // Row 0: F-Row
        var currentX = keyMargin
        val fKeys = listOf("Esc", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12", "Del")
        val fKeyWidth = (keyboardWidth - keyMargin * (fKeys.size + 1)) / fKeys.size
        for (label in fKeys) {
            val code = when (label) {
                "Esc" -> KeyEvent.KEYCODE_ESCAPE
                "Del" -> KeyEvent.KEYCODE_FORWARD_DEL
                else -> KeyEvent.KEYCODE_F1 + (label.drop(1).toIntOrNull()?.minus(1) ?: 0)
            }
            val keyType = if (label == "Esc" || label == "Del") KeyType.DANGER else KeyType.NORMAL
            keys.add(Key(RectF(currentX, currentY, currentX + fKeyWidth, currentY + rowHeights[1] - keyMargin), label, "", code, keyType))
            currentX += fKeyWidth + keyMargin
        }
        currentY += rowHeights[1]
        
        // Helper to add rows
        fun addRow(rowDef: List<Pair<String, Float>>, isShift: Boolean = false) {
            currentX = keyMargin
            for ((label, weight) in rowDef) {
                val keyW = baseWidth * weight + keyMargin * (weight - 1)
                val code = getKeyCode(label)
                val type = when {
                    label in listOf("Tab", "Caps", "Shift", "Ctrl", "Win", "Alt", "Fn") -> KeyType.MODIFIER
                    label == "Enter" -> KeyType.ACCENT
                    else -> KeyType.NORMAL
                }
                keys.add(Key(RectF(currentX, currentY, currentX + keyW, currentY + rowHeights[if (isShift) 4 else 3] - keyMargin), label, "", code, type))
                currentX += keyW + keyMargin
            }
            currentY += rowHeights[if (isShift) 4 else 3]
        }
        
        // Row 1: Numbers
        val numRow = listOf("`" to 1f, "1" to 1f, "2" to 1f, "3" to 1f, "4" to 1f, "5" to 1f, "6" to 1f, "7" to 1f, "8" to 1f, "9" to 1f, "0" to 1f, "-" to 1f, "=" to 1f, "Bksp" to 2f)
        addRow(numRow)
        
        // Row 2: QWERTY
        val qwertyRow = listOf("Tab" to 1.5f, "Q" to 1f, "W" to 1f, "E" to 1f, "R" to 1f, "T" to 1f, "Y" to 1f, "U" to 1f, "I" to 1f, "O" to 1f, "P" to 1f, "[" to 1f, "]" to 1f, "\\" to 1.5f)
        addRow(qwertyRow)
        
        // Row 3: Home
        currentY -= rowHeights[3]
        currentY += rowHeights[3]
        
        val homeRow = listOf("Caps" to 1.8f, "A" to 1f, "S" to 1f, "D" to 1f, "F" to 1f, "G" to 1f, "H" to 1f, "J" to 1f, "K" to 1f, "L" to 1f, ";" to 1f, "'" to 1f, "Enter" to 2.2f)
        addRow(homeRow, true) // uses rowHeight[4]
        
        // Row 4: Shift
        currentY -= rowHeights[4]
        currentY += rowHeights[4]
        val shiftRow = listOf("Shift" to 2.3f, "Z" to 1f, "X" to 1f, "C" to 1f, "V" to 1f, "B" to 1f, "N" to 1f, "M" to 1f, "," to 1f, "." to 1f, "/" to 1f, "Shift" to 2.7f)
        addRow(shiftRow)
        
        // Row 5: Bottom
        currentY -= rowHeights[3]
        currentY += rowHeights[5]
        val bottomRow = listOf("Ctrl" to 1.5f, "Win" to 1.2f, "Alt" to 1.2f, "SPACE" to 6.1f, "Alt" to 1.2f, "Fn" to 1.2f, "<-" to 1f, "v" to 1f, "->" to 1f) // Missing up arrow for simplicity for now
        addRow(bottomRow)
    }

    
    private fun getKeyCode(label: String): Int {
        if (label.length == 1) {
            val char = label[0]
            if (char in 'A'..'Z') {
                return KeyEvent.KEYCODE_A + (char - 'A')
            }
        }
        return KeyEvent.KEYCODE_UNKNOWN
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        for (key in keys) {
            val isPressed = false // TODO: State tracking
            
            keyPaint.color = when (key.type) {
                KeyType.MODIFIER -> colorKeyModifier
                KeyType.ACCENT -> colorKeyAccent
                KeyType.DANGER -> colorKeyDanger
                else -> colorKeyDefault
            }
            
            val drawRect = if (isPressed) {
                val cx = key.rect.centerX()
                val cy = key.rect.centerY()
                val w = key.rect.width() * 0.92f / 2
                val h = key.rect.height() * 0.92f / 2
                RectF(cx - w, cy - h, cx + w, cy + h)
            } else {
                key.rect
            }
            
            val radius = if (key.label == "SPACE") 8f * resources.displayMetrics.density else 6f * resources.displayMetrics.density
            
            canvas.drawRoundRect(drawRect, radius, radius, keyPaint)
            canvas.drawRoundRect(drawRect, radius, radius, keyBorderPaint)
            
            textPaintPrimary.color = if (key.type == KeyType.MODIFIER) colorTextAccent else colorTextPrimary
            
            canvas.drawText(key.label, drawRect.centerX(), drawRect.centerY() + textPaintPrimary.textSize / 3, textPaintPrimary)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector?.onTouchEvent(event)
        
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y
            for (key in keys) {
                if (key.rect.contains(x, y)) {
                    handleKeyPress(key)
                    return true
                }
            }
        }
        return true
    }
    
    private fun handleKeyPress(key: Key) {
        if (key.code == KeyEvent.KEYCODE_SPACE || key.label == "SPACE") {
            dispatcher?.sendText(" ")
        } else if (key.code == KeyEvent.KEYCODE_DEL || key.code == KeyEvent.KEYCODE_ENTER) {
            dispatcher?.sendKey(key.code)
        } else if (key.code != KeyEvent.KEYCODE_UNKNOWN) {
            dispatcher?.sendKey(key.code)
        } else {
            dispatcher?.sendText(key.label)
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (width * 0.70f).toInt() // Standard keyboard aspect ratio approx
        setMeasuredDimension(width, height)
    }
    
    enum class KeyType { NORMAL, MODIFIER, ACCENT, DANGER }
    data class Key(val rect: RectF, val label: String, val secondaryLabel: String, val code: Int, val type: KeyType)
}
