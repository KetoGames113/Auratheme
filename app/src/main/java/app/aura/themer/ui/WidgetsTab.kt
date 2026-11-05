package app.aura.themer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.aura.themer.data.Fonts
import app.aura.themer.data.Repo
import app.aura.themer.model.Templates
import app.aura.themer.model.WidgetElement
import app.aura.themer.model.WidgetSpec
import app.aura.themer.widget.AuraWidgets
import java.util.UUID

private val kinds = listOf(
    "time" to "ساعة", "date" to "تاريخ", "weekday" to "يوم", "battery" to "بطارية",
    "text" to "نص", "ring" to "حلقة", "rect" to "مستطيل", "circle" to "دائرة", "line" to "خط"
)
private val anims = listOf(
    "none" to "بدون", "pulse" to "نبض", "float" to "طفو", "bounce" to "قفز",
    "rotate" to "دوران", "blink" to "وميض", "glow" to "توهج"
)
private val ringSources = listOf(
    "battery" to "البطارية", "seconds" to "الثواني", "minutes" to "الدقائق", "hours" to "الساعات", "day" to "اليوم"
)
private val aligns = listOf("center" to "وسط", "left" to "يسار", "right" to "يمين")

private fun newElement(kind: String): WidgetElement = when (kind) {
    "time" -> WidgetElement(kind = kind, size = 48f, bold = true)
    "date" -> WidgetElement(kind = kind, size = 16f, y = 0.8f)
    "weekday" -> WidgetElement(kind = kind, size = 22f, y = 0.3f)
    "battery" -> WidgetElement(kind = kind, size = 18f, y = 0.8f)
    "ring" -> WidgetElement(kind = kind, text = "battery", w = 80f, size = 8f, color = "#FF4DA6")
    "rect" -> WidgetElement(kind = kind, w = 120f, h = 40f, size = 12f, color = "#337C5CFF")
    "circle" -> WidgetElement(kind = kind, w = 40f, color = "#337C5CFF")
    "line" -> WidgetElement(kind = kind, w = 80f, h = 3f, color = "#7C5CFF", y = 0.85f)
    else -> WidgetElement(kind = "text", text = "نص جديد", size = 22f)
}

@Composable
fun WidgetsTab(onOpenAi: () -> Unit) {
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    val theme by Repo.active.collectAsState()
    val spec = theme.widgets.firstOrNull { it.id == editing }
    if (editing != null && spec != null) {
        WidgetEditor(spec) { editing = null }
    } else {
        WidgetList(onEdit = { editing = it }, onOpenAi = onOpenAi)
    }
}

@Composable
private fun WidgetList(onEdit: (String) -> Unit, onOpenAi: () -> Unit) {
    val ctx = LocalContext.current
    val theme by Repo.active.collectAsState()

    fun add(w: WidgetSpec) = Repo.updateActive { it.copy(widgets = it.widgets + w.copy(id = UUID.randomUUID().toString())) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("ويدجت الثيم", "تظهر تلقائياً على شاشة Aura الرئيسية، ويمكن إضافتها كويدجت نظام لأي لانشر.")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GradientButton("ويدجت بالذكاء", Icons.Rounded.AutoAwesome) { onOpenAi() }
                    SoftButton("ويدجت فارغ", Icons.Rounded.Add) {
                        add(WidgetSpec(name = "ويدجت جديد", elements = listOf(newElement("time"))))
                    }
                }
                SectionTitle("قوالب جاهزة")
                ChipsRow(Templates.all().mapIndexed { i, w -> i.toString() to w.name }, "-1") { k ->
                    add(Templates.all()[k.toInt()])
                }
            }
        }

        if (theme.widgets.isEmpty()) Hint("ما فيه ويدجت في هذا الثيم. أضف من القوالب أو بالذكاء.")

        theme.widgets.forEach { w ->
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(w.name, style = MaterialTheme.typography.titleSmall)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        WallpaperView(theme.wallpaper, Modifier.matchParentSize(), animate = false)
                        Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                            WidgetView(
                                w,
                                Modifier.fillMaxWidth().aspectRatio(w.w.toFloat() / w.h.toFloat()),
                                font = theme.font
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradientButton("تعديل", Icons.Rounded.Edit) { onEdit(w.id) }
                        SoftButton("للشاشة", Icons.Rounded.PushPin) {
                            if (!AuraWidgets.requestPin(ctx, w.id)) {
                                Actions.toast(ctx, "أضفه يدوياً: اضغط مطولاً على الشاشة ← الويدجت ← Aura")
                            }
                        }
                        IconButton(onClick = { add(w.copy(name = w.name + " (نسخة)")) }) {
                            Icon(Icons.Rounded.ContentCopy, "نسخ", tint = AuraColors.Muted)
                        }
                        IconButton(onClick = {
                            Repo.updateActive { t -> t.copy(widgets = t.widgets.filter { it.id != w.id }) }
                        }) { Icon(Icons.Rounded.Delete, "حذف", tint = AuraColors.Pink) }
                    }
                }
            }
        }
        Box(Modifier.height(24.dp))
    }
}

