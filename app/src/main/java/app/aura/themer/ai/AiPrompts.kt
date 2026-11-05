package app.aura.themer.ai

import app.aura.themer.data.Repo
import app.aura.themer.model.AuraTheme
import app.aura.themer.model.ScheduleSlot
import app.aura.themer.model.WallpaperSpec
import app.aura.themer.model.WidgetElement
import app.aura.themer.model.WidgetSpec
import kotlinx.serialization.json.JsonObject
import java.util.UUID

enum class AiMode { THEME, WIDGET, MODIFY }

object AiPrompts {

    private val SCHEMA = """
THEME JSON SCHEMA (all keys optional, use exactly these names):
{
  "name": "theme name",
  "author": "",
  "palette": { "primary": "#RRGGBB", "secondary": "#RRGGBB", "background": "#RRGGBB", "surface": "#RRGGBB", "text": "#RRGGBB", "accent": "#RRGGBB" },
  "wallpaper": { "type": "solid|gradient|aurora|particles|stars|waves|rain", "colors": ["#RRGGBB", "..."], "angle": 90, "speed": 1.0, "density": 1.0, "dim": 0.0 },
  "schedule": [ { "from": "18:00", "to": "06:00", "wallpaper": { same fields as wallpaper } } ],
  "icons": { "shape": "circle|squircle|square|teardrop|hexagon|none", "tint": null, "background": null, "scale": 1.0, "showLabels": true, "labelColor": null },
  "launcher": { "columns": 4 },
  "notifications": { "enabled": true, "style": "glass|solid|outline|minimal", "background": "#AARRGGBB", "titleColor": "#RRGGBB", "textColor": "#AARRGGBB", "accent": "#RRGGBB", "corner": 28, "hideOriginal": true },
  "widgets": [ WIDGET, WIDGET ]
}

WIDGET:
{ "name": "widget name", "w": 300, "h": 140, "x": 0.5, "y": 0.2, "background": "#AARRGGBB", "border": null, "corner": 28, "elements": [ ELEMENT, ... ] }

ELEMENT:
{ "kind": "time|date|weekday|battery|text|ring|rect|circle|line", "text": "", "format": null, "x": 0.5, "y": 0.5, "size": 24, "w": 0, "h": 0, "color": "#AARRGGBB", "align": "center|left|right", "bold": false, "anim": "none|pulse|float|bounce|rotate|blink|glow", "opacity": 1.0 }

RULES:
- Colors are "#RRGGBB" or "#AARRGGBB" (the alpha comes FIRST, e.g. "#80000000" = 50% black).
- wallpaper.colors per type: solid = [color]; gradient = 2-5 colors top-to-bottom (angle 90 = top to bottom); aurora = [dark base, glow colors...]; particles = [background, particle colors...]; stars = [top, bottom, star color]; waves = [top, bottom, wave colors...]; rain = [background, drop color].
- speed 0-3, density 0.3-3, dim 0-0.8. NEVER use image/gif/video types (you cannot provide files).
- Widget w/h are in dp (usually 120-360). Element x,y are FRACTIONS 0..1 of the widget, pointing at the element CENTER (for align=left/right it is the start/end edge). Elements are drawn in order (later on top).
- size: font size in dp for text-like kinds; corner radius for "rect"; stroke width for "ring".
- "ring": w = diameter, text = "battery" | "seconds" | "minutes" | "hours" | "day". "rect": w,h,size=corner. "circle": w = diameter. "line": w = length, h = thickness.
- "time" uses Java SimpleDateFormat in "format" (default "HH:mm"; examples: "hh", "mm", "h:mm a"). "date" default "EEEE d MMMM". "weekday" default "EEEE". "text" may contain {time} {date} {day} {battery} and \n for line breaks. Arabic text is fine.
- Widget x,y = where the widget sits on the home screen (center, fractions); keep widgets from overlapping, y between 0.1 and 0.65.
- Make the design cohesive and beautiful: harmonious colors, good contrast, tasteful animations (glow / pulse / float). Do not overuse animations.
- Do NOT output the keys: id, font, path, pack, overrides, dock.
""".trimIndent()

    private val EXAMPLE_WIDGET = """
{"name":"Neon Clock","w":300,"h":150,"x":0.5,"y":0.2,"background":"#66120A2E","border":"#7C5CFF","corner":34,"elements":[
 {"kind":"time","x":0.5,"y":0.42,"size":66,"color":"#00E5FF","bold":true,"anim":"glow"},
 {"kind":"date","x":0.5,"y":0.82,"size":16,"color":"#CCFFFFFF"}]}
""".trimIndent()

    private const val FOOT =
        "Reply with ONLY the JSON object inside a single code block. No explanations, no comments, no trailing commas."

    fun theme(request: String): String = """
You are a senior mobile UI designer creating a full phone THEME for the Android app "Aura".
The user request (may be Arabic):
"${request.trim().ifBlank { "اصنع ثيم جميل ومميز" }}"

Create a complete theme: palette, animated wallpaper, icon style, notification card style and 2-4 beautifully designed widgets.

$SCHEMA

$FOOT
""".trimIndent()

