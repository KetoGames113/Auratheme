package app.aura.themer.ui

import android.app.TimePickerDialog
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Wallpaper
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.aura.themer.data.Media
import app.aura.themer.data.Repo
import app.aura.themer.model.ScheduleSlot
import app.aura.themer.model.WallpaperSpec

private val wallpaperTypes = listOf(
    "aurora" to "شفق",
    "gradient" to "تدرّج",
    "solid" to "لون ثابت",
    "particles" to "جزيئات",
    "stars" to "نجوم",
    "waves" to "أمواج",
    "rain" to "مطر",
    "image" to "صورة",
    "gif" to "GIF",
    "video" to "فيديو"
)

private val minColors = mapOf(
    "solid" to 1, "gradient" to 2, "aurora" to 3, "particles" to 2,
    "stars" to 3, "waves" to 3, "rain" to 2
)

private val padColors = listOf("#07070F", "#5B2BFF", "#00E5FF", "#FF2E93", "#FFFFFF")

private fun colorHint(type: String): String = when (type) {
    "solid" -> "لون واحد."
    "gradient" -> "ألوان التدرّج من الأعلى للأسفل (٢ – ٥ ألوان)."
    "aurora" -> "اللون الأول = الخلفية، والباقي ألوان الوهج المتحرك."
    "particles" -> "الأول = الخلفية، والباقي ألوان الجزيئات."
    "stars" -> "الأول = أعلى السماء، الثاني = أسفلها، الثالث = لون النجوم."
    "waves" -> "الأول = أعلى، الثاني = أسفل، والباقي ألوان طبقات الأمواج."
    "rain" -> "الأول = الخلفية، الثاني = لون القطرات."
    else -> ""
}

@Composable
fun WallpaperTab() {
    val ctx = LocalContext.current
    val theme by Repo.active.collectAsState()
    val wp = theme.wallpaper

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .height(340.dp)
                    .aspectRatio(0.5f)
                    .clip(RoundedCornerShape(28.dp))
            ) {
                WallpaperView(wp, Modifier.fillMaxSize())
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("الخلفية الأساسية")
                WallpaperEditor(wp) { n -> Repo.updateActive { it.copy(wallpaper = n) } }
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("تطبيق الخلفية على الجوال")
                Hint("الخلفيات المتحركة والمجدولة والفيديو تشتغل كخلفية حيّة. الثابتة تُحفظ كصورة.")
                val isVideo = wp.type == "video"
                GradientButton(
                    if (isVideo) "تفعيل كخلفية فيديو" else "تفعيل كخلفية حيّة (متحركة + مجدولة)",
                    Icons.Rounded.Wallpaper, Modifier.fillMaxWidth()
                ) { Actions.setLiveWallpaper(ctx, isVideo) }
                SoftButton("تطبيق صورة ثابتة فقط", Icons.Rounded.Smartphone, Modifier.fillMaxWidth()) {
                    Actions.setStaticWallpaper(ctx, wp)
                }
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("خلفيات حسب الوقت", "مثلاً: خلفية نهارية وخلفية ليلية. تشتغل مع الخلفية الحيّة.")
                if (theme.schedule.isEmpty()) Hint("ما فيه جدول. أضف فترة وتتبدل الخلفية تلقائياً.")
                theme.schedule.forEachIndexed { i, slot ->
                    SlotCard(
                        slot,
                        onChange = { n -> Repo.updateActive { t -> t.copy(schedule = t.schedule.toMutableList().also { it[i] = n }) } },
                        onDelete = { Repo.updateActive { t -> t.copy(schedule = t.schedule.toMutableList().also { it.removeAt(i) }) } }
                    )
                }
                SoftButton("إضافة فترة", Icons.Rounded.Add, Modifier.fillMaxWidth()) {
                    Repo.updateActive { t ->
                        t.copy(schedule = t.schedule + ScheduleSlot(
                            from = "06:00", to = "18:00",
                            wallpaper = WallpaperSpec("gradient", listOf("#FFB36B", "#FF6A8E", "#5B2BFF"), angle = 90f)
                        ))
                    }
                }
            }
        }
        Box(Modifier.height(24.dp))
    }
}

