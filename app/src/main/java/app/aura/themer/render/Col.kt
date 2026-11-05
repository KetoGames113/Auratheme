package app.aura.themer.render

import android.graphics.Color

object Col {
    private val rgbaRegex = Regex("""rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*(?:,\s*([\d.]+))?\s*\)""")

    fun parse(s: String?, fallback: Int = Color.WHITE): Int {
        if (s.isNullOrBlank()) return fallback
        val v = s.trim()
        return try {
            if (v.startsWith("#")) {
                var hex = v.substring(1)
                if (hex.length == 3 || hex.length == 4) {
                    hex = hex.map { "$it$it" }.joinToString("")
                }
                Color.parseColor("#$hex")
            } else if (v.startsWith("rgb")) {
                val m = rgbaRegex.matchEntire(v) ?: return fallback
                val r = m.groupValues[1].toInt().coerceIn(0, 255)
                val g = m.groupValues[2].toInt().coerceIn(0, 255)
                val b = m.groupValues[3].toInt().coerceIn(0, 255)
                val a = (m.groupValues[4].toFloatOrNull() ?: 1f).coerceIn(0f, 1f)
                Color.argb((a * 255).toInt(), r, g, b)
            } else {
                Color.parseColor(v)
            }
        } catch (e: Exception) {
            fallback
        }
    }

    fun withAlpha(color: Int, a: Float): Int {
        val na = (Color.alpha(color) * a).toInt().coerceIn(0, 255)
        return (color and 0x00FFFFFF) or (na shl 24)
    }

    /** ARGB hex, e.g. #FF7C5CFF (or #RRGGBB when fully opaque). */
    fun hex(color: Int): String =
        if (Color.alpha(color) == 255) String.format("#%06X", color and 0xFFFFFF)
        else String.format("#%08X", color)

    fun luminance(color: Int): Float =
        (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)) / 255f

    fun readableOn(color: Int): Int = if (luminance(color) > 0.6f) Color.BLACK else Color.WHITE

    fun darken(color: Int, f: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * f).coerceIn(0f, 1f)
        return Color.HSVToColor(Color.alpha(color), hsv)
    }
}
