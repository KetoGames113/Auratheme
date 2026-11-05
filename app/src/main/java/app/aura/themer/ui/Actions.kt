package app.aura.themer.ui

import android.app.WallpaperManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import app.aura.themer.model.ThemeSchedule
import app.aura.themer.model.WallpaperSpec
import app.aura.themer.render.WallpaperRenderer
import app.aura.themer.services.AuraVideoWallpaperService
import app.aura.themer.services.AuraWallpaperService
import kotlin.concurrent.thread

object Actions {

    fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    fun copy(ctx: Context, label: String, text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        toast(ctx, "تم النسخ ✓")
    }

    fun paste(ctx: Context): String {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip ?: return ""
        if (clip.itemCount == 0) return ""
        return clip.getItemAt(0).coerceToText(ctx)?.toString().orEmpty()
    }

    fun share(ctx: Context, title: String, text: String) {
        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        ctx.startActivity(Intent.createChooser(i, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Frozen frame of the wallpaper as the system wallpaper. */
    fun setStaticWallpaper(ctx: Context, spec: WallpaperSpec) {
        val app = ctx.applicationContext
        toast(ctx, "جاري تطبيق الخلفية…")
        thread {
            try {
                val dm = app.resources.displayMetrics
                val frozen = if (spec.type == "gif" || spec.type == "video") spec.copy(type = if (spec.type == "gif") "image" else "video") else spec
                val bmp = WallpaperRenderer.renderBitmap(dm.widthPixels, dm.heightPixels, frozen)
                WallpaperManager.getInstance(app).setBitmap(bmp, null, true)
                android.os.Handler(android.os.Looper.getMainLooper()).post { toast(app, "تم تطبيق الخلفية ✓") }
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post { toast(app, "تعذّر تطبيق الخلفية") }
            }
        }
    }

    /** Opens the system preview for the Aura live wallpaper (animated, scheduled, video). */
    fun setLiveWallpaper(ctx: Context, video: Boolean) {
        val cls = if (video) AuraVideoWallpaperService::class.java else AuraWallpaperService::class.java
        try {
            val i = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(ctx, cls))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
        } catch (e: Exception) {
            try {
                ctx.startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e2: Exception) {
                toast(ctx, "جوالك ما يدعم اختيار الخلفيات الحية من هنا")
            }
        }
    }

    fun openHomeSettings(ctx: Context) {
        try {
            ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            try {
                ctx.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e2: Exception) {
                toast(ctx, "افتح: الإعدادات ← التطبيقات ← التطبيقات الافتراضية ← تطبيق الشاشة الرئيسية")
            }
        }
    }

    fun openNotificationAccess(ctx: Context) {
        try {
            ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            toast(ctx, "تعذّر فتح إعدادات الوصول للإشعارات")
        }
    }

    fun notificationAccessGranted(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
        return flat.contains(ctx.packageName)
    }

    @Suppress("unused")
    fun scheduleNow(theme: app.aura.themer.model.AuraTheme) = ThemeSchedule.resolve(theme)
}
