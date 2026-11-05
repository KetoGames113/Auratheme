package app.aura.themer.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.util.LruCache
import android.util.Xml
import app.aura.themer.model.IconStyle
import app.aura.themer.render.Col
import org.xmlpull.v1.XmlPullParser
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

data class AppEntry(val pkg: String, val cls: String, val label: String)

object Apps {
    @Volatile
    private var cache: List<AppEntry>? = null

    fun load(ctx: Context, force: Boolean = false): List<AppEntry> {
        val c = cache
        if (c != null && !force) return c
        val pm = ctx.packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val list = pm.queryIntentActivities(i, 0)
            .map { AppEntry(it.activityInfo.packageName, it.activityInfo.name, it.loadLabel(pm).toString()) }
            .sortedBy { it.label.lowercase() }
        cache = list
        return list
    }

    fun launch(ctx: Context, e: AppEntry) {
        val i = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(e.pkg, e.cls))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            ctx.startActivity(i)
        } catch (ex: Exception) {
            // app not launchable anymore
        }
    }

    fun launchIntent(e: AppEntry): Intent =
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(e.pkg, e.cls))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

    fun defaultDock(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        )
        val all = load(ctx)
        return intents
            .mapNotNull { pm.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName }
            .distinct()
            .mapNotNull { p -> all.firstOrNull { it.pkg == p } }
    }

    fun dockFor(ctx: Context, packages: List<String>): List<AppEntry> {
        if (packages.isEmpty()) return defaultDock(ctx)
        val all = load(ctx)
        return packages.mapNotNull { p -> all.firstOrNull { it.pkg == p } }.ifEmpty { defaultDock(ctx) }
    }
}

object IconPacks {
    data class Pack(val pkg: String, val label: String)

    private val actions = listOf(
        "org.adw.launcher.THEMES",
        "com.novalauncher.THEME",
        "com.gau.go.launcherex.theme"
    )
    private val maps = HashMap<String, Map<String, String>>()

    fun installed(ctx: Context): List<Pack> {
        val pm = ctx.packageManager
        val out = LinkedHashMap<String, Pack>()
        for (a in actions) {
            for (ri in pm.queryIntentActivities(Intent(a), 0)) {
                val p = ri.activityInfo.packageName
                if (!out.containsKey(p)) out[p] = Pack(p, ri.loadLabel(pm).toString())
            }
        }
        return out.values.sortedBy { it.label }
    }

    @Synchronized
    private fun map(ctx: Context, pack: String): Map<String, String> =
        maps.getOrPut(pack) { parse(ctx, pack) }

    private fun parse(ctx: Context, pack: String): Map<String, String> {
        val out = HashMap<String, String>()
        try {
            val res = ctx.packageManager.getResourcesForApplication(pack)
            val xmlId = res.getIdentifier("appfilter", "xml", pack)
            val parser: XmlPullParser = if (xmlId != 0) {
                res.getXml(xmlId)
            } else {
                Xml.newPullParser().also { it.setInput(res.assets.open("appfilter.xml"), "UTF-8") }
            }
            var ev = parser.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG && parser.name == "item") {
                    val comp = parser.getAttributeValue(null, "component")
                    val dr = parser.getAttributeValue(null, "drawable")
                    if (comp != null && dr != null) {
                        out[comp.removePrefix("ComponentInfo{").removeSuffix("}")] = dr
                    }
                }
                ev = parser.next()
            }
        } catch (ex: Exception) {
            // unreadable pack
        }
        return out
    }

    @Suppress("DEPRECATION")
    fun drawable(ctx: Context, pack: String, e: AppEntry): Drawable? {
        val name = map(ctx, pack)["${e.pkg}/${e.cls}"] ?: return null
        return try {
            val res = ctx.packageManager.getResourcesForApplication(pack)
            val id = res.getIdentifier(name, "drawable", pack)
            if (id != 0) res.getDrawable(id, null) else null
        } catch (ex: Exception) {
            null
        }
    }
}

object IconEngine {
    private val cache = object : LruCache<String, Bitmap>(160) {}

    fun clearCache() = cache.evictAll()

