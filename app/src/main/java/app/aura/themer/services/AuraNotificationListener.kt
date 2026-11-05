package app.aura.themer.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.widget.RemoteViews
import app.aura.themer.R
import app.aura.themer.data.Repo
import app.aura.themer.model.NotifStyle
import app.aura.themer.render.NotifRenderer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotifPoster {
    const val CHANNEL = "aura_styled"

    private fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL) == null) {
            val ch = NotificationChannel(CHANNEL, "إشعارات Aura المصمّمة", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(ch)
        }
    }

    fun toBitmap(d: Drawable, size: Int = 96): Bitmap {
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        d.setBounds(0, 0, size, size)
        d.draw(c)
        return b
    }

    fun post(
        ctx: Context,
        st: NotifStyle,
        font: String?,
        id: Int,
        appName: String,
        title: String,
        text: String,
        icon: Bitmap?,
        contentIntent: PendingIntent?,
        actions: Array<Notification.Action>? = null
    ) {
        ensureChannel(ctx)
        val dm = ctx.resources.displayMetrics
        val widthPx = (dm.widthPixels - 24f * dm.density).toInt()
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val small = NotifRenderer.render(st, font, appName, title, text, time, icon, widthPx, true, dm.density)
        val big = NotifRenderer.render(st, font, appName, title, text, time, icon, widthPx, false, dm.density)
        val rvSmall = RemoteViews(ctx.packageName, R.layout.notif_card)
        rvSmall.setImageViewBitmap(R.id.notif_img, small)
        val rvBig = RemoteViews(ctx.packageName, R.layout.notif_card)
        rvBig.setImageViewBitmap(R.id.notif_img, big)

        val b = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_aura)
            .setCustomContentView(rvSmall)
            .setCustomBigContentView(rvBig)
            .setAutoCancel(true)
            .setContentTitle(title)
            .setContentText(text)
        if (contentIntent != null) b.setContentIntent(contentIntent)
        actions?.forEach { b.addAction(it) }
        val n = b.build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify("aura", id, n)
    }
}

/** Re-posts other apps' notifications using the active theme's card design. */
class AuraNotificationListener : NotificationListenerService() {

    private val skipPackages = setOf(
        "android", "com.android.systemui", "com.android.phone", "com.android.incallui",
        "com.android.server.telecom", "com.miui.securitycenter"
    )
    private val skipCategories = setOf(
        Notification.CATEGORY_CALL, Notification.CATEGORY_PROGRESS, Notification.CATEGORY_TRANSPORT,
        Notification.CATEGORY_ALARM, Notification.CATEGORY_NAVIGATION, Notification.CATEGORY_SERVICE,
        Notification.CATEGORY_SYSTEM
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val theme = Repo.active.value
            val st = theme.notifications
            if (!st.enabled) return
            if (sbn.packageName == packageName || sbn.packageName in skipPackages) return
            val n = sbn.notification ?: return
            val skipFlags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE or
                Notification.FLAG_GROUP_SUMMARY or Notification.FLAG_NO_CLEAR
            if (n.flags and skipFlags != 0) return
            val cat: String? = n.category
            if (cat != null && cat in skipCategories) return

            val extras = n.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
            if (title.isBlank() && text.isBlank()) return

            val pm = packageManager
            val appName = try {
                pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
            } catch (e: Exception) {
                sbn.packageName
            }
            val iconBmp: Bitmap? = try {
                val large = n.getLargeIcon()?.loadDrawable(this)
                val d = large ?: pm.getApplicationIcon(sbn.packageName)
                NotifPoster.toBitmap(d)
            } catch (e: Exception) {
                null
            }

            NotifPoster.post(
                ctx = this,
                st = st,
                font = theme.font,
                id = sbn.key.hashCode(),
                appName = appName,
                title = title.ifBlank { appName },
                text = text,
                icon = iconBmp,
                contentIntent = n.contentIntent,
                actions = n.actions
            )
            if (st.hideOriginal) cancelNotification(sbn.key)
        } catch (e: Exception) {
            // never crash the listener
        }
    }
}
