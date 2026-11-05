package app.aura.themer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import app.aura.themer.data.Repo
import app.aura.themer.model.AuraTheme
import app.aura.themer.model.ThemeSchedule

@Composable
fun ThemesTab(onOpenAi: () -> Unit) {
    val ctx = LocalContext.current
    val themes by Repo.themes.collectAsState()
    val active by Repo.active.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("خلّ Aura شاشتك الرئيسية", style = MaterialTheme.typography.titleMedium)
                    Hint("عشان تظهر الويدجت المتحركة والأيقونات المخصصة على شاشتك مباشرة، اجعل Aura اللانشر الافتراضي.")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GradientButton("اجعله اللانشر", Icons.Rounded.Home) { Actions.openHomeSettings(ctx) }
                        SoftButton("ثيم بالذكاء", Icons.Rounded.AutoAwesome) { onOpenAi() }
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionTitle("ثيماتك", "اضغط على الثيم لتفعيله، وعدّل عليه من التبويبات تحت")
        }
        items(themes, key = { it.id }) { t ->
            ThemeCard(t, selected = t.id == active.id, canDelete = themes.size > 1)
        }
    }
}

@Composable
private fun ThemeCard(t: AuraTheme, selected: Boolean, canDelete: Boolean) {
    val ctx = LocalContext.current
    GlassCard(Modifier.fillMaxWidth(), selected = selected, onClick = { Repo.setActive(t.id) }) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.62f)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            ) {
                WallpaperView(ThemeSchedule.resolve(t), Modifier.fillMaxSize(), animate = false)
                val w = t.widgets.firstOrNull()
                if (w != null) {
                    WidgetView(
                        w, Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 26.dp, start = 10.dp, end = 10.dp)
                            .fillMaxWidth()
                            .aspectRatio(w.w.toFloat() / w.h.toFloat()),
                        font = t.font, animate = false
                    )
                }
                if (selected) {
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(AuraColors.Violet)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) { Text("مفعّل", style = MaterialTheme.typography.labelSmall, color = Color.White) }
                }
            }
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(t.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { Repo.duplicate(t.id) }) {
                        Icon(Icons.Rounded.ContentCopy, "نسخ", tint = AuraColors.Muted)
                    }
                    IconButton(onClick = { Actions.share(ctx, "مشاركة الثيم", Repo.export(t)) }) {
                        Icon(Icons.Rounded.Share, "مشاركة", tint = AuraColors.Muted)
                    }
                    if (canDelete) {
                        IconButton(onClick = { Repo.delete(t.id) }) {
                            Icon(Icons.Rounded.Delete, "حذف", tint = AuraColors.Pink)
                        }
                    }
                }
            }
        }
    }
}
