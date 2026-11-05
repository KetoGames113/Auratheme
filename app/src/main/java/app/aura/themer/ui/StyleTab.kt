package app.aura.themer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import app.aura.themer.data.AppEntry
import app.aura.themer.data.Apps
import app.aura.themer.data.Fonts
import app.aura.themer.data.IconPacks
import app.aura.themer.data.Media
import app.aura.themer.data.Repo
import app.aura.themer.model.IconStyle
import app.aura.themer.model.NotifStyle
import app.aura.themer.render.NotifRenderer
import app.aura.themer.services.NotifPoster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StyleTab() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconsSection()
        FontsSection()
        NotificationsSection()
        LauncherSection()
        Box(Modifier.height(24.dp))
    }
}

// ───────────────────────── Icons ─────────────────────────

private val shapes = listOf(
    "squircle" to "دائري ناعم",
    "circle" to "دائرة",
    "square" to "مربع",
    "teardrop" to "قطرة",
    "hexagon" to "سداسي",
    "none" to "أصلي"
)

@Composable
private fun IconsSection() {
    val ctx = LocalContext.current
    val theme by Repo.active.collectAsState()
    val st = theme.icons
    fun set(n: IconStyle) = Repo.updateActive { it.copy(icons = n) }

    val apps by produceState<List<AppEntry>>(emptyList()) {
        value = withContext(Dispatchers.IO) { Apps.load(ctx) }
    }
    val packs = remember { IconPacks.installed(ctx) }
    var pickApp by remember { mutableStateOf(false) }
    var pendingPkg by remember { mutableStateOf<String?>(null) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val pkg = pendingPkg
        if (uri != null && pkg != null) {
            val path = Media.importIcon(ctx, uri, pkg)
            if (path != null) {
                app.aura.themer.data.IconEngine.clearCache()
                set(st.copy(overrides = st.overrides + (pkg to path)))
            } else Actions.toast(ctx, "تعذّر قراءة الصورة")
        }
        pendingPkg = null
    }

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("أيقونات التطبيقات", "تظهر في لانشر Aura (الشاشة الرئيسية)")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(apps.take(10)) { a -> AppIcon(a, st, 54.dp) }
            }
            ChipsRow(shapes, st.shape) { set(st.copy(shape = it)) }
            LabeledSlider("حجم الرمز داخل الأيقونة", st.scale, 0.6f..1.4f) { set(st.copy(scale = it)) }

            LabeledRow("تلوين الرموز بلون واحد") {
                Switch(
                    checked = st.tint != null,
                    onCheckedChange = { on -> set(st.copy(tint = if (on) "#FFFFFF" else null)) },
                    colors = SwitchDefaults.colors(checkedTrackColor = AuraColors.Violet)
                )
            }
            if (st.tint != null) ColorField("لون الرموز", st.tint) { set(st.copy(tint = it)) }

            LabeledRow("لون خلفية موحّد") {
                Switch(
                    checked = st.background != null,
                    onCheckedChange = { on -> set(st.copy(background = if (on) "#262636" else null)) },
                    colors = SwitchDefaults.colors(checkedTrackColor = AuraColors.Violet)
                )
            }
            if (st.background != null) ColorField("لون الخلفية", st.background) { set(st.copy(background = it)) }

            LabeledRow("إظهار أسماء التطبيقات") {
                Switch(
                    checked = st.showLabels,
                    onCheckedChange = { set(st.copy(showLabels = it)) },
                    colors = SwitchDefaults.colors(checkedTrackColor = AuraColors.Violet)
                )
            }
            ColorField("لون الأسماء", st.labelColor ?: "#FFFFFF") { set(st.copy(labelColor = it)) }

            SectionTitle("حزمة أيقونات")
            if (packs.isEmpty()) {
                Hint("ما فيه حزم أيقونات منصّبة. نزّل أي حزمة تدعم Nova/ADW من Play Store وبتظهر هنا.")
            } else {
                val opts = listOf("" to "بدون") + packs.map { it.pkg to it.label }
                ChipsRow(opts, st.pack ?: "") { k -> set(st.copy(pack = k.ifBlank { null })) }
            }

            SectionTitle("أيقونة مخصصة لتطبيق", "اختر تطبيق ثم صورة من معرضك")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton("اختيار تطبيق", Icons.Rounded.Add) { pickApp = true }
                if (st.overrides.isNotEmpty()) {
                    SoftButton("مسح المخصّصة (${st.overrides.size})", Icons.Rounded.Delete, danger = true) {
                        app.aura.themer.data.IconEngine.clearCache()
                        set(st.copy(overrides = emptyMap()))
                    }
                }
            }
        }
    }

    if (pickApp) {
        Dialog(onDismissRequest = { pickApp = false }) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(AuraColors.Surface)
                    .padding(12.dp)
            ) {
                Text("اختر التطبيق", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(8.dp))
                LazyColumn(Modifier.height(420.dp)) {
                    items(apps) { a ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pendingPkg = a.pkg
                                    pickApp = false
                                    pickImage.launch("image/*")
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(a, st.copy(overrides = emptyMap()), 40.dp)
                            Box(Modifier.width(12.dp))
                            Text(a.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── Fonts ─────────────────────────

@Composable
private fun FontsSection() {
    val ctx = LocalContext.current
    val theme by Repo.active.collectAsState()
    val version by Fonts.version.collectAsState()
    val fonts = remember(version) { Fonts.all() }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = Fonts.import(ctx, uri)
            if (name != null) {
                Repo.updateActive { it.copy(font = name) }
                Actions.toast(ctx, "تم استيراد الخط وتطبيقه ✓")
            } else Actions.toast(ctx, "الملف مو خط صالح (TTF / OTF)")
        }
    }

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("الخطوط", "الخط يتطبّق على التطبيق والويدجت والإشعارات واللانشر")
            GradientButton("استيراد خط (TTF / OTF)", Icons.Rounded.Upload, Modifier.fillMaxWidth()) { pick.launch("*/*") }

            FontRow("الخط الافتراضي", theme.font == null, FontFamily.Default) {
                Repo.updateActive { it.copy(font = null) }
            }
            fonts.forEach { f ->
                FontRow(
                    f.name, theme.font == f.name, Fonts.composeFamily(f.name),
                    onDelete = if (!f.system) ({
                        if (theme.font == f.name) Repo.updateActive { it.copy(font = null) }
                        Fonts.delete(f.name)
                    }) else null
                ) { Repo.updateActive { it.copy(font = f.name) } }
            }
        }
    }
}

