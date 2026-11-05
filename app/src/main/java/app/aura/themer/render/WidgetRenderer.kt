package app.aura.themer.render

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.BatteryManager
import app.aura.themer.data.Fonts
import app.aura.themer.model.WidgetElement
import app.aura.themer.model.WidgetSpec
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object Battery {
    /** Returns (percent, isCharging). */
    fun read(ctx: Context): Pair<Int, Boolean> {
        val i = ctx.applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else 100
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return pct to charging
    }
}

object WidgetRenderer {

    class Env(val nowMs: Long, val t: Float, val battery: Int, val charging: Boolean, val font: String? = null)

    private val formats = HashMap<String, SimpleDateFormat>()

    private fun locale(): Locale {
        val l = Locale.getDefault()
        return if (l.language == "ar") Locale.forLanguageTag("ar-u-nu-latn") else l
    }

    @Synchronized
    private fun fmt(pattern: String, ms: Long): String {
        return try {
            val f = formats.getOrPut(pattern) { SimpleDateFormat(pattern, locale()) }
            f.format(Date(ms))
        } catch (e: Exception) {
            ""
        }
    }

    fun hasAnimation(spec: WidgetSpec): Boolean =
        spec.elements.any { it.anim != "none" || (it.kind == "ring" && it.text == "seconds") }

    private fun resolveText(e: WidgetElement, env: Env): String = when (e.kind) {
        "time" -> fmt(e.format ?: "HH:mm", env.nowMs)
        "date" -> fmt(e.format ?: "EEEE d MMMM", env.nowMs)
        "weekday" -> fmt(e.format ?: "EEEE", env.nowMs)
        "battery" -> "${env.battery}%" + if (env.charging) " ⚡" else ""
        else -> e.text
            .replace("{time}", fmt("HH:mm", env.nowMs))
            .replace("{date}", fmt("EEEE d MMMM", env.nowMs))
            .replace("{day}", fmt("EEEE", env.nowMs))
            .replace("{battery}", "${env.battery}%")
    }

    fun draw(c: Canvas, wPx: Float, hPx: Float, spec: WidgetSpec, env: Env, drawBackground: Boolean = true) {
        val sw = max(1, spec.w).toFloat()
        val sh = max(1, spec.h).toFloat()
        val s = min(wPx / sw, hPx / sh)
        val ww = sw * s
        val hh = sh * s
        c.save()
        c.translate((wPx - ww) / 2f, (hPx - hh) / 2f)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        if (drawBackground) {
            val r = spec.corner * s
            p.style = Paint.Style.FILL
            p.color = Col.parse(spec.background, Color.TRANSPARENT)
            c.drawRoundRect(0f, 0f, ww, hh, r, r, p)
            if (!spec.border.isNullOrBlank()) {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 1.5f * s
                p.color = Col.parse(spec.border, Color.TRANSPARENT)
                c.drawRoundRect(0.75f * s, 0.75f * s, ww - 0.75f * s, hh - 0.75f * s, r, r, p)
            }
        }
        for (e in spec.elements) drawElement(c, e, ww, hh, s, env, p)
        c.restore()
    }

    private fun drawElement(c: Canvas, e: WidgetElement, ww: Float, hh: Float, s: Float, env: Env, p: Paint) {
        val t = env.t
        var cy = e.y * hh
        val cx = e.x * ww
        var scale = 1f
        var rot = 0f
        var alpha = e.opacity.coerceIn(0f, 1f)
        var glow = 0f
        when (e.anim) {
            "pulse" -> scale = 1f + 0.06f * sin(t * 2f * PI.toFloat() * 0.8f)
            "float" -> cy += sin(t * 1.4f) * 6f * s
            "bounce" -> cy -= abs(sin(t * 2.2f)) * 10f * s
            "rotate" -> rot = (t * 40f) % 360f
            "blink" -> alpha *= 0.55f + 0.45f * sin(t * 3f)
            "glow" -> glow = (8f + 6f * sin(t * 2f)) * s
        }
        val color = Col.withAlpha(Col.parse(e.color, Color.WHITE), alpha)

        c.save()
        c.translate(cx, cy)
        if (rot != 0f) c.rotate(rot)
        if (scale != 1f) c.scale(scale, scale)

        p.reset()
        p.isAntiAlias = true
        p.color = color

        when (e.kind) {
            "rect" -> {
                val w = e.w * s
                val h = e.h * s
                val r = e.size * s
                p.style = Paint.Style.FILL
                c.drawRoundRect(-w / 2f, -h / 2f, w / 2f, h / 2f, r, r, p)
            }
            "circle" -> {
                p.style = Paint.Style.FILL
                c.drawCircle(0f, 0f, e.w * s / 2f, p)
            }
            "line" -> {
                p.style = Paint.Style.STROKE
                p.strokeCap = Paint.Cap.ROUND
                p.strokeWidth = max(1f, e.h) * s
                c.drawLine(-e.w * s / 2f, 0f, e.w * s / 2f, 0f, p)
            }
            "ring" -> drawRing(c, e, s, env, p, color)
            else -> drawText(c, e, s, env, p, color, glow)
        }
        c.restore()
    }

    private fun drawRing(c: Canvas, e: WidgetElement, s: Float, env: Env, p: Paint, color: Int) {
        val d = max(8f, e.w) * s
        val stroke = max(2f, e.size) * s
        val rect = RectF(-d / 2f + stroke / 2f, -d / 2f + stroke / 2f, d / 2f - stroke / 2f, d / 2f - stroke / 2f)
        val cal = Calendar.getInstance().apply { timeInMillis = env.nowMs }
        val sec = cal.get(Calendar.SECOND) + cal.get(Calendar.MILLISECOND) / 1000f
        val min = cal.get(Calendar.MINUTE) + sec / 60f
        val hour = (cal.get(Calendar.HOUR) + min / 60f)
        val frac = when (e.text) {
            "seconds" -> sec / 60f
            "minutes" -> min / 60f
            "hours" -> hour / 12f
            "day" -> (cal.get(Calendar.HOUR_OF_DAY) * 60f + cal.get(Calendar.MINUTE)) / 1440f
            else -> env.battery / 100f
        }.coerceIn(0f, 1f)
        p.style = Paint.Style.STROKE
        p.strokeWidth = stroke
        p.strokeCap = Paint.Cap.ROUND
        p.color = Col.withAlpha(color, 0.18f)
        c.drawArc(rect, 0f, 360f, false, p)
        p.color = color
        if (frac > 0f) c.drawArc(rect, -90f, 360f * frac, false, p)
    }

    private fun drawText(c: Canvas, e: WidgetElement, s: Float, env: Env, p: Paint, color: Int, glow: Float) {
        val str = resolveText(e, env)
        if (str.isEmpty()) return
        p.style = Paint.Style.FILL
        p.typeface = Fonts.get(e.font ?: env.font, e.bold)
        p.textSize = max(4f, e.size) * s
        p.color = color
        p.textAlign = when (e.align) {
            "left" -> Paint.Align.LEFT
            "right" -> Paint.Align.RIGHT
            else -> Paint.Align.CENTER
        }
        if (glow > 0f) p.setShadowLayer(glow, 0f, 0f, color)
        val fm = p.fontMetrics
        val lineH = (fm.descent - fm.ascent) * 1.05f
        val lines = str.split("\n")
        var y = -(fm.ascent + fm.descent) / 2f - (lines.size - 1) * lineH / 2f
        for (ln in lines) {
            c.drawText(ln, 0f, y, p)
            y += lineH
        }
    }
}
