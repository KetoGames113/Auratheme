package app.aura.themer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import app.aura.themer.data.AppEntry
import app.aura.themer.data.IconEngine
import app.aura.themer.model.IconStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppIcon(entry: AppEntry, style: IconStyle, size: Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val bmp by produceState<ImageBitmap?>(null, entry, style, px) {
        value = withContext(Dispatchers.Default) {
            try {
                IconEngine.render(ctx, entry, style, px).asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
    val b = bmp
    if (b != null) Image(b, contentDescription = entry.label, modifier = modifier.size(size))
    else Box(modifier.size(size))
}
