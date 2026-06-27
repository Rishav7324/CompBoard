package com.example.theme

data class KeyboardTheme(
    val id: String,
    val name: String,
    val keyBg: Int,
    val keyModifierBg: Int,
    val keyActiveBg: Int,
    val keyAccentBg: Int,
    val keyDangerBg: Int,
    val keyboardBg: Int,
    val keyText: Int,
    val keySecondaryText: Int,
    val keyBorder: Int,
    val keyActiveGlow: Int,
    val cornerRadius: Float,
    val fontFamily: Int,
    val backgroundBlur: Boolean
)

class ThemeManager {
    companion object {
        val AMOLED_DARK = KeyboardTheme(
            id = "amoled_dark",
            name = "AMOLED Dark",
            keyBg = android.graphics.Color.parseColor("#1E1E1E"),
            keyModifierBg = android.graphics.Color.parseColor("#2A1F3D"),
            keyActiveBg = android.graphics.Color.parseColor("#5C3DDD"),
            keyAccentBg = android.graphics.Color.parseColor("#7B5CF6"),
            keyDangerBg = android.graphics.Color.parseColor("#C0392B"),
            keyboardBg = android.graphics.Color.parseColor("#0A0A0A"),
            keyText = android.graphics.Color.parseColor("#F5F5F5"),
            keySecondaryText = android.graphics.Color.parseColor("#888888"),
            keyBorder = android.graphics.Color.parseColor("#2A2A2A"),
            keyActiveGlow = android.graphics.Color.parseColor("#4D7B5CF6"),
            cornerRadius = 6f,
            fontFamily = 0,
            backgroundBlur = false
        )
        
        fun getTheme(id: String): KeyboardTheme {
            return AMOLED_DARK
        }
    }
}
