package app.aura.themer.ui

import android.graphics.Color as AColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.aura.themer.model.WallpaperSpec
import app.aura.themer.model.WidgetSpec
import app.aura.themer.render.Battery
import app.aura.themer.render.Col
import app.aura.themer.render.WallpaperRenderer
import app.aura.themer.render.WidgetRenderer

/** Seconds since this composable entered composition, updated every frame (only when [running]). */
@Composable
fun rememberClock(running: Boolean = true): androidx.compose.runtime.State<Float> {
    val t = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        if (running) {
            val start = withFrameNanos { it }
            while (true) {
                withFrameNanos { t.floatValue = (it - start) / 1_000_000_000f }
            }
        }
    }
    return t
}

@Composable
fun WallpaperView(spec: WallpaperSpec, modifier: Modifier = Modifier, animate: Boolean = true) {
    val animated = animate && WallpaperRenderer.isAnimated(spec)
    val t = rememberClock(animated)
    Canvas(modifier) {
        val tt = t.value
        drawIntoCanvas { c ->
            WallpaperRenderer.draw(c.nativeCanvas, size.width.toInt(), size.height.toInt(), spec, tt + 6f)
        }
    }
}

@Composable
fun WidgetView(spec: WidgetSpec, modifier: Modifier = Modifier, font: String? = null, animate: Boolean = true) {
    val ctx = LocalContext.current
    val animated = animate && WidgetRenderer.hasAnimation(spec)
    val t = rememberClock(animated)
    var bat by remember { mutableStateOf(Battery.read(ctx)) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            bat = Battery.read(ctx)
        }
    }
    var minuteTick by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            minuteTick += 1f
        }
    }
    Canvas(modifier) {
        val tt = t.value
        val tick = minuteTick // read so the canvas redraws every second (keeps the clock correct)
        drawIntoCanvas { c ->
            WidgetRenderer.draw(
                c.nativeCanvas, size.width, size.height, spec,
                WidgetRenderer.Env(System.currentTimeMillis() + (if (tick < 0f) 1L else 0L), tt, bat.first, bat.second, font)
            )
        }
    }
}

private val swatches = listOf(
    "#FFFFFF", "#000000", "#7C5CFF", "#00E5FF", "#FF4DA6", "#FF6A3D", "#FFC371", "#FFD27A",
    "#3DDC84", "#1E6BFF", "#22D3EE", "#9AA0A6", "#07070F", "#161625", "#2A1245", "#061826"
)

@Composable
fun ColorPickerDialog(initial: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val start = Col.parse(initial, AColor.WHITE)
    val hsv0 = FloatArray(3).also { AColor.colorToHSV(start, it) }
    var h by remember { mutableFloatStateOf(hsv0[0]) }
    var s by remember { mutableFloatStateOf(hsv0[1]) }
    var v by remember { mutableFloatStateOf(hsv0[2]) }
    var a by remember { mutableFloatStateOf(AColor.alpha(start) / 255f) }
    var hex by remember { mutableStateOf(Col.hex(start)) }

    fun current(): Int = AColor.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))
    fun sync() { hex = Col.hex(current()) }
    fun applyHex(text: String) {
        val c = Col.parse(text, Int.MIN_VALUE)
        if (c == Int.MIN_VALUE) return
        val tmp = FloatArray(3)
        AColor.colorToHSV(c, tmp)
        h = tmp[0]; s = tmp[1]; v = tmp[2]; a = AColor.alpha(c) / 255f
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(26.dp))
                .background(AuraColors.Surface)
                .border(1.dp, AuraColors.Stroke, RoundedCornerShape(26.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF3A3A4A))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(Color(current()))
                )
            }
            val hueColors = (0..6).map { Color(AColor.HSVToColor(floatArrayOf(it * 60f, 1f, 1f))) }
            Text("اللون", color = AuraColors.Muted, style = MaterialTheme.typography.labelSmall)
            Slider(
                value = h, onValueChange = { h = it; sync() }, valueRange = 0f..360f,
                colors = SliderDefaults.colors(thumbColor = Color(AColor.HSVToColor(floatArrayOf(h, 1f, 1f))), activeTrackColor = hueColors[1])
            )
            Text("التشبّع", color = AuraColors.Muted, style = MaterialTheme.typography.labelSmall)
            Slider(value = s, onValueChange = { s = it; sync() }, valueRange = 0f..1f)
            Text("السطوع", color = AuraColors.Muted, style = MaterialTheme.typography.labelSmall)
            Slider(value = v, onValueChange = { v = it; sync() }, valueRange = 0f..1f)
            Text("الشفافية", color = AuraColors.Muted, style = MaterialTheme.typography.labelSmall)
            Slider(value = a, onValueChange = { a = it; sync() }, valueRange = 0f..1f)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(swatches) { sw ->
                    Dot(Color(Col.parse(sw)), 30) { applyHex(sw); sync() }
                }
            }
            OutlinedTextField(
                value = hex,
                onValueChange = { hex = it; applyHex(it) },
                singleLine = true,
                label = { Text("HEX") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AuraColors.Violet,
                    unfocusedBorderColor = AuraColors.Stroke
                )
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton("إلغاء", modifier = Modifier.weight(1f)) { onDismiss() }
                GradientButton("اختيار", modifier = Modifier.weight(1f)) {
                    onPick(Col.hex(current()))
                    onDismiss()
                }
            }
        }
    }
}

/** A tappable colour swatch + hex label that opens the picker. */
@Composable
fun ColorField(label: String, value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    LabeledRow(label) {
        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { open = true }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(value.uppercase(), color = AuraColors.Muted, style = MaterialTheme.typography.labelSmall)
            Dot(Color(Col.parse(value)), 28)
        }
    }
    if (open) ColorPickerDialog(value, { open = false }, onChange)
}

@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = AuraColors.Muted)
            Text(String.format("%.2f", value), style = MaterialTheme.typography.bodySmall, color = AuraColors.Muted)
        }
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
    }
}

@Composable
fun ChipsRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { (key, label) ->
            Chip(label, key == selected) { onSelect(key) }
        }
    }
}

@Composable
fun TextInput(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    singleLine: Boolean = minLines == 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        minLines = minLines,
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AuraColors.Violet,
            unfocusedBorderColor = AuraColors.Stroke,
            focusedTextColor = AuraColors.Text,
            unfocusedTextColor = AuraColors.Text
        )
    )
}

@Composable
fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = AuraColors.Muted,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}
