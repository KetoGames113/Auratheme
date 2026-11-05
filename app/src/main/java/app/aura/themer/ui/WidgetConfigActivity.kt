package app.aura.themer.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.aura.themer.data.Prefs
import app.aura.themer.data.Repo
import app.aura.themer.widget.AuraWidgets

/** Shown when a system widget is added: pick which Aura widget design it should display. */
class WidgetConfigActivity : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Pinned from inside Aura: bind straight away.
        val pending = Prefs.pendingWidget(this)
        if (pending != null && Repo.active.value.widgets.any { it.id == pending }) {
            Prefs.setPendingWidget(this, null)
            finishWith(pending)
            return
        }

        setContent {
            val theme by Repo.active.collectAsState()
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AuraMaterial {
                    AuraBackdrop(Modifier.fillMaxSize()) {
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("اختر تصميم الويدجت", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp))
                            if (theme.widgets.isEmpty()) {
                                Text("ما فيه ويدجت في الثيم الحالي. افتح Aura وأضف واحد.", color = AuraColors.Muted)
                            }
                            theme.widgets.forEach { w ->
                                GlassCard(Modifier.fillMaxWidth(), onClick = { finishWith(w.id) }) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(w.name, style = MaterialTheme.typography.titleSmall)
                                        Box(
                                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            WidgetView(
                                                w, Modifier.fillMaxWidth().aspectRatio(w.w.toFloat() / w.h.toFloat()),
                                                font = theme.font, animate = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun finishWith(specId: String) {
        Prefs.bindWidget(this, widgetId, specId)
        AuraWidgets.update(this, AppWidgetManager.getInstance(this), widgetId)
        AuraWidgets.scheduleTick(this)
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}
