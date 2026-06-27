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
    private val activePointers = mutableMapOf<Int, Key?>()
    
    // Repeat Engine
    private val repeatHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val repeatingKeys = mutableMapOf<Int, Runnable>()
    private val REPEAT_DELAY_MS = 400L
    private val REPEAT_INTERVAL_MS = 50L
    
    // Theme Colors
    private val colorBackground = Color.parseColor("#050505")
    private val colorSurface = Color.parseColor("#050505")
    private val colorKeyDefault = Color.parseColor("#1A1A1A")
    private val colorKeyModifier = Color.parseColor("#262626")
    private val colorKeyActive = Color.parseColor("#5C3DDD") 
    private val colorKeyAccent = Color.parseColor("#5C3DDD")
    private val colorKeyDanger = Color.parseColor("#DC2626")
    private val colorTextPrimary = Color.parseColor("#F5F5F5")
    private val colorTextSecondary = Color.parseColor("#737373")
    private val colorBorder = Color.parseColor("#2A2A2A")
    private val colorTextAccent = Color.parseColor("#FFFFFF")
    private val colorBorderAccent = Color.parseColor("#5C3DDD")
    private val colorBorderDanger = Color.parseColor("#B91C1C")
    private val colorTextDanger = Color.parseColor("#FFFFFF")

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
        textSize = 18f * android.content.res.Resources.getSystem().displayMetrics.density
    }
    private val textPaintSecondary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorTextSecondary
        textAlign = Paint.Align.RIGHT
        typeface = android.graphics.Typeface.MONOSPACE
        textSize = 12f * android.content.res.Resources.getSystem().displayMetrics.density
    }
    
    init {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        isFocusable = false
        isFocusableInTouchMode = false
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
        keyMargin = w * 0.01f // ~4dp gap
        
        textPaintPrimary.textSize = w * 0.038f // Slightly larger text
        textPaintSecondary.textSize = w * 0.026f
        
        generateLayout()
    }
    
    private fun generateLayout() {
        keys.clear()
        val numCols = 15f
        val baseWidth = (keyboardWidth - keyMargin * (numCols + 1)) / numCols
        val rowHeights = listOf(
            keyboardHeight * 0.12f, // F-Row
            keyboardHeight * 0.15f, // Num Row
            keyboardHeight * 0.15f, // QWERTY
            keyboardHeight * 0.16f, // Home
            keyboardHeight * 0.15f, // Shift
            keyboardHeight * 0.17f  // Bottom
        )
        
        var currentY = 0f
        
        // Skip some height for the compose top bar overlay so we don't draw under it
        currentY += keyboardHeight * 0.10f
        
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
            val keyType = if (label == "Esc") KeyType.DANGER else KeyType.NORMAL
            keys.add(Key(RectF(currentX, currentY, currentX + fKeyWidth, currentY + rowHeights[0] - keyMargin), label, "", code, keyType))
            currentX += fKeyWidth + keyMargin
        }
        currentY += rowHeights[0]
        
        // Helper to add rows
        fun addRow(rowDef: List<Triple<String, String, Float>>, heightIndex: Int) {
            currentX = keyMargin
            for ((label, secLabel, weight) in rowDef) {
                val keyW = baseWidth * weight + keyMargin * (weight - 1)
                val code = getKeyCode(label)
                val type = when {
                    label in listOf("Tab", "Caps", "Shift", "Ctrl", "Win", "Alt", "Fn", "AltGr") -> KeyType.MODIFIER
                    label == "Enter" || label == "⌫" || label == "Bksp" -> KeyType.ACCENT
                    else -> KeyType.NORMAL
                }
                keys.add(Key(RectF(currentX, currentY, currentX + keyW, currentY + rowHeights[heightIndex] - keyMargin), label, secLabel, code, type))
                currentX += keyW + keyMargin
            }
            currentY += rowHeights[heightIndex]
        }
        
        // Row 1: Numbers
        val numRow = listOf(
            Triple("`", "~", 1f), Triple("1", "!", 1f), Triple("2", "@", 1f), Triple("3", "#", 1f), 
            Triple("4", "$", 1f), Triple("5", "%", 1f), Triple("6", "^", 1f), Triple("7", "&", 1f), 
            Triple("8", "*", 1f), Triple("9", "(", 1f), Triple("0", ")", 1f), Triple("-", "_", 1f), 
            Triple("=", "+", 1f), Triple("⌫", "", 2f)
        )
        addRow(numRow, 1)
        
        // Row 2: QWERTY
        val qwertyRow = listOf(
            Triple("Tab", "", 1.5f), Triple("Q", "", 1f), Triple("W", "", 1f), Triple("E", "", 1f), 
            Triple("R", "", 1f), Triple("T", "", 1f), Triple("Y", "", 1f), Triple("U", "", 1f), 
            Triple("I", "", 1f), Triple("O", "", 1f), Triple("P", "", 1f), Triple("[", "{", 1f), 
            Triple("]", "}", 1f), Triple("\\", "|", 1.5f)
        )
        addRow(qwertyRow, 2)
        
        // Row 3: Home
        val homeRow = listOf(
            Triple("Caps", "", 1.8f), Triple("A", "", 1f), Triple("S", "", 1f), Triple("D", "", 1f), 
            Triple("F", "", 1f), Triple("G", "", 1f), Triple("H", "", 1f), Triple("J", "", 1f), 
            Triple("K", "", 1f), Triple("L", "", 1f), Triple(";", ":", 1f), Triple("'", "\"", 1f), 
            Triple("Enter", "", 2.2f)
        )
        addRow(homeRow, 3)
        
        // Row 4: Shift
        val shiftRow = listOf(
            Triple("Shift", "", 2.3f), Triple("Z", "", 1f), Triple("X", "", 1f), Triple("C", "", 1f), 
            Triple("V", "", 1f), Triple("B", "", 1f), Triple("N", "", 1f), Triple("M", "", 1f), 
            Triple(",", "<", 1f), Triple(".", ">", 1f), Triple("/", "?", 1f), Triple("Shift", "", 2.7f)
        )
        addRow(shiftRow, 4)
        
        // Row 5: Bottom
        val bottomRow = listOf(
            Triple("Ctrl", "", 1.5f), Triple("Win", "", 1.2f), Triple("Alt", "", 1.2f), 
            Triple("EN", "", 6.1f), Triple("AltGr", "", 1.2f), Triple("Fn", "", 1.2f), 
            Triple("Ctrl", "", 1.2f)
        )
        // Add arrow keys manually since they have a custom layout
        currentX = keyMargin
        for ((label, secLabel, weight) in bottomRow) {
            val keyW = baseWidth * weight + keyMargin * (weight - 1)
            val code = getKeyCode(label)
            val type = if (label == "EN") KeyType.NORMAL else KeyType.MODIFIER
            keys.add(Key(RectF(currentX, currentY, currentX + keyW, currentY + rowHeights[5] - keyMargin), label, secLabel, code, type))
            currentX += keyW + keyMargin
        }
        
        // Arrow Keys (Up, Down, Left, Right)
        val arrW = (keyboardWidth - currentX - keyMargin) / 3f - keyMargin
        val arrH = (rowHeights[5] - keyMargin * 2) / 2f
        val startX = currentX
        
        keys.add(Key(RectF(startX, currentY + arrH + keyMargin, startX + arrW, currentY + rowHeights[5] - keyMargin), "◀", "", KeyEvent.KEYCODE_DPAD_LEFT, KeyType.NORMAL))
        keys.add(Key(RectF(startX + arrW + keyMargin, currentY, startX + arrW * 2 + keyMargin, currentY + arrH), "▲", "", KeyEvent.KEYCODE_DPAD_UP, KeyType.NORMAL))
        keys.add(Key(RectF(startX + arrW + keyMargin, currentY + arrH + keyMargin, startX + arrW * 2 + keyMargin, currentY + rowHeights[5] - keyMargin), "▼", "", KeyEvent.KEYCODE_DPAD_DOWN, KeyType.NORMAL))
        keys.add(Key(RectF(startX + arrW * 2 + keyMargin * 2, currentY + arrH + keyMargin, startX + arrW * 3 + keyMargin * 2, currentY + rowHeights[5] - keyMargin), "▶", "", KeyEvent.KEYCODE_DPAD_RIGHT, KeyType.NORMAL))
    }

    
    private fun getKeyCode(label: String): Int {
        if (label.length == 1) {
            val char = label[0]
            if (char in 'A'..'Z') {
                return KeyEvent.KEYCODE_A + (char - 'A')
            }
            if (char in '0'..'9') {
                return KeyEvent.KEYCODE_0 + (char - '0')
            }
            return when (char) {
                '`' -> KeyEvent.KEYCODE_GRAVE
                '-' -> KeyEvent.KEYCODE_MINUS
                '=' -> KeyEvent.KEYCODE_EQUALS
                '[' -> KeyEvent.KEYCODE_LEFT_BRACKET
                ']' -> KeyEvent.KEYCODE_RIGHT_BRACKET
                '\\' -> KeyEvent.KEYCODE_BACKSLASH
                ';' -> KeyEvent.KEYCODE_SEMICOLON
                '\'' -> KeyEvent.KEYCODE_APOSTROPHE
                ',' -> KeyEvent.KEYCODE_COMMA
                '.' -> KeyEvent.KEYCODE_PERIOD
                '/' -> KeyEvent.KEYCODE_SLASH
                else -> KeyEvent.KEYCODE_UNKNOWN
            }
        }
        return when (label) {
            "⌫", "Bksp" -> KeyEvent.KEYCODE_DEL
            "Enter" -> KeyEvent.KEYCODE_ENTER
            "EN", "SPACE" -> KeyEvent.KEYCODE_SPACE
            "Tab" -> KeyEvent.KEYCODE_TAB
            "Shift" -> KeyEvent.KEYCODE_SHIFT_LEFT
            "Ctrl" -> KeyEvent.KEYCODE_CTRL_LEFT
            "Alt", "AltGr" -> KeyEvent.KEYCODE_ALT_LEFT
            "Win" -> KeyEvent.KEYCODE_META_LEFT
            "Caps" -> KeyEvent.KEYCODE_CAPS_LOCK
            "Esc" -> KeyEvent.KEYCODE_ESCAPE
            "◀" -> KeyEvent.KEYCODE_DPAD_LEFT
            "▲" -> KeyEvent.KEYCODE_DPAD_UP
            "▼" -> KeyEvent.KEYCODE_DPAD_DOWN
            "▶" -> KeyEvent.KEYCODE_DPAD_RIGHT
            else -> KeyEvent.KEYCODE_UNKNOWN
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        for (key in keys) {
            val isPressed = activePointers.values.contains(key)
            val isToggled = when (key.code) {
                KeyEvent.KEYCODE_SHIFT_LEFT -> ModifierState.shiftPressed || ModifierState.capsLockEnabled
                KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> ModifierState.ctrlPressed
                KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> ModifierState.altPressed
                KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT -> ModifierState.metaPressed
                else -> false
            }
            
            keyPaint.color = when (key.type) {
                KeyType.MODIFIER -> if (isToggled) colorKeyActive else colorKeyModifier
                KeyType.ACCENT -> colorKeyAccent
                KeyType.DANGER -> colorKeyDanger
                else -> colorKeyDefault
            }
            
            val radius = 8f * resources.displayMetrics.density
            
            val drawRect = if (isPressed) {
                val cx = key.rect.centerX()
                val cy = key.rect.centerY()
                val w = key.rect.width() * 0.94f / 2
                val h = key.rect.height() * 0.94f / 2
                // Slightly offset down when pressed
                RectF(cx - w, cy - h + 2f * resources.displayMetrics.density, cx + w, cy + h + 2f * resources.displayMetrics.density)
            } else {
                key.rect
            }

            // Draw shadow for 3D effect if not pressed
            if (!isPressed) {
                val shadowRect = RectF(drawRect.left, drawRect.top + 3f * resources.displayMetrics.density, drawRect.right, drawRect.bottom + 3f * resources.displayMetrics.density)
                val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
                    color = Color.parseColor("#000000")
                }
                canvas.drawRoundRect(shadowRect, radius, radius, shadowPaint)
            }
            
            canvas.drawRoundRect(drawRect, radius, radius, keyPaint)
            
            keyBorderPaint.color = when (key.type) {
                KeyType.ACCENT -> colorBorderAccent
                KeyType.DANGER -> colorBorderDanger
                KeyType.MODIFIER -> if (isToggled) colorKeyActive else colorBorder
                else -> colorBorder
            }
            canvas.drawRoundRect(drawRect, radius, radius, keyBorderPaint)
            
            if (isToggled) {
                // Draw a white line indicator at the bottom of toggled keys
                val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
                val lineH = 3f * resources.displayMetrics.density
                val lineW = drawRect.width() * 0.5f
                val lineRect = RectF(drawRect.centerX() - lineW/2, drawRect.bottom - lineH - 6f * resources.displayMetrics.density, drawRect.centerX() + lineW/2, drawRect.bottom - 6f * resources.displayMetrics.density)
                canvas.drawRoundRect(lineRect, lineH/2, lineH/2, indicatorPaint)
            } else if (key.label == "Caps" && ModifierState.capsLockEnabled) {
                // Dot indicator
                val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#34D399"); style = Paint.Style.FILL }
                canvas.drawCircle(drawRect.right - 10f * resources.displayMetrics.density, drawRect.top + 10f * resources.displayMetrics.density, 4f * resources.displayMetrics.density, indicatorPaint)
            }
            
            textPaintPrimary.color = when (key.type) {
                KeyType.MODIFIER -> if (isToggled) Color.WHITE else colorTextAccent
                KeyType.ACCENT -> if (key.label == "⌫") colorTextDanger else colorTextAccent
                KeyType.DANGER -> colorTextPrimary
                else -> colorTextPrimary
            }
            
            if (key.secondaryLabel.isNotEmpty()) {
                // If there's a secondary label, we usually put both stacked or side by side
                // The design shows the secondary label small in the top-left, primary centered or slightly offset
                textPaintPrimary.textAlign = Paint.Align.CENTER
                canvas.drawText(key.label, drawRect.centerX(), drawRect.centerY() + textPaintPrimary.textSize, textPaintPrimary)
                
                textPaintSecondary.textAlign = Paint.Align.LEFT
                canvas.drawText(key.secondaryLabel, drawRect.left + 8f * resources.displayMetrics.density, drawRect.top + 16f * resources.displayMetrics.density, textPaintSecondary)
            } else {
                textPaintPrimary.textAlign = Paint.Align.CENTER
                canvas.drawText(key.label, drawRect.centerX(), drawRect.centerY() + textPaintPrimary.textSize / 3, textPaintPrimary)
            }
        }
        
        // Draw popups for active keys on top
        for (key in activePointers.values) {
            if (key == null || key.type == KeyType.MODIFIER || key.label.length > 1) continue
            
            val cx = key.rect.centerX()
            val cy = key.rect.centerY()
            val pw = key.rect.width() * 1.5f
            val ph = key.rect.height() * 1.8f
            val popupRect = RectF(cx - pw/2, cy - ph, cx + pw/2, cy)
            
            val radius = 12f * resources.displayMetrics.density
            
            // Popup shadow
            val shadowRect = RectF(popupRect.left, popupRect.top + 4f * resources.displayMetrics.density, popupRect.right, popupRect.bottom + 4f * resources.displayMetrics.density)
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#40000000") }
            canvas.drawRoundRect(shadowRect, radius, radius, shadowPaint)
            
            // Popup body
            val popupPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorKeyActive }
            canvas.drawRoundRect(popupRect, radius, radius, popupPaint)
            
            // Draw connecting shape to key
            val path = android.graphics.Path()
            path.moveTo(cx - key.rect.width()/2, cy)
            path.lineTo(cx - pw/2 + radius, cy)
            path.lineTo(cx - pw/2, cy - radius)
            path.lineTo(cx + pw/2, cy - radius)
            path.lineTo(cx + pw/2 - radius, cy)
            path.lineTo(cx + key.rect.width()/2, cy)
            path.close()
            // Just a simple overlay
            
            // Popup text
            val isShifted = ModifierState.shiftPressed || ModifierState.capsLockEnabled
            val displayChar = if (key.label.length == 1 && key.label[0] in 'A'..'Z') {
                if (isShifted) key.label.uppercase() else key.label.lowercase()
            } else if (isShifted && key.secondaryLabel.isNotEmpty()) {
                key.secondaryLabel
            } else {
                key.label
            }
            
            val popupTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = textPaintPrimary.textSize * 1.8f
                textAlign = Paint.Align.CENTER
                typeface = textPaintPrimary.typeface
            }
            canvas.drawText(displayChar, popupRect.centerX(), popupRect.centerY() + popupTextPaint.textSize/3, popupTextPaint)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector?.onTouchEvent(event)
        
        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)

        when (action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                val key = findKeyAt(x, y)
                if (key != null) {
                    activePointers[pointerId] = key
                    handleKeyDown(key)
                    invalidateKey(key)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pId = event.getPointerId(i)
                    val px = event.getX(i)
                    val py = event.getY(i)
                    val currentKey = activePointers[pId]
                    val newKey = findKeyAt(px, py)
                    
                    if (currentKey != newKey) {
                        currentKey?.let { 
                            handleKeyUp(it)
                            invalidateKey(it) 
                        }
                        if (newKey != null) {
                            activePointers[pId] = newKey
                            handleKeyDown(newKey)
                            invalidateKey(newKey)
                        } else {
                            activePointers.remove(pId)
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
                val key = activePointers.remove(pointerId)
                key?.let { 
                    handleKeyUp(it)
                    invalidateKey(it) 
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                for (key in activePointers.values) {
                    key?.let { 
                        handleKeyUp(it)
                        invalidateKey(it) 
                    }
                }
                activePointers.clear()
            }
        }
        return true
    }
    
    @Suppress("DEPRECATION")
    private fun invalidateKey(key: Key) {
        val r = key.rect
        invalidate((r.left - 2).toInt(), (r.top - 2).toInt(), (r.right + 2).toInt(), (r.bottom + 2).toInt())
    }
    
    private fun findKeyAt(x: Float, y: Float): Key? {
        for (key in keys) {
            if (key.rect.contains(x, y)) {
                return key
            }
        }
        return null
    }
    
    private fun handleKeyDown(key: Key) {
        performCustomHapticFeedback()
        // Start repeat engine
        val runnable = object : Runnable {
            override fun run() {
                repeatKey(key)
                repeatHandler.postDelayed(this, REPEAT_INTERVAL_MS)
            }
        }
        repeatingKeys[key.code]?.let { repeatHandler.removeCallbacks(it) }
        if (key.type != KeyType.MODIFIER) {
            repeatingKeys[key.code] = runnable
            repeatHandler.postDelayed(runnable, REPEAT_DELAY_MS)
        }

        when {
            key.label == "SPACE" || key.label == "EN" -> dispatcher?.sendText(" ")
            key.code == KeyEvent.KEYCODE_DEL || key.code == KeyEvent.KEYCODE_ENTER || 
            key.code == KeyEvent.KEYCODE_DPAD_LEFT || key.code == KeyEvent.KEYCODE_DPAD_RIGHT || 
            key.code == KeyEvent.KEYCODE_DPAD_UP || key.code == KeyEvent.KEYCODE_DPAD_DOWN ||
            key.code == KeyEvent.KEYCODE_ESCAPE || key.code == KeyEvent.KEYCODE_TAB -> {
                dispatcher?.sendKeyDown(key.code)
            }
            key.type == KeyType.MODIFIER -> {
                dispatcher?.sendKeyDown(key.code)
            }
            else -> {
                dispatcher?.sendKeyDown(key.code)
            }
        }
    }
    
    private fun repeatKey(key: Key) {
        when {
            key.label == "SPACE" || key.label == "EN" -> dispatcher?.sendText(" ")
            key.code == KeyEvent.KEYCODE_DEL || key.code == KeyEvent.KEYCODE_ENTER || 
            key.code == KeyEvent.KEYCODE_DPAD_LEFT || key.code == KeyEvent.KEYCODE_DPAD_RIGHT || 
            key.code == KeyEvent.KEYCODE_DPAD_UP || key.code == KeyEvent.KEYCODE_DPAD_DOWN ||
            key.code == KeyEvent.KEYCODE_ESCAPE || key.code == KeyEvent.KEYCODE_TAB -> {
                dispatcher?.sendKeyDown(key.code)
                dispatcher?.sendKeyUp(key.code)
            }
            else -> {
                dispatcher?.sendKeyDown(key.code)
                dispatcher?.sendKeyUp(key.code)
                val noModifiers = ModifierState.getModifierMask() == 0
                if (key.code == KeyEvent.KEYCODE_UNKNOWN || (noModifiers && key.label.length == 1)) {
                    val isShifted = ModifierState.shiftPressed || ModifierState.capsLockEnabled
                    val textToCommit = if (key.label.length == 1) {
                        if (isShifted) key.label.uppercase() else key.label.lowercase()
                    } else {
                        key.label
                    }
                    dispatcher?.sendText(textToCommit)
                }
            }
        }
    }

    private fun handleKeyUp(key: Key) {
        repeatingKeys[key.code]?.let { repeatHandler.removeCallbacks(it) }
        repeatingKeys.remove(key.code)

        when {
            key.code == KeyEvent.KEYCODE_DEL || key.code == KeyEvent.KEYCODE_ENTER || 
            key.code == KeyEvent.KEYCODE_DPAD_LEFT || key.code == KeyEvent.KEYCODE_DPAD_RIGHT || 
            key.code == KeyEvent.KEYCODE_DPAD_UP || key.code == KeyEvent.KEYCODE_DPAD_DOWN ||
            key.code == KeyEvent.KEYCODE_ESCAPE || key.code == KeyEvent.KEYCODE_TAB -> {
                dispatcher?.sendKeyUp(key.code)
            }
            key.type == KeyType.MODIFIER -> {
                dispatcher?.sendKeyUp(key.code)
            }
            key.label != "SPACE" && key.label != "EN" -> {
                dispatcher?.sendKeyUp(key.code)
                // If it's a character key and no modifiers are active, we should commit text explicitly
                val noModifiers = ModifierState.ctrlPressed == false && ModifierState.altPressed == false && ModifierState.metaPressed == false
                if (key.code == KeyEvent.KEYCODE_UNKNOWN || (noModifiers && key.label.length == 1)) {
                    val isShifted = ModifierState.shiftPressed || ModifierState.capsLockEnabled
                    
                    val textToCommit = if (key.label.length == 1) {
                        val char = key.label[0]
                        if (char in 'A'..'Z') {
                            if (isShifted) char.uppercaseChar().toString() else char.lowercaseChar().toString()
                        } else {
                            if (ModifierState.shiftPressed) {
                                val shiftMap = mapOf(
                                    '`' to '~', '1' to '!', '2' to '@', '3' to '#', '4' to '$', '5' to '%',
                                    '6' to '^', '7' to '&', '8' to '*', '9' to '(', '0' to ')',
                                    '-' to '_', '=' to '+', '[' to '{', ']' to '}', '\\' to '|',
                                    ';' to ':', '\'' to '"', ',' to '<', '.' to '>', '/' to '?'
                                )
                                shiftMap[char]?.toString() ?: char.toString()
                            } else {
                                char.toString()
                            }
                        }
                    } else {
                        key.label
                    }
                    
                    dispatcher?.sendText(textToCommit)
                    
                    if (ModifierState.shiftPressed && !ModifierState.capsLockEnabled) {
                        ModifierState.shiftPressed = false
                        invalidate()
                    }
                }
            }
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = MeasureSpec.getSize(widthMeasureSpec)
        if (width == 0) {
            width = context.resources.displayMetrics.widthPixels
        }
        val height = (width * 0.70f).toInt()
        setMeasuredDimension(width, height)
    }

    private fun performCustomHapticFeedback() {
        val prefs = context.getSharedPreferences("haptics_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("haptics_enabled", true)) return
        
        val durationMs = prefs.getFloat("haptics_duration", 20f).toLong()
        val intensity = prefs.getFloat("haptics_intensity", 0.5f)
        
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        if (vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // intensity is 0..1, convert to 1..255
                val amplitude = (intensity * 255).toInt().coerceIn(1, 255)
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    }
    
    enum class KeyType { NORMAL, MODIFIER, ACCENT, DANGER }
    data class Key(val rect: RectF, val label: String, val secondaryLabel: String, val code: Int, val type: KeyType)
}
