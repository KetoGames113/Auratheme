package app.aura.themer.data

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface as ComposeTypeface
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

object Fonts {
    data class Info(val name: String, val system: Boolean)

    val systemNames = listOf(
        "sans-serif", "sans-serif-light", "sans-serif-medium",
        "sans-serif-condensed", "serif", "monospace", "cursive", "casual"
    )

    /** Bumped whenever a font is imported / removed so UI can refresh. */
    val version = MutableStateFlow(0)

    private lateinit var appCtx: Context
    private val cache = HashMap<String, Typeface>()

    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
    }

    fun dir(): File = File(appCtx.filesDir, "fonts").apply { mkdirs() }

    fun imported(): List<String> =
        dir().listFiles()
            ?.filter { it.extension.lowercase() in setOf("ttf", "otf") }
            ?.map { it.nameWithoutExtension }
            ?.sorted()
            ?: emptyList()

    fun all(): List<Info> =
        systemNames.map { Info(it, true) } + imported().map { Info(it, false) }

    @Synchronized
    fun get(name: String?, bold: Boolean = false): Typeface {
        if (name.isNullOrBlank()) {
            return if (bold) cache.getOrPut("::bold") { Typeface.create(Typeface.DEFAULT, Typeface.BOLD) } else Typeface.DEFAULT
        }
        val base = cache.getOrPut(name) { load(name) }
        return if (bold) cache.getOrPut("b:$name") { Typeface.create(base, Typeface.BOLD) } else base
    }

    private fun load(name: String): Typeface {
        if (name in systemNames) return Typeface.create(name, Typeface.NORMAL)
        val f = listOf("ttf", "otf").map { File(dir(), "$name.$it") }.firstOrNull { it.exists() }
            ?: return Typeface.DEFAULT
        return try {
            Typeface.createFromFile(f)
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }

    fun composeFamily(name: String?): FontFamily =
        if (name.isNullOrBlank()) FontFamily.Default else FontFamily(ComposeTypeface(get(name)))

    private fun queryName(ctx: Context, uri: Uri): String? =
        try {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }

    private fun looksLikeFont(f: File): Boolean = try {
        val b = ByteArray(4)
        f.inputStream().use { it.read(b) }
        val tag = String(b, Charsets.ISO_8859_1)
        tag == "OTTO" || tag == "true" || tag == "ttcf" ||
            (b[0] == 0.toByte() && b[1] == 1.toByte() && b[2] == 0.toByte() && b[3] == 0.toByte())
    } catch (e: Exception) {
        false
    }

    /** Copies a TTF/OTF into app storage. Returns the font name or null when invalid. */
    fun import(ctx: Context, uri: Uri): String? {
        val raw = queryName(ctx, uri) ?: "font_${System.currentTimeMillis()}.ttf"
        val ext = if (raw.substringAfterLast('.', "ttf").lowercase() == "otf") "otf" else "ttf"
        val base = raw.substringBeforeLast('.')
            .replace(Regex("[^\\p{L}\\p{N}_-]"), "_")
            .take(40)
            .ifBlank { "font" }
        val f = File(dir(), "$base.$ext")
        try {
            ctx.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
                ?: return null
            if (!looksLikeFont(f)) {
                f.delete()
                return null
            }
        } catch (e: Exception) {
            f.delete()
            return null
        }
        synchronized(this) {
            cache.remove(base)
            cache.remove("b:$base")
        }
        version.value += 1
        return base
    }

    fun delete(name: String) {
        listOf("ttf", "otf").forEach { File(dir(), "$name.$it").delete() }
        synchronized(this) {
            cache.remove(name)
            cache.remove("b:$name")
        }
        version.value += 1
    }
}