    fun widget(request: String): String = """
You are a senior mobile UI designer creating ONE home-screen WIDGET for the Android app "Aura".
The user request (may be Arabic):
"${request.trim().ifBlank { "ويدجت ساعة جميل" }}"

Return exactly ONE WIDGET object (not a full theme).

$SCHEMA

Example of a valid WIDGET:
$EXAMPLE_WIDGET

$FOOT
""".trimIndent()

    fun modify(request: String, currentThemeJson: String): String = """
You are a senior mobile UI designer editing an existing THEME for the Android app "Aura".
Change request (may be Arabic):
"${request.trim().ifBlank { "حسّن التصميم" }}"

Here is the CURRENT theme JSON. Apply the request and return the COMPLETE updated theme JSON (keep everything that was not asked to change):
$currentThemeJson

$SCHEMA

$FOOT
""".trimIndent()

    fun build(mode: AiMode, request: String, current: AuraTheme): String = when (mode) {
        AiMode.THEME -> theme(request)
        AiMode.WIDGET -> widget(request)
        AiMode.MODIFY -> modify(request, Repo.export(current.copy(font = null, widgets = current.widgets.map { it.copy(id = "x") })))
    }
}

sealed class ImportResult {
    data class ThemeResult(val theme: AuraTheme) : ImportResult()
    data class WidgetResult(val widget: WidgetSpec) : ImportResult()
    data class Failure(val message: String) : ImportResult()
}

object AiImport {

    fun extractJson(raw: String): String? {
        var s = raw.trim()
        val fence = Regex("```(?:json|JSON)?\\s*([\\s\\S]*?)```").find(s)
        if (fence != null) s = fence.groupValues[1].trim()
        val a = s.indexOf('{')
        val b = s.lastIndexOf('}')
        if (a < 0 || b <= a) return null
        return s.substring(a, b + 1)
    }

    private val allowedWallpapers = setOf("solid", "gradient", "aurora", "particles", "stars", "waves", "rain")

    private fun cleanWallpaper(w: WallpaperSpec): WallpaperSpec {
        val type = if (w.type in allowedWallpapers) w.type else "aurora"
        val colors = w.colors.filter { it.isNotBlank() }.take(8).ifEmpty { listOf("#07070F", "#5B2BFF", "#00E5FF") }
        return w.copy(
            type = type,
            colors = colors,
            speed = w.speed.coerceIn(0f, 3f),
            density = w.density.coerceIn(0.3f, 3f),
            dim = w.dim.coerceIn(0f, 0.8f),
            path = null
        )
    }

    private fun cleanElement(e: WidgetElement): WidgetElement = e.copy(
        x = e.x.coerceIn(-0.2f, 1.2f),
        y = e.y.coerceIn(-0.2f, 1.2f),
        size = e.size.coerceIn(1f, 300f),
        w = e.w.coerceIn(0f, 600f),
        h = e.h.coerceIn(0f, 600f),
        opacity = e.opacity.coerceIn(0f, 1f),
        font = null
    )

    fun cleanWidget(w: WidgetSpec): WidgetSpec = w.copy(
        id = UUID.randomUUID().toString(),
        w = w.w.coerceIn(60, 500),
        h = w.h.coerceIn(40, 500),
        x = w.x.coerceIn(0f, 1f),
        y = w.y.coerceIn(0f, 1f),
        corner = w.corner.coerceIn(0f, 150f),
        elements = w.elements.take(40).map { cleanElement(it) }
    )

    private fun cleanTheme(t: AuraTheme): AuraTheme = t.copy(
        id = UUID.randomUUID().toString(),
        name = t.name.ifBlank { "ثيم من الذكاء الاصطناعي" }.take(40),
        wallpaper = cleanWallpaper(t.wallpaper),
        schedule = t.schedule.take(6).map { s: ScheduleSlot -> s.copy(wallpaper = cleanWallpaper(s.wallpaper)) },
        icons = t.icons.copy(pack = null, overrides = emptyMap(), scale = t.icons.scale.coerceIn(0.6f, 1.5f)),
        launcher = t.launcher.copy(columns = t.launcher.columns.coerceIn(3, 6), dock = emptyList()),
        notifications = t.notifications.copy(corner = t.notifications.corner.coerceIn(0f, 60f)),
        font = null,
        widgets = t.widgets.take(12).map { cleanWidget(it) }
    )

    fun parse(raw: String): ImportResult {
        val body = extractJson(raw)
            ?: return ImportResult.Failure("ما لقيت JSON صالح في النص. تأكد إنك نسخت رد الذكاء الاصطناعي كامل.")
        return try {
            val el = Repo.json.parseToJsonElement(body)
            val obj = el as? JsonObject
                ?: return ImportResult.Failure("الملف لازم يكون كائن JSON.")
            val isWidget = obj.containsKey("elements") && !obj.containsKey("wallpaper") && !obj.containsKey("palette")
            if (isWidget) {
                val w = Repo.json.decodeFromString(WidgetSpec.serializer(), body)
                ImportResult.WidgetResult(cleanWidget(w))
            } else {
                val t = Repo.json.decodeFromString(AuraTheme.serializer(), body)
                ImportResult.ThemeResult(cleanTheme(t))
            }
        } catch (e: Exception) {
            ImportResult.Failure("تعذّرت قراءة الـ JSON: ${e.message?.take(140) ?: "خطأ غير معروف"}")
        }
    }
}