@Composable
private fun WidgetEditor(spec: WidgetSpec, onBack: () -> Unit) {
    val theme by Repo.active.collectAsState()
    val fontVersion by Fonts.version.collectAsState()
    val fonts = remember(fontVersion) { Fonts.all().map { it.name } }
    var sel by remember { mutableIntStateOf(0) }
    val idx = if (spec.elements.isEmpty()) -1 else sel.coerceIn(0, spec.elements.lastIndex)

    fun setSpec(n: WidgetSpec) = Repo.updateActive { t -> t.copy(widgets = t.widgets.map { if (it.id == n.id) n else it }) }
    fun setEl(n: WidgetElement) {
        if (idx < 0) return
        setSpec(spec.copy(elements = spec.elements.toMutableList().also { it[idx] = n }))
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "رجوع", tint = AuraColors.Text) }
            Text("تعديل الويدجت", style = MaterialTheme.typography.titleLarge)
        }

        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))) {
            WallpaperView(theme.wallpaper, Modifier.matchParentSize(), animate = false)
            Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                WidgetView(
                    spec,
                    Modifier.fillMaxWidth().aspectRatio(spec.w.toFloat() / spec.h.toFloat()),
                    font = theme.font
                )
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("الويدجت")
                TextInput(spec.name, { setSpec(spec.copy(name = it)) }, "الاسم")
                LabeledSlider("العرض", spec.w.toFloat(), 100f..420f) { setSpec(spec.copy(w = it.toInt())) }
                LabeledSlider("الارتفاع", spec.h.toFloat(), 60f..420f) { setSpec(spec.copy(h = it.toInt())) }
                LabeledSlider("استدارة الزوايا", spec.corner, 0f..150f) { setSpec(spec.copy(corner = it)) }
                ColorField("لون الخلفية", spec.background) { setSpec(spec.copy(background = it)) }
                LabeledRow("إطار") {
                    Switch(
                        checked = spec.border != null,
                        onCheckedChange = { on -> setSpec(spec.copy(border = if (on) "#55FFFFFF" else null)) },
                        colors = SwitchDefaults.colors(checkedTrackColor = AuraColors.Violet)
                    )
                }
                if (spec.border != null) ColorField("لون الإطار", spec.border) { setSpec(spec.copy(border = it)) }
                Hint("مكان الويدجت على الشاشة الرئيسية (لانشر Aura):")
                LabeledSlider("أفقي", spec.x, 0f..1f) { setSpec(spec.copy(x = it)) }
                LabeledSlider("عمودي", spec.y, 0f..0.9f) { setSpec(spec.copy(y = it)) }
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("العناصر", "اختر عنصر لتعديله، أو أضف عنصر جديد")
                if (spec.elements.isNotEmpty()) {
                    ChipsRow(
                        spec.elements.mapIndexed { i, e -> i.toString() to "${i + 1}. ${kinds.firstOrNull { it.first == e.kind }?.second ?: e.kind}" },
                        idx.toString()
                    ) { sel = it.toInt() }
                }
                Hint("إضافة:")
                ChipsRow(kinds, "") { k ->
                    setSpec(spec.copy(elements = spec.elements + newElement(k)))
                    sel = spec.elements.size
                }
            }
        }

        if (idx >= 0) {
            val e = spec.elements[idx]
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("خصائص العنصر")
                    ChipsRow(kinds, e.kind) { setEl(e.copy(kind = it)) }

                    when (e.kind) {
                        "text" -> TextInput(e.text, { setEl(e.copy(text = it)) }, "النص ({time} {date} {day} {battery})", minLines = 2)
                        "time", "date", "weekday" -> TextInput(
                            e.format ?: "", { setEl(e.copy(format = it.ifBlank { null })) },
                            "صيغة (اختياري) مثل HH:mm أو hh أو d MMMM"
                        )
                        "ring" -> ChipsRow(ringSources, e.text.ifBlank { "battery" }) { setEl(e.copy(text = it)) }
                    }

                    LabeledSlider("الموضع الأفقي", e.x, 0f..1f) { setEl(e.copy(x = it)) }
                    LabeledSlider("الموضع العمودي", e.y, 0f..1f) { setEl(e.copy(y = it)) }

                    val sizeLabel = when (e.kind) {
                        "ring" -> "سماكة الحلقة"
                        "rect" -> "استدارة المستطيل"
                        "circle", "line" -> null
                        else -> "حجم الخط"
                    }
                    if (sizeLabel != null) LabeledSlider(sizeLabel, e.size, 4f..140f) { setEl(e.copy(size = it)) }
                    if (e.kind in setOf("ring", "rect", "circle", "line")) {
                        LabeledSlider(if (e.kind == "line") "الطول" else "العرض / القطر", e.w, 4f..420f) { setEl(e.copy(w = it)) }
                    }
                    if (e.kind in setOf("rect", "line")) {
                        LabeledSlider(if (e.kind == "line") "السماكة" else "الارتفاع", e.h, 1f..300f) { setEl(e.copy(h = it)) }
                    }

                    ColorField("اللون", e.color) { setEl(e.copy(color = it)) }
                    LabeledSlider("الشفافية", e.opacity, 0f..1f) { setEl(e.copy(opacity = it)) }

                    if (e.kind in setOf("time", "date", "weekday", "battery", "text")) {
                        LabeledRow("خط عريض") {
                            Switch(
                                checked = e.bold,
                                onCheckedChange = { setEl(e.copy(bold = it)) },
                                colors = SwitchDefaults.colors(checkedTrackColor = AuraColors.Violet)
                            )
                        }
                        SectionTitle("الخط")
                        ChipsRow(
                            listOf("" to "خط الثيم") + fonts.map { it to it },
                            e.font ?: ""
                        ) { setEl(e.copy(font = it.ifBlank { null })) }
                        ChipsRow(aligns, e.align) { setEl(e.copy(align = it)) }
                    }

                    SectionTitle("الحركة")
                    ChipsRow(anims, e.anim) { setEl(e.copy(anim = it)) }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SoftButton("نسخ العنصر", Icons.Rounded.ContentCopy) {
                            setSpec(spec.copy(elements = spec.elements + e.copy(x = (e.x + 0.05f).coerceAtMost(1f), y = (e.y + 0.05f).coerceAtMost(1f))))
                            sel = spec.elements.size
                        }
                        SoftButton("حذف العنصر", Icons.Rounded.Delete, danger = true) {
                            setSpec(spec.copy(elements = spec.elements.toMutableList().also { it.removeAt(idx) }))
                            sel = 0
                        }
                    }
                }
            }
        }
        Box(Modifier.height(24.dp))
    }
}
