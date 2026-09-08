package com.easypocket.mobile.ui.theme

import androidx.compose.ui.graphics.Color

object StoreColors {
    val light = listOf(
        Color(0xFFA1A1AA), Color(0xFF2DD4BF), Color(0xFF4ADE80), Color(0xFF60A5FA),
        Color(0xFFA78BFA), Color(0xFFF472B6), Color(0xFFF87171), Color(0xFFFB923C),
        Color(0xFFFACC15),
    )
    val dark = listOf(
        Color(0xFF6B7280), Color(0xFF0D9488), Color(0xFF059669), Color(0xFF2563EB),
        Color(0xFF7C3AED), Color(0xFFBE185D), Color(0xFFDC2626), Color(0xFFEA580C),
        Color(0xFFCA8A04),
    )

    fun get(index: Int, isDark: Boolean): Color {
        val palette = if (isDark) dark else light
        return if (index in 0..palette.lastIndex) palette[index] else palette[0]
    }

    fun shade(color: Color, factor: Float): Color {
        val target = if (factor < 0) Color.Black else Color.White
        return lerp(color, target, kotlin.math.abs(factor))
    }

    private fun lerp(a: Color, b: Color, t: Float) = Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = 1f,
    )

    fun textColor(index: Int, isDark: Boolean): Color =
        shade(get(index, isDark), if (isDark) 0.75f else -0.55f)

    fun backgroundAlpha(index: Int, isDark: Boolean): Color =
        get(index, isDark).copy(alpha = if (isDark) 0.2f else 0.35f)
}
