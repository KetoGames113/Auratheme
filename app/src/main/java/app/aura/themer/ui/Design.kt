package app.aura.themer.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AuraColors {
    val Bg = Color(0xFF07070F)
    val Surface = Color(0xFF12121F)
    val Surface2 = Color(0xFF1B1B2E)
    val Violet = Color(0xFF7C5CFF)
    val Cyan = Color(0xFF00E5FF)
    val Pink = Color(0xFFFF4DA6)
    val Text = Color(0xFFF4F4FF)
    val Muted = Color(0xFF9A9AB8)
    val Stroke = Color(0x22FFFFFF)
    val Brand = Brush.linearGradient(listOf(Violet, Cyan))
    val BrandWarm = Brush.linearGradient(listOf(Violet, Pink))
}

private fun scheme(): ColorScheme = darkColorScheme(
    primary = AuraColors.Violet,
    onPrimary = Color.White,
    secondary = AuraColors.Cyan,
    onSecondary = Color.Black,
    tertiary = AuraColors.Pink,
    background = AuraColors.Bg,
    onBackground = AuraColors.Text,
    surface = AuraColors.Surface,
    onSurface = AuraColors.Text,
    surfaceVariant = AuraColors.Surface2,
    onSurfaceVariant = AuraColors.Muted,
    outline = AuraColors.Stroke
)

private fun typography(family: FontFamily): Typography {
    fun s(size: Int, w: FontWeight = FontWeight.Normal) =
        TextStyle(fontFamily = family, fontSize = size.sp, fontWeight = w, color = AuraColors.Text)
    return Typography(
        displayLarge = s(40, FontWeight.Bold),
        headlineLarge = s(30, FontWeight.Bold),
        headlineMedium = s(24, FontWeight.Bold),
        titleLarge = s(20, FontWeight.Bold),
        titleMedium = s(16, FontWeight.SemiBold),
        titleSmall = s(14, FontWeight.SemiBold),
        bodyLarge = s(16),
        bodyMedium = s(14),
        bodySmall = s(12),
        labelLarge = s(14, FontWeight.SemiBold),
        labelMedium = s(12, FontWeight.Medium),
        labelSmall = s(11, FontWeight.Medium)
    )
}

@Composable
fun AuraMaterial(family: FontFamily = FontFamily.Default, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme(), typography = typography(family), content = content)
}

@Composable
fun AuraBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val tr = rememberInfiniteTransition(label = "backdrop")
    val a by tr.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000), RepeatMode.Reverse), label = "a"
    )
    Box(modifier.background(AuraColors.Bg)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(AuraColors.Violet.copy(alpha = 0.28f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(200f + 500f * a, 120f),
                        radius = 900f
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(AuraColors.Cyan.copy(alpha = 0.14f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(900f - 500f * a, 1500f),
                        radius = 1000f
                    )
                )
        )
        content()
    }
}



@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val border = if (selected) BorderStroke(1.6.dp, AuraColors.Brand) else BorderStroke(1.dp, AuraColors.Stroke)
    var m = modifier
        .clip(shape)
        .background(AuraColors.Surface.copy(alpha = 0.82f))
        .border(border, shape)
    if (onClick != null) m = m.clickable { onClick() }
    Box(m) { content() }
}

@Composable
fun SectionTitle(text: String, sub: String? = null) {
    Column(Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = AuraColors.Text)
        if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = AuraColors.Muted)
    }
}

@Composable
fun GradientButton(
    text: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    warm: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .clip(shape)
            .background(if (warm) AuraColors.BrandWarm else AuraColors.Brand)
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Box(Modifier.width(8.dp))
        }
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SoftButton(
    text: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val tint = if (danger) AuraColors.Pink else AuraColors.Text
    Row(
        modifier
            .clip(shape)
            .background(AuraColors.Surface2)
            .border(1.dp, AuraColors.Stroke, shape)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Box(Modifier.width(8.dp))
        }
        Text(text, color = tint, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) AuraColors.Violet.copy(alpha = 0.28f) else AuraColors.Surface2)
            .border(1.dp, if (selected) AuraColors.Violet else AuraColors.Stroke, shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            color = if (selected) Color.White else AuraColors.Muted,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun Dot(color: Color, size: Int = 26, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    var m = Modifier
        .size(size.dp)
        .clip(CircleShape)
        .background(color)
        .border(if (selected) 2.dp else 1.dp, if (selected) Color.White else AuraColors.Stroke, CircleShape)
    if (onClick != null) m = m.clickable { onClick() }
    Box(m)
}

@Composable
fun LabeledRow(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AuraColors.Text)
        content()
    }
}
