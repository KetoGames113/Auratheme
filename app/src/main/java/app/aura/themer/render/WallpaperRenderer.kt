package app.aura.themer.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.MediaMetadataRetriever
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.graphics.ImageDecoder
import android.os.Build
import android.util.LruCache
import app.aura.themer.model.WallpaperSpec
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object MediaCache {
    private val bitmaps = LruCache<String, Bitmap>(4)
    private val thumbs = LruCache<String, Bitmap>(4)
    private var gifPath: String? = null
    private var gif: Drawable? = null

    @Synchronized
    fun bitmap(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        val f = File(path)
        if (!f.exists()) return null
        val key = path + f.lastModified()
        bitmaps.get(key)?.let { return it }
        return try {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, o)
            var sample = 1
            while (max(o.outWidth, o.outHeight) / (sample * 2) >= 1600) sample *= 2
            val o2 = BitmapFactory.Options().apply { inSampleSize = sample }
            val b = BitmapFactory.decodeFile(path, o2)
            if (b != null) bitmaps.put(key, b)
            b
        } catch (e: Throwable) {
            null
        }
    }

    @Synchronized
    fun videoThumb(path: String?): Bitmap? {
        if (path.isNullOrBlank() || !File(path).exists()) return null
        thumbs.get(path)?.let { return it }
        return try {
            val r = MediaMetadataRetriever()
            r.setDataSource(path)
            val b = r.getFrameAtTime(0)
            r.release()
            if (b != null) thumbs.put(path, b)
            b
        } catch (e: Throwable) {
            null
        }
    }

    @Synchronized
    fun gif(path: String?): Drawable? {
        if (path.isNullOrBlank() || Build.VERSION.SDK_INT < 28) return null
        if (path == gifPath && gif != null) return gif
        val f = File(path)
        if (!f.exists()) return null
        return try {
            val d = ImageDecoder.decodeDrawable(ImageDecoder.createSource(f))
            if (d is AnimatedImageDrawable) {
                d.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                d.start()
            }
            gifPath = path
            gif = d
            d
        } catch (e: Throwable) {
            null
        }
    }
}

object WallpaperRenderer {

    fun isAnimated(spec: WallpaperSpec): Boolean = when (spec.type) {
        "solid", "image", "video" -> false
        "gradient" -> spec.speed > 0f
        else -> true
    }

    private fun hash(n: Int): Float {
        val x = sin(n * 12.9898) * 43758.5453
        return (x - floor(x)).toFloat()
    }

    fun draw(c: Canvas, w: Int, h: Int, spec: WallpaperSpec, t: Float) {
        if (w <= 0 || h <= 0) return
        val colors = spec.colors.map { Col.parse(it, Color.BLACK) }.ifEmpty { listOf(Color.BLACK) }
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val wf = w.toFloat()
        val hf = h.toFloat()
        when (spec.type) {
            "solid" -> c.drawColor(colors[0])
            "gradient" -> gradient(c, wf, hf, colors, spec, t, p)
            "aurora" -> aurora(c, wf, hf, colors, spec, t, p)
            "particles" -> particles(c, wf, hf, colors, spec, t, p)
            "stars" -> stars(c, wf, hf, colors, spec, t, p)
            "waves" -> waves(c, wf, hf, colors, spec, t, p)
            "rain" -> rain(c, wf, hf, colors, spec, t, p)
            "image" -> image(c, wf, hf, colors, MediaCache.bitmap(spec.path))
            "video" -> image(c, wf, hf, colors, MediaCache.videoThumb(spec.path))
            "gif" -> gif(c, wf, hf, colors, spec)
            else -> c.drawColor(colors[0])
        }
        if (spec.dim > 0f) {
            c.drawColor((spec.dim.coerceIn(0f, 0.9f) * 255).toInt() shl 24)
        }
    }

