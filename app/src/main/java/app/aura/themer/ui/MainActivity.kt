package app.aura.themer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.aura.themer.data.Fonts
import app.aura.themer.data.Repo
import app.aura.themer.widget.AuraWidgets
import kotlinx.coroutines.delay

private enum class Tab(val label: String, val icon: ImageVector) {
    Themes("الثيمات", Icons.Rounded.Palette),
    Wallpaper("الخلفية", Icons.Rounded.Wallpaper),
    Style("الشكل", Icons.Rounded.Style),
    Widgets("الويدجت", Icons.Rounded.Widgets),
    Ai("الذكاء", Icons.Rounded.AutoAwesome)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { StudioRoot() }
    }
}

@Composable
private fun StudioRoot() {
    val theme by Repo.active.collectAsState()
    val fontVersion by Fonts.version.collectAsState()
    val family = remember(theme.font, fontVersion) { Fonts.composeFamily(theme.font) }
    val ctx = LocalContext.current

    // Keep placed system widgets in sync with whatever is being edited.
    LaunchedEffect(theme) {
        delay(700)
        AuraWidgets.updateAll(ctx)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AuraMaterial(family) {
            var tab by rememberSaveable { mutableStateOf(0) }
            AuraBackdrop(Modifier.fillMaxSize()) {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        NavigationBar(containerColor = AuraColors.Surface.copy(alpha = 0.94f)) {
                            Tab.values().forEachIndexed { i, t ->
                                NavigationBarItem(
                                    selected = tab == i,
                                    onClick = { tab = i },
                                    icon = { Icon(t.icon, contentDescription = t.label) },
                                    label = { Text(t.label, style = MaterialTheme.typography.labelSmall) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        selectedTextColor = Color.White,
                                        indicatorColor = AuraColors.Violet.copy(alpha = 0.45f),
                                        unselectedIconColor = AuraColors.Muted,
                                        unselectedTextColor = AuraColors.Muted
                                    )
                                )
                            }
                        }
                    }
                ) { pad ->
                    Column(Modifier.padding(pad).fillMaxSize()) {
                        Header(themeName = theme.name)
                        Box(Modifier.fillMaxSize()) {
                            AnimatedContent(
                                targetState = tab,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "tabs"
                            ) { t ->
                                when (Tab.values()[t]) {
                                    Tab.Themes -> ThemesTab(onOpenAi = { tab = Tab.Ai.ordinal })
                                    Tab.Wallpaper -> WallpaperTab()
                                    Tab.Style -> StyleTab()
                                    Tab.Widgets -> WidgetsTab(onOpenAi = { tab = Tab.Ai.ordinal })
                                    Tab.Ai -> AiTab(onDone = { tab = Tab.Themes.ordinal })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(themeName: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Aura", style = MaterialTheme.typography.headlineMedium, color = AuraColors.Text)
            Text("الثيم الحالي: $themeName", style = MaterialTheme.typography.bodySmall, color = AuraColors.Muted)
        }
    }
}
