package app.aura.themer.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.aura.themer.data.AppEntry
import app.aura.themer.data.Apps
import app.aura.themer.data.Fonts
import app.aura.themer.data.Prefs
import app.aura.themer.data.Repo
import app.aura.themer.render.Col
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HomeActivity : ComponentActivity() {
    private val homeSignal = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { HomeRoot(homeSignal.intValue) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeSignal.intValue += 1
    }
}

private fun loadPins(ctx: android.content.Context): List<String> =
    Prefs.sp(ctx).getString("home_pins", "").orEmpty().split(",").filter { it.isNotBlank() }

private fun savePins(ctx: android.content.Context, pins: List<String>) =
    Prefs.sp(ctx).edit().putString("home_pins", pins.joinToString(",")).apply()

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeRoot(signal: Int) {
    val ctx = LocalContext.current
    val theme by Repo.active.collectAsState()
    val fontVersion by Fonts.version.collectAsState()
    val family = remember(theme.font, fontVersion) { Fonts.composeFamily(theme.font) }

    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var drawer by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val pins: SnapshotStateList<String> = remember { mutableStateListOf<String>().also { it.addAll(loadPins(ctx)) } }

    LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { Apps.load(ctx) } }
    LaunchedEffect(signal) {
        drawer = false
        query = ""
        if (signal > 0) apps = withContext(Dispatchers.IO) { Apps.load(ctx, force = true) }
    }
    BackHandler(enabled = drawer) { drawer = false; query = "" }

    val dock = remember(apps, theme.launcher.dock) {
        if (apps.isEmpty()) emptyList() else Apps.dockFor(ctx, theme.launcher.dock)
    }
    val pinned = remember(apps, pins.toList()) { pins.mapNotNull { p -> apps.firstOrNull { it.pkg == p } } }
    val labelColor = Color(Col.parse(theme.icons.labelColor, android.graphics.Color.WHITE))

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AuraMaterial(family) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = {
                            ctx.startActivity(Intent(ctx, MainActivity::class.java))
                        })
                    }
                    .pointerInput(Unit) {
                        var total = 0f
                        detectVerticalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = { if (total < -120f) drawer = true },
                            onVerticalDrag = { _, dy -> total += dy }
                        )
                    }
            ) {
                // ── Home page ──
                BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
                    val maxW = maxWidth
                    val maxH = maxHeight
                    theme.widgets.forEach { w ->
                        val wDp = minOf(w.w.dp, maxW - 24.dp)
                        val hDp = wDp * (w.h.toFloat() / w.w.toFloat())
                        WidgetView(
                            w,
                            Modifier
                                .offset(x = maxW * w.x - wDp / 2, y = maxH * w.y)
                                .size(wDp, hDp),
                            font = theme.font
                        )
                    }
                }

                // pinned apps
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 120.dp, start = 16.dp, end = 16.dp)
                ) {
                    if (pinned.isEmpty() && apps.isNotEmpty()) {
                        Text(
                            "اضغط مطولاً على أي تطبيق في الدرج لتثبيته هنا",
                            color = Color.White.copy(alpha = 0.55f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        val cols = theme.launcher.columns
                        val rows = (pinned.size + cols - 1) / cols
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(cols),
                            modifier = Modifier.height((rows * 96).coerceAtMost(96 * 3).dp),
                            userScrollEnabled = false,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pinned.take(cols * 3), key = { it.pkg + it.cls }) { a ->
                                AppCell(a, theme, labelColor, 54, onClick = { Apps.launch(ctx, a) }, onLong = {
                                    pins.remove(a.pkg); savePins(ctx, pins)
                                })
                            }
                        }
                    }
                }

                // dock
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black.copy(alpha = 0.32f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(32.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dock.take(4).forEach { a ->
                        Box(Modifier.clickable { Apps.launch(ctx, a) }) { AppIcon(a, theme.icons, 52.dp) }
                    }
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(AuraColors.Brand)
                            .clickable { drawer = true },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Apps, "التطبيقات", tint = Color.White) }
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable { ctx.startActivity(Intent(ctx, MainActivity::class.java)) },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Palette, "Aura", tint = Color.White) }
                }

                // ── Drawer ──
                AnimatedVisibility(
                    visible = drawer,
                    enter = fadeIn() + slideInVertically { it / 6 },
                    exit = fadeOut() + slideOutVertically { it / 6 }
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.62f))
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(horizontal = 14.dp)
                            .pointerInput(Unit) { detectTapGestures { } }
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            placeholder = { Text("ابحث عن تطبيق…", color = Color.White.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.6f)) },
                            shape = RoundedCornerShape(26.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuraColors.Violet,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        val list = remember(apps, query) {
                            if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(theme.launcher.columns),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(list, key = { it.pkg + it.cls }) { a ->
                                AppCell(a, theme, labelColor, 56, onClick = {
                                    drawer = false; query = ""
                                    Apps.launch(ctx, a)
                                }, onLong = {
                                    if (a.pkg in pins) pins.remove(a.pkg) else pins.add(a.pkg)
                                    savePins(ctx, pins)
                                    Actions.toast(ctx, if (a.pkg in pins) "تم التثبيت في الشاشة الرئيسية" else "تمت الإزالة")
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCell(
    a: AppEntry,
    theme: app.aura.themer.model.AuraTheme,
    labelColor: Color,
    iconDp: Int,
    onClick: () -> Unit,
    onLong: () -> Unit
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLong)
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIcon(a, theme.icons, iconDp.dp)
        if (theme.icons.showLabels) {
            Text(
                a.label,
                color = labelColor,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp)
            )
        }
    }
}