    private fun gradient(c: Canvas, w: Float, h: Float, colors: List<Int>, spec: WallpaperSpec, t: Float, p: Paint) {
        val arr = if (colors.size == 1) intArrayOf(colors[0], colors[0]) else colors.toIntArray()
        val deg = spec.angle + sin(t * 0.15f * spec.speed) * 25f
        val ang = Math.toRadians(deg.toDouble()).toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val half = (abs(w * cos(ang)) + abs(h * sin(ang))) / 2f
        val dx = cos(ang) * half
        val dy = sin(ang) * half
        p.shader = LinearGradient(cx - dx, cy - dy, cx + dx, cy + dy, arr, null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
    }

    private fun aurora(c: Canvas, w: Float, h: Float, colors: List<Int>, spec: WallpaperSpec, t: Float, p: Paint) {
        c.drawColor(colors[0])
        val blobColors = if (colors.size > 1) colors.drop(1) else listOf(Color.parseColor("#5B2BFF"))
        val n = max(3, blobColors.size)
        val sp = spec.speed
        for (i in 0 until n) {
            val col = blobColors[i % blobColors.size]
            val ph = i * 1.7f
            val cx = w * (0.5f + 0.38f * sin(t * 0.23f * sp + ph * 1.3f))
            val cy = h * (0.5f + 0.34f * cos(t * 0.19f * sp + ph))
            val r = max(w, h) * (0.55f + 0.1f * sin(t * 0.3f * sp + ph))
            p.shader = RadialGradient(
                cx, cy, r,
                intArrayOf(Col.withAlpha(col, 0.78f), Col.withAlpha(col, 0f)),
                null, Shader.TileMode.CLAMP
            )
            c.drawCircle(cx, cy, r, p)
        }
        p.shader = null
    }

    private fun particles(c: Canvas, w: Float, h: Float, colors: List<Int>, spec: WallpaperSpec, t: Float, p: Paint) {
        c.drawColor(colors[0])
        val pcolors = if (colors.size > 1) colors.drop(1) else listOf(Color.WHITE)
        val count = (55 * spec.density).toInt().coerceIn(5, 320)
        val k = w / 1080f
        for (i in 0 until count) {
            val r1 = hash(i * 3 + 1)
            val r2 = hash(i * 3 + 2)
            val r3 = hash(i * 3 + 3)
            val x = (r1 + 0.03f * sin(t * 0.6f * spec.speed + i)) * w
            val y = h * (1f - ((r2 + t * spec.speed * (0.010f + 0.03f * r3)) % 1f))
            val rad = (2.5f + 8f * r3 * r3) * k * 2f
            val pulse = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * 1.3f + i))
            val col = pcolors[i % pcolors.size]
            p.color = Col.withAlpha(col, 0.16f * pulse)
            c.drawCircle(x, y, rad * 3f, p)
            p.color = Col.withAlpha(col, 0.85f * pulse)
            c.drawCircle(x, y, rad, p)
        }
    }

    private fun stars(c: Canvas, w: Float, h: Float, colors: List<Int>, spec: WallpaperSpec, t: Float, p: Paint) {
        val top = colors[0]
        val bottom = if (colors.size > 1) colors[1] else colors[0]
        p.shader = LinearGradient(0f, 0f, 0f, h, top, bottom, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        val starColor = if (colors.size > 2) colors[2] else Color.WHITE
        val count = (130 * spec.density).toInt().coerceIn(10, 500)
        val k = w / 1080f
        for (i in 0 until count) {
            val r1 = hash(i * 4 + 1)
            val r2 = hash(i * 4 + 2)
            val r3 = hash(i * 4 + 3)
            val x = (r1 * w + t * spec.speed * (2f + 6f * r3)) % w
            val y = r2 * h
            val tw = sin(t * (0.8f + 1.5f * r3) + i)
            val a = 0.25f + 0.75f * tw * tw
            p.color = Col.withAlpha(starColor, a)
            c.drawCircle(x, y, (1.2f + 2.6f * r3) * k * 1.6f, p)
        }
        // shooting star every ~7s
        val period = 7f
        val bucket = floor(t / period).toInt()
        val prog = (t % period) / 1.3f
        if (prog in 0f..1f) {
            val sx = w * (0.2f + 0.7f * hash(bucket + 77))
            val sy = h * (0.05f + 0.3f * hash(bucket + 91))
            val hx = sx - prog * w * 0.55f
            val hy = sy + prog * h * 0.18f
            val tx = hx + w * 0.16f
            val ty = hy - h * 0.05f
            p.shader = LinearGradient(
                tx, ty, hx, hy,
                Col.withAlpha(starColor, 0f), Col.withAlpha(starColor, 1f - prog * 0.6f),
                Shader.TileMode.CLAMP
            )
            p.strokeWidth = 3f * k * 1.6f
            p.strokeCap = Paint.Cap.ROUND
            c.drawLine(tx, ty, hx, hy, p)
            p.shader = null
        }
    }

    private fun waves(c: Canvas, w: Float, h: Float, colors: List<Int>, spec: WallpaperSpec, t: Float, p: Paint) {
        val top = colors[0]
        val bottom = if (colors.size > 1) colors[1] else colors[0]
        p.shader = LinearGradient(0f, 0f, 0f, h, top, bottom, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        val layers = if (colors.size > 2) colors.drop(2) else listOf(Color.parseColor("#33FFFFFF"))
        val path = Path()
        val steps = 48
        for ((i, col) in layers.withIndex()) {
            val base = h * (0.60f + 0.09f * i)
            val amp = h * (0.035f - 0.004f * i).coerceAtLeast(0.012f)
            val freq = (2f * PI.toFloat() * (1.2f + 0.5f * i)) / w
            val phase = t * spec.speed * (0.6f + 0.25f * i) * (if (i % 2 == 0) 1f else -1f)
            path.reset()
            path.moveTo(0f, h)
            for (s in 0..steps) {
                val x = w * s / steps
                val y = base + amp * sin(x * freq + phase) + amp * 0.5f * sin(x * freq * 2.1f - phase * 1.3f)
                path.lineTo(x, y)
            }
            path.lineTo(w, h)
            path.close()
            p.color = Col.withAlpha(col, 0.62f)
            c.drawPath(path, p)
        }
    }

    private fun rain(c: Canvas, w: Float, h: Float, colors: List<Int>, spec: WallpaperSpec, t: Float, p: Paint) {
        c.drawColor(colors[0])
        val dropColor = if (colors.size > 1) colors[1] else Color.WHITE
        val count = (90 * spec.density).toInt().coerceIn(10, 400)
        val k = w / 1080f
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 2.5f * k * 1.5f
        for (i in 0 until count) {
            val r1 = hash(i * 3 + 11)
            val r2 = hash(i * 3 + 12)
            val r3 = hash(i * 3 + 13)
            val x = r1 * w
            val y = ((r2 + t * spec.speed * (0.35f + 0.5f * r3)) % 1f) * (h + 200f) - 100f
            val len = (28f + 90f * r3) * k * 1.5f
            p.color = Col.withAlpha(dropColor, 0.18f + 0.45f * r3)
            c.drawLine(x, y, x - len * 0.12f, y + len, p)
        }
    }

    private fun image(c: Canvas, w: Float, h: Float, colors: List<Int>, bmp: Bitmap?) {
        if (bmp == null) {
            c.drawColor(colors[0])
            return
        }
        val k = max(w / bmp.width, h / bmp.height)
        val dw = bmp.width * k
        val dh = bmp.height * k
        val dst = RectF((w - dw) / 2f, (h - dh) / 2f, (w + dw) / 2f, (h + dh) / 2f)
        c.drawBitmap(bmp, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private fun gif(c: Canvas, w: Float, h: Float, colors: List<Int>, spec: WallpaperSpec) {
        val d = MediaCache.gif(spec.path)
        if (d == null) {
            image(c, w, h, colors, MediaCache.bitmap(spec.path))
            return
        }
        val iw = max(1, d.intrinsicWidth).toFloat()
        val ih = max(1, d.intrinsicHeight).toFloat()
        val k = max(w / iw, h / ih)
        val dw = iw * k
        val dh = ih * k
        c.save()
        c.clipRect(0f, 0f, w, h)
        d.setBounds(((w - dw) / 2f).toInt(), ((h - dh) / 2f).toInt(), ((w + dw) / 2f).toInt(), ((h + dh) / 2f).toInt())
        d.draw(c)
        c.restore()
    }

    /** Renders one frame to a bitmap (used to set a static wallpaper). */
    fun renderBitmap(w: Int, h: Int, spec: WallpaperSpec, t: Float = 6f): Bitmap {
        val b = Bitmap.createBitmap(max(1, w), max(1, h), Bitmap.Config.ARGB_8888)
        draw(Canvas(b), w, h, spec, t)
        return b
    }

    @Suppress("unused")
    private fun clampMin(a: Float, b: Float) = min(a, b)
}