@Composable
private fun SlotCard(slot: ScheduleSlot, onChange: (ScheduleSlot) -> Unit, onDelete: () -> Unit) {
    val ctx = LocalContext.current
    GlassCard(Modifier.fillMaxWidth(), selected = false) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Schedule, null, tint = AuraColors.Cyan)
                Box(Modifier.width(8.dp))
                SoftButton(slot.from) { pickTime(ctx, slot.from) { onChange(slot.copy(from = it)) } }
                Text("  إلى  ", color = AuraColors.Muted)
                SoftButton(slot.to) { pickTime(ctx, slot.to) { onChange(slot.copy(to = it)) } }
                Box(Modifier.weight(1f))
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.Close, "حذف", tint = AuraColors.Pink) }
            }
            Box(
                Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(16.dp))
            ) { WallpaperView(slot.wallpaper, Modifier.fillMaxSize(), animate = false) }
            WallpaperEditor(slot.wallpaper) { n -> onChange(slot.copy(wallpaper = n)) }
        }
    }
}

private fun pickTime(ctx: Context, current: String, onPick: (String) -> Unit) {
    val p = current.split(":")
    val h = p.getOrNull(0)?.toIntOrNull() ?: 0
    val m = p.getOrNull(1)?.toIntOrNull() ?: 0
    TimePickerDialog(ctx, { _, hh, mm -> onPick(String.format("%02d:%02d", hh, mm)) }, h, m, true).show()
}

@Composable
fun WallpaperEditor(spec: WallpaperSpec, onChange: (WallpaperSpec) -> Unit) {
    val ctx = LocalContext.current
    val isMedia = spec.type == "image" || spec.type == "gif" || spec.type == "video"

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val r = Media.importFile(ctx, uri)
            if (r != null) onChange(spec.copy(type = r.second, path = r.first))
            else Actions.toast(ctx, "ملف غير مدعوم")
        }
    }

    ChipsRow(wallpaperTypes, spec.type) { t ->
        when (t) {
            "image" -> pickImage.launch("image/*")
            "gif" -> pickImage.launch("image/gif")
            "video" -> pickImage.launch("video/*")
            else -> {
                val need = minColors[t] ?: 1
                val colors = spec.colors.toMutableList()
                var k = 0
                while (colors.size < need) { colors.add(padColors[k % padColors.size]); k++ }
                onChange(spec.copy(type = t, colors = colors, path = null))
            }
        }
    }

    if (isMedia) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SoftButton(
                if (spec.path == null) "اختر ملف" else "تغيير الملف",
                if (spec.type == "video") Icons.Rounded.VideoLibrary else Icons.Rounded.Image
            ) {
                pickImage.launch(if (spec.type == "video") "video/*" else if (spec.type == "gif") "image/gif" else "image/*")
            }
        }
        Hint(
            when (spec.type) {
                "video" -> "الفيديو يشتغل بدون صوت ويتكرر. فعّله من زر «تفعيل كخلفية فيديو»."
                "gif" -> "الـ GIF يتحرك في الخلفية الحيّة وفي الشاشة الرئيسية (أندرويد 9+)."
                else -> "الصورة تتغطّى الشاشة بالكامل."
            }
        )
    } else {
        Hint(colorHint(spec.type))
        spec.colors.forEachIndexed { i, c ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    ColorField("لون ${i + 1}", c) { n ->
                        onChange(spec.copy(colors = spec.colors.toMutableList().also { it[i] = n }))
                    }
                }
                val need = minColors[spec.type] ?: 1
                if (spec.colors.size > need) {
                    IconButton(onClick = { onChange(spec.copy(colors = spec.colors.toMutableList().also { it.removeAt(i) })) }) {
                        Icon(Icons.Rounded.Close, "حذف", tint = AuraColors.Muted)
                    }
                }
            }
        }
        if (spec.colors.size < 8) {
            SoftButton("إضافة لون", Icons.Rounded.Add) {
                onChange(spec.copy(colors = spec.colors + padColors[spec.colors.size % padColors.size]))
            }
        }
    }

    if (!isMedia && spec.type != "solid") {
        LabeledSlider("السرعة", spec.speed, 0f..3f) { onChange(spec.copy(speed = it)) }
        if (spec.type in setOf("particles", "stars", "rain")) {
            LabeledSlider("الكثافة", spec.density, 0.3f..3f) { onChange(spec.copy(density = it)) }
        }
        if (spec.type == "gradient") {
            LabeledSlider("الزاوية", spec.angle, 0f..360f) { onChange(spec.copy(angle = it)) }
        }
    }
    LabeledSlider("تعتيم (لوضوح الأيقونات)", spec.dim, 0f..0.8f) { onChange(spec.copy(dim = it)) }
}
