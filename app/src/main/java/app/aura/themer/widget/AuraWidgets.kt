package app.aura.themer.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.RemoteViews
import app.aura.themer.R
import app.aura.themer.data.Prefs
import app.aura.themer.data.Repo
import app.aura.themer.model.Templates
import app.aura.themer.model.WidgetSpec
import app.aura.themer.render.Battery
import app.aura.themer.render.WidgetRenderer
import app.aura.themer.ui.MainActivity
import kotlin.math.max
import kotlin.math.min

object AuraWidgets {
    const val ACTION_TICK = "app.aura.themer.TICK"

    private val providers = listOf(AuraWidgetSmall::class.java, AuraWidgetMedium::class.java, AuraWidgetLarge::class.java)

    fun resolveSpec(ctx: Context, appWidgetId: Int): WidgetSpec {
        val sid = Prefs.widgetBinding(ctx, appWidgetId)
        val th = Repo.active.value
        return th.widgets.firstOrNull { it.id == sid } ?: th.widgets.firstOrNull() ?: Templates.defaultClock()
    }

    fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
        try {
            val spec = resolveSpec(ctx, id)
            val opts = mgr.getAppWidgetOptions(id)
            val density = ctx.resources.displayMetrics.density
            val availW = max(60, opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, spec.w))
            val availH = max(40, opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, spec.h))
            val s = min(availW.toFloat() / spec.w, availH.toFloat() / spec.h)
            var bw = spec.w * s * density
            var bh = spec.h * s * density
            val cap = min(1f, 1000f / max(bw, bh))
            bw *= cap
            bh *= cap
            val bmp = Bitmap.createBitmap(max(32, bw.toInt()), max(32, bh.toInt()), Bitmap.Config.ARGB_8888)
            val (lvl, chg) = Battery.read(ctx)
            WidgetRenderer.draw(
                Canvas(bmp), bmp.width.toFloat(), bmp.height.toFloat(), spec,
                WidgetRenderer.Env(System.currentTimeMillis(), 0f, lvl, chg, Repo.active.value.font)
            )
            val rv = RemoteViews(ctx.packageName, R.layout.widget_view)
            rv.setImageViewBitmap(R.id.widget_img, bmp)
            val pi = PendingIntent.getActivity(
                ctx, id, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            rv.setOnClickPendingIntent(R.id.widget_img, pi)
            mgr.updateAppWidget(id, rv)
        } catch (e: Exception) {
            // keep the previous remote views
        }
    }

    /** Updates every placed widget. Returns how many exist. */
    fun updateAll(ctx: Context): Int {
        val mgr = AppWidgetManager.getInstance(ctx)
        var count = 0
        for (cls in providers) {
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, cls))
            count += ids.size
            ids.forEach { update(ctx, mgr, it) }
        }
        return count
    }

    fun scheduleTick(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            ctx, 7, Intent(ctx, AuraTickReceiver::class.java).setAction(ACTION_TICK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val next = (System.currentTimeMillis() / 60_000L + 1L) * 60_000L
        am.setAndAllowWhileIdle(AlarmManager.RTC, next, pi)
    }

    fun cancelTick(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            ctx, 7, Intent(ctx, AuraTickReceiver::class.java).setAction(ACTION_TICK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        am.cancel(pi)
    }

    fun requestPin(ctx: Context, specId: String): Boolean {
        val mgr = AppWidgetManager.getInstance(ctx)
        if (!mgr.isRequestPinAppWidgetSupported) return false
        Prefs.setPendingWidget(ctx, specId)
        return mgr.requestPinAppWidget(ComponentName(ctx, AuraWidgetMedium::class.java), null, null)
    }
}

open class AuraWidgetBase : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { AuraWidgets.update(context, appWidgetManager, it) }
        AuraWidgets.scheduleTick(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle) {
        AuraWidgets.update(context, appWidgetManager, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        AuraWidgets.scheduleTick(context)
    }

    override fun onDisabled(context: Context) {
        AuraWidgets.cancelTick(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { Prefs.unbindWidget(context, it) }
    }
}

class AuraWidgetSmall : AuraWidgetBase()
class AuraWidgetMedium : AuraWidgetBase()
class AuraWidgetLarge : AuraWidgetBase()

class AuraTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val n = AuraWidgets.updateAll(context)
        if (n > 0) AuraWidgets.scheduleTick(context)
    }
}