@Composable
private fun FontRow(
    label: String,
    selected: Boolean,
    family: FontFamily,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    GlassCard(Modifier.fillMaxWidth(), selected = selected, onClick = onClick) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = AuraColors.Muted)
                Text("أهلاً Aura 12:45 ✨", fontFamily = family, style = MaterialTheme.typography.titleMedium)
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, "حذف", tint = AuraColors.Pink) }
            }
        }
    }
}

// ───────────────────────── Notifications ─────────────────────────

private val notifStyles = listOf("glass" to "زجاجي", "solid" to "صلب", "outline" to "إطار", "minimal" to "بسيط")

@Composable
private fun NotificationsSection() {
    val ctx = LocalContext.current
    val theme by Repo.active.collectAsState()
    val st = theme.notifications
    fun set(n: NotifStyle) = Repo.updateActive { it.copy(notifications = n) }

    var granted by remember { mutableStateOf(Actions.notificationAccessGranted(ctx)) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1500)
            granted = Actions.notificationAccessGranted(ctx)
        }
    }

    fun postTest() {
        NotifPoster.post(
            ctx, st.copy(enabled = true), theme.font, 4242, "Aura",
            "إشعار تجريبي", "هذا شكل إشعاراتك بالثيم الحالي ✨", null, null
        )
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) postTest() else Actions.toast(ctx, "فعّل إذن الإشعارات لتجربة الشكل")
    }

    val preview = remember(st, theme.font) {
        val dm = ctx.resources.displayMetrics
        NotifRenderer.render(
            st, theme.font, "Aura", "رسالة جديدة", "مرحباً! هذا شكل الإشعارات الجديد بتصميمك الخاص.",
            "12:45", null, (dm.widthPixels - 64f * dm.density).toInt(), false, dm.density
        )
    }

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("شكل الإشعارات", "Aura يعيد عرض إشعارات التطبيقات بتصميمك")
            Image(
                bitmap = preview.asImageBitmap(), contentDescription = null,
                modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth
            )
            LabeledRow("تفعيل الإشعارات المصمّمة") {
                Switch(
                    checked = st.enabled,
                    onCheckedChange = { set(st.copy(enabled = it)) },
                    colors = SwitchDefaults.colors(checkedTrackColor = AuraColors.Violet)
                )
            }
            if (st.enabled && !granted) {
                Hint("لازم تعطي Aura صلاحية «الوصول إلى الإشعارات» عشان يشتغل.")
                GradientButton("فتح صلاحية الإشعارات", Icons.Rounded.Notifications, Modifier.fillMaxWidth(), warm = true) {
                    Actions.openNotificationAccess(ctx)
                }
            }
            ChipsRow(notifStyles, st.style) { set(st.copy(style = it)) }
            ColorField("الخلفية", st.background) { set(st.copy(background = it)) }
            ColorField("لون العنوان", st.titleColor) { set(st.copy(titleColor = it)) }
            ColorField("لون النص", st.textColor) { set(st.copy(textColor = it)) }
            ColorField("لون التمييز", st.accent) { set(st.copy(accent = it)) }
            LabeledSlider("استدارة الزوايا", st.corner, 0f..50f) { set(st.copy(corner = it)) }
            LabeledRow("إخفاء الإشعار الأصلي") {
                Switch(
                    checked = st.hideOriginal,
                    onCheckedChange = { set(st.copy(hideOriginal = it)) },
                    colors = SwitchDefaults.colors(checkedTrackColor = AuraColors.Violet)
                )
            }
            SoftButton("إرسال إشعار تجريبي", Icons.Rounded.Notifications, Modifier.fillMaxWidth()) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else postTest()
            }
        }
    }
}

// ───────────────────────── Launcher ─────────────────────────

@Composable
private fun LauncherSection() {
    val theme by Repo.active.collectAsState()
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("شبكة التطبيقات", "عدد الأعمدة في درج التطبيقات")
            ChipsRow(
                listOf("3", "4", "5", "6").map { it to "$it أعمدة" },
                theme.launcher.columns.toString()
            ) { k -> Repo.updateActive { it.copy(launcher = it.launcher.copy(columns = k.toInt())) } }
        }
    }
}
