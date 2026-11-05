package app.aura.themer.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

object Media {
    fun dir(ctx: Context): File = File(ctx.filesDir, "media").apply { mkdirs() }
    fun iconDir(ctx: Context): File = File(ctx.filesDir, "icons").apply { mkdirs() }

    /** Copies a picked file into private storage. Returns (path, type) with type = image | gif | video. */
    fun importFile(ctx: Context, uri: Uri): Pair<String, String>? {
        val mime = ctx.contentResolver.getType(uri) ?: ""
        val type = when {
            mime == "image/gif" -> "gif"
            mime.startsWith("video/") -> "video"
            mime.startsWith("image/") -> "image"
            else -> return null
        }
        val ext = when (type) {
            "gif" -> "gif"
            "video" -> "mp4"
            else -> "img"
        }
        val f = File(dir(ctx), "m_${System.currentTimeMillis()}.$ext")
        return try {
            ctx.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
                ?: return null
            f.absolutePath to type
        } catch (e: Exception) {
            f.delete()
            null
        }
    }

    /** Saves a picked image as an app icon override (square, max 512px). */
    fun importIcon(ctx: Context, uri: Uri, key: String): String? {
        return try {
            val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
            val side = minOf(bmp.width, bmp.height)
            val sq = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
            val scaled = Bitmap.createScaledBitmap(sq, minOf(512, side), minOf(512, side), true)
            val f = File(iconDir(ctx), key.replace(Regex("[^A-Za-z0-9_.-]"), "_") + "_${System.currentTimeMillis()}.png")
            f.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
            f.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
