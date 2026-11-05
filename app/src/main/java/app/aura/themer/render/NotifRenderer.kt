package app.aura.themer.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import app.aura.themer.data.Fonts
import app.aura.themer.model.NotifStyle
import kotlin.math.max

object NotifRenderer {

    private fun layout(text: CharSequence, paint: TextPaint, width: Int, maxLines: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, max(1, width))
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()

    /**
     * Draws a styled notification card as a bitmap.
     * widthPx: final bitmap width. compact = single title + single text line.
     */
    fun render(
        st: NotifStyle,
        font: String?,
        appName: String,
        title: String,
        text: String,
        time: String,
        icon: Bitmap?,
        widthPx: Int,
        compact: Boolean,
        density: Float
    ): Bitmap {
        val d = density
        val heightPx = ((if (compact) 68f else 116f) * d).toInt()
        val bmp = Bitmap.createBitmap(max(64, widthPx), heightPx, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        val r = st.corner * d
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val accent = Col.parse(st.accent, Color.parseColor("#7C5CFF"))
        val bg = Col.parse(st.background, Color.parseColor("#CC161625"))
        val inset = 1.5f * d
        val rect = RectF(inset, inset, w - inset, h - inset)

        when (st.style) {
            "outline" -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2f * d
                p.color = accent
                c.drawRoundRect(rect, r, r, p)
            }
            "minimal" -> {
                p.style = Paint.Style.FILL
                p.color = bg
                c.drawRoundRect(rect, r * 0.5f, r * 0.5f, p)
                p.color = accent
                c.drawRoundRect(RectF(inset, inset + 10f * d, inset + 4f * d, h - inset - 10f * d), 2f * d, 2f * d, p)
            }
            "solid" -> {
                p.style = Paint.Style.FILL
                p.color = bg
                c.drawRoundRect(rect, r, r, p)
            }
            else -> { // glass
                p.style = Paint.Style.FILL
                p.color = bg
                c.drawRoundRect(rect, r, r, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 1.2f * d
                p.color = 0x33FFFFFF
                c.drawRoundRect(rect, r, r, p)
            }
        }

        // icon
        val pad = 14f * d
        val iconSize = (if (compact) 38f else 46f) * d
        val iconLeft = pad
        val iconTop = if (compact) (h - iconSize) / 2f else pad
        p.style = Paint.Style.FILL
        val circle = Path().apply { addCircle(iconLeft + iconSize / 2f, iconTop + iconSize / 2f, iconSize / 2f, Path.Direction.CW) }
        if (icon != null) {
            c.save()
            c.clipPath(circle)
            c.drawBitmap(icon, null, RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize), Paint(Paint.FILTER_BITMAP_FLAG))
            c.restore()
        } else {
            p.color = accent
            c.drawPath(circle, p)
        }

        val tx = iconLeft + iconSize + 12f * d
        val textW = (w - tx - pad).toInt()
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Col.parse(st.titleColor, Color.WHITE)
            textSize = 15f * d
            typeface = Fonts.get(font, true)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Col.parse(st.textColor, Color.LTGRAY)
            textSize = 13f * d
            typeface = Fonts.get(font, false)
        }
        val smallPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Col.withAlpha(Col.parse(st.textColor, Color.LTGRAY), 0.8f)
            textSize = 11f * d
            typeface = Fonts.get(font, false)
        }

        var y = if (compact) 11f * d else pad - 2f * d
        if (!compact) {
            val header = if (time.isBlank()) appName else "$appName • $time"
            val hl = layout(header, smallPaint, textW, 1)
            c.save(); c.translate(tx, y); hl.draw(c); c.restore()
            y += hl.height + 3f * d
        }
        val tl = layout(title, titlePaint, textW, 1)
        c.save(); c.translate(tx, y); tl.draw(c); c.restore()
        y += tl.height + 2f * d
        val bl = layout(text, bodyPaint, textW, if (compact) 1 else 2)
        c.save(); c.translate(tx, y); bl.draw(c); c.restore()
        return bmp
    }
}