    fun render(ctx: Context, e: AppEntry, st: IconStyle, sizePx: Int): Bitmap {
        val key = "${e.pkg}/${e.cls}|${st.shape}|${st.tint}|${st.background}|${st.scale}|${st.pack}|${st.overrides[e.pkg]}|$sizePx"
        cache.get(key)?.let { return it }
        val b = build(ctx, e, st, sizePx)
        cache.put(key, b)
        return b
    }

    private fun shapePath(shape: String, s: Float): Path {
        val p = Path()
        when (shape) {
            "circle" -> p.addCircle(s / 2f, s / 2f, s / 2f, Path.Direction.CW)
            "square" -> p.addRoundRect(0f, 0f, s, s, s * 0.14f, s * 0.14f, Path.Direction.CW)
            "teardrop" -> p.addRoundRect(
                RectF(0f, 0f, s, s),
                floatArrayOf(s / 2f, s / 2f, s / 2f, s / 2f, s / 2f, s / 2f, s * 0.1f, s * 0.1f),
                Path.Direction.CW
            )
            "hexagon" -> {
                for (i in 0 until 6) {
                    val a = (PI / 3.0 * i - PI / 2.0)
                    val x = (s / 2f + (s / 2f) * 0.98f * cos(a)).toFloat()
                    val y = (s / 2f + (s / 2f) * 0.98f * sin(a)).toFloat()
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
            }
            else -> p.addRoundRect(0f, 0f, s, s, s * 0.34f, s * 0.34f, Path.Direction.CW)
        }
        return p
    }

    private fun drawLayer(c: Canvas, layer: Drawable, size: Int, scale: Float, tint: Int?) {
        val extent = size * 1.5f * scale
        val off = (size - extent) / 2f
        layer.setBounds(off.toInt(), off.toInt(), (off + extent).toInt(), (off + extent).toInt())
        layer.colorFilter = if (tint != null) PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN) else null
        layer.draw(c)
        layer.colorFilter = null
    }

    private fun build(ctx: Context, e: AppEntry, st: IconStyle, size: Int): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val s = size.toFloat()
        val clip = st.shape != "none"
        val path = shapePath(st.shape, s)
        val tint = st.tint?.let { Col.parse(it, Color.WHITE) }
        val bg = st.background?.let { Col.parse(it, Color.DKGRAY) }

        // 1) user supplied image
        val ov = st.overrides[e.pkg]
        if (ov != null && File(ov).exists()) {
            val bmp = BitmapFactory.decodeFile(ov)
            if (bmp != null) {
                c.save()
                if (clip) c.clipPath(path)
                val k = max(s / bmp.width, s / bmp.height)
                val dw = bmp.width * k
                val dh = bmp.height * k
                c.drawBitmap(bmp, null, RectF((s - dw) / 2f, (s - dh) / 2f, (s + dw) / 2f, (s + dh) / 2f), Paint(Paint.FILTER_BITMAP_FLAG))
                c.restore()
                return out
            }
        }

        // 2) icon pack drawable
        val packName = st.pack
        if (!packName.isNullOrBlank()) {
            val pd = IconPacks.drawable(ctx, packName, e)
            if (pd != null) {
                pd.setBounds(0, 0, size, size)
                pd.draw(c)
                return out
            }
        }

        // 3) the app's own icon, re-masked
        val pm = ctx.packageManager
        val base: Drawable = try {
            pm.getActivityIcon(ComponentName(e.pkg, e.cls))
        } catch (ex: Exception) {
            pm.defaultActivityIcon
        }
        val d = base.mutate()
        val scale = st.scale.coerceIn(0.5f, 1.6f)

        c.save()
        if (clip) c.clipPath(path)
        if (d is AdaptiveIconDrawable) {
            if (bg != null) c.drawColor(bg) else d.background?.let { drawLayer(c, it.mutate(), size, scale, null) }
            d.foreground?.let { drawLayer(c, it.mutate(), size, scale, tint) }
        } else {
            if (clip) c.drawColor(bg ?: Color.parseColor("#262636"))
            val inset = if (clip) (s * 0.16f / scale).toInt() else 0
            d.setBounds(inset, inset, size - inset, size - inset)
            d.colorFilter = if (tint != null) PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN) else null
            d.draw(c)
            d.colorFilter = null
        }
        c.restore()
        return out
    }
}
