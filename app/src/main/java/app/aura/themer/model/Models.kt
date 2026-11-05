package app.aura.themer.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Palette(
    val primary: String = "#7C5CFF",
    val secondary: String = "#00E5FF",
    val background: String = "#0B0B14",
    val surface: String = "#161625",
    val text: String = "#FFFFFF",
    val accent: String = "#FF4DA6"
)

/** type: solid, gradient, aurora, particles, stars, waves, rain, image, gif, video */
@Serializable
data class WallpaperSpec(
    val type: String = "aurora",
    val colors: List<String> = listOf("#07070F", "#5B2BFF", "#00E5FF", "#FF2E93"),
    val angle: Float = 90f,
    val speed: Float = 1f,
    val density: Float = 1f,
    val path: String? = null,
    val dim: Float = 0f
)

@Serializable
data class ScheduleSlot(
    val from: String = "18:00",
    val to: String = "06:00",
    val wallpaper: WallpaperSpec = WallpaperSpec(
        type = "stars",
        colors = listOf("#05060F", "#101842", "#FFFFFF"),
        density = 1f
    )
)

/** shape: circle, squircle, square, teardrop, hexagon, none */
@Serializable
data class IconStyle(
    val shape: String = "squircle",
    val tint: String? = null,
    val background: String? = null,
    val scale: Float = 1f,
    val showLabels: Boolean = true,
    val labelColor: String? = null,
    val pack: String? = null,
    val overrides: Map<String, String> = emptyMap()
)

@Serializable
data class LauncherSpec(
    val columns: Int = 4,
    val dock: List<String> = emptyList()
)

/** style: glass, solid, outline, minimal */
@Serializable
data class NotifStyle(
    val enabled: Boolean = false,
    val style: String = "glass",
    val background: String = "#CC161625",
    val titleColor: String = "#FFFFFF",
    val textColor: String = "#CCFFFFFF",
    val accent: String = "#7C5CFF",
    val corner: Float = 28f,
    val hideOriginal: Boolean = true
)

/**
 * kind: time, date, weekday, battery, text, ring, rect, circle, line
 * anim: none, pulse, float, bounce, rotate, blink, glow
 */
@Serializable
data class WidgetElement(
    val kind: String = "text",
    val text: String = "",
    val format: String? = null,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val size: Float = 24f,
    val w: Float = 0f,
    val h: Float = 0f,
    val color: String = "#FFFFFF",
    val font: String? = null,
    val align: String = "center",
    val bold: Boolean = false,
    val anim: String = "none",
    val opacity: Float = 1f
)

@Serializable
data class WidgetSpec(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "ويدجت",
    val w: Int = 300,
    val h: Int = 140,
    val x: Float = 0.5f,
    val y: Float = 0.25f,
    val background: String = "#99161625",
    val border: String? = null,
    val corner: Float = 28f,
    val elements: List<WidgetElement> = emptyList()
)

@Serializable
data class AuraTheme(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "ثيم جديد",
    val author: String = "",
    val palette: Palette = Palette(),
    val wallpaper: WallpaperSpec = WallpaperSpec(),
    val schedule: List<ScheduleSlot> = emptyList(),
    val icons: IconStyle = IconStyle(),
    val launcher: LauncherSpec = LauncherSpec(),
    val notifications: NotifStyle = NotifStyle(),
    val font: String? = null,
    val widgets: List<WidgetSpec> = emptyList()
)

object ThemeSchedule {
    fun toMin(hhmm: String): Int {
        val p = hhmm.split(":")
        val h = p.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
        val m = p.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        return (h * 60 + m).coerceIn(0, 24 * 60)
    }

    fun nowMinutes(): Int {
        val c = java.util.Calendar.getInstance()
        return c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE)
    }

    fun resolve(theme: AuraTheme, nowMin: Int = nowMinutes()): WallpaperSpec {
        for (s in theme.schedule) {
            val a = toMin(s.from)
            val b = toMin(s.to)
            val inRange = if (a <= b) nowMin in a until b else (nowMin >= a || nowMin < b)
            if (inRange) return s.wallpaper
        }
        return theme.wallpaper
    }
}
