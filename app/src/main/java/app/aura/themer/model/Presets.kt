package app.aura.themer.model

/** Ready-made widget designs (also used inside the preset themes). */
object Templates {

    fun neonClock(color: String = "#00E5FF", border: String = "#7C5CFF", y: Float = 0.2f) = WidgetSpec(
        name = "ساعة نيون", w = 300, h = 150, x = 0.5f, y = y,
        background = "#66120A2E", border = border, corner = 34f,
        elements = listOf(
            WidgetElement(kind = "time", x = 0.5f, y = 0.42f, size = 66f, color = color, bold = true, anim = "glow"),
            WidgetElement(kind = "date", x = 0.5f, y = 0.82f, size = 16f, color = "#CCFFFFFF")
        )
    )

    fun ringBattery(accent: String = "#FF4DA6", y: Float = 0.2f) = WidgetSpec(
        name = "ساعة + حلقة البطارية", w = 330, h = 140, x = 0.5f, y = y,
        background = "#88161625", border = "#22FFFFFF", corner = 30f,
        elements = listOf(
            WidgetElement(kind = "time", x = 0.3f, y = 0.42f, size = 54f, color = "#FFFFFF", bold = true),
            WidgetElement(kind = "date", x = 0.3f, y = 0.8f, size = 14f, color = "#B3FFFFFF"),
            WidgetElement(kind = "ring", text = "battery", x = 0.82f, y = 0.5f, w = 80f, size = 8f, color = accent),
            WidgetElement(kind = "battery", x = 0.82f, y = 0.5f, size = 15f, color = "#FFFFFF", bold = true)
        )
    )

    fun giantClock(accent: String = "#FF4DA6", y: Float = 0.2f) = WidgetSpec(
        name = "ساعة عملاقة", w = 340, h = 210, x = 0.5f, y = y,
        background = "#00000000", corner = 0f,
        elements = listOf(
            WidgetElement(kind = "time", format = "hh", x = 0.5f, y = 0.32f, size = 96f, color = "#FFFFFF", bold = true, anim = "float"),
            WidgetElement(kind = "time", format = "mm", x = 0.5f, y = 0.74f, size = 96f, color = accent, bold = true, anim = "float")
        )
    )

    fun dateCard(accent: String = "#00E5FF", y: Float = 0.42f) = WidgetSpec(
        name = "بطاقة التاريخ", w = 280, h = 120, x = 0.5f, y = y,
        background = "#66000000", border = "#22FFFFFF", corner = 28f,
        elements = listOf(
            WidgetElement(kind = "weekday", x = 0.5f, y = 0.3f, size = 32f, color = "#FFFFFF", bold = true),
            WidgetElement(kind = "date", format = "d MMMM yyyy", x = 0.5f, y = 0.62f, size = 16f, color = "#CCFFFFFF"),
            WidgetElement(kind = "line", x = 0.5f, y = 0.85f, w = 70f, h = 3f, color = accent, anim = "pulse")
        )
    )

    fun circleClock(accent: String = "#7C5CFF", y: Float = 0.2f) = WidgetSpec(
        name = "ساعة دائرية", w = 200, h = 200, x = 0.5f, y = y,
        background = "#66000000", border = "#22FFFFFF", corner = 100f,
        elements = listOf(
            WidgetElement(kind = "ring", text = "seconds", x = 0.5f, y = 0.5f, w = 182f, size = 6f, color = accent),
            WidgetElement(kind = "time", x = 0.5f, y = 0.46f, size = 44f, color = "#FFFFFF", bold = true),
            WidgetElement(kind = "weekday", x = 0.5f, y = 0.7f, size = 14f, color = "#CCFFFFFF")
        )
    )

    fun quote(color: String = "#FFFFFF", y: Float = 0.42f) = WidgetSpec(
        name = "عبارة اليوم", w = 300, h = 100, x = 0.5f, y = y,
        background = "#44000000", corner = 26f,
        elements = listOf(
            WidgetElement(kind = "text", text = "ابتسم ✨ فالحياة أجمل", x = 0.5f, y = 0.5f, size = 22f, color = color, bold = true, anim = "pulse")
        )
    )

    fun minimalTime(y: Float = 0.2f) = WidgetSpec(
        name = "وقت بسيط", w = 240, h = 100, x = 0.5f, y = y,
        background = "#00000000", corner = 0f,
        elements = listOf(
            WidgetElement(kind = "time", x = 0.5f, y = 0.5f, size = 72f, color = "#FFFFFF")
        )
    )

    fun all(): List<WidgetSpec> = listOf(
        neonClock(), ringBattery(), giantClock(), dateCard(), circleClock(), quote(), minimalTime()
    )

    fun defaultClock() = neonClock()
}

object Presets {

    private fun neon() = AuraTheme(
        id = "preset_neon", name = "نيون سايبر", author = "Aura",
        palette = Palette("#7C5CFF", "#00E5FF", "#0B0B14", "#161625", "#FFFFFF", "#FF4DA6"),
        wallpaper = WallpaperSpec("aurora", listOf("#07070F", "#5B2BFF", "#00E5FF", "#FF2E93"), speed = 0.8f),
        schedule = listOf(ScheduleSlot()),
        icons = IconStyle(shape = "squircle"),
        notifications = NotifStyle(false, "glass", "#CC161625", "#FFFFFF", "#CCFFFFFF", "#7C5CFF", 28f),
        widgets = listOf(Templates.neonClock(y = 0.18f), Templates.dateCard(y = 0.38f))
    )

    private fun amoled() = AuraTheme(
        id = "preset_amoled", name = "AMOLED أسود", author = "Aura",
        palette = Palette("#FFFFFF", "#9AA0A6", "#000000", "#0E0E0E", "#FFFFFF", "#FFFFFF"),
        wallpaper = WallpaperSpec("stars", listOf("#000000", "#000000", "#FFFFFF"), density = 0.7f, speed = 0.6f),
        icons = IconStyle(shape = "circle"),
        notifications = NotifStyle(false, "solid", "#FF0E0E0E", "#FFFFFF", "#B3FFFFFF", "#FFFFFF", 22f),
        widgets = listOf(Templates.giantClock(accent = "#9AA0A6", y = 0.2f), Templates.quote(y = 0.45f))
    )

    private fun sunset() = AuraTheme(
        id = "preset_sunset", name = "غروب دافئ", author = "Aura",
        palette = Palette("#FF6A3D", "#FFC371", "#1A0B2E", "#2A1245", "#FFF4E8", "#FF4D8D"),
        wallpaper = WallpaperSpec("waves", listOf("#1A0B2E", "#7B2D8E", "#FF6A3D", "#C2358A", "#FF9E5E"), speed = 0.7f),
        icons = IconStyle(shape = "teardrop"),
        notifications = NotifStyle(false, "glass", "#CC2A1245", "#FFF4E8", "#CCFFF4E8", "#FF6A3D", 30f),
        widgets = listOf(Templates.circleClock(accent = "#FFC371", y = 0.2f), Templates.dateCard(accent = "#FF6A3D", y = 0.42f))
    )

    private fun forest() = AuraTheme(
        id = "preset_forest", name = "غابة الأضواء", author = "Aura",
        palette = Palette("#3DDC84", "#E6FF8A", "#04140C", "#0B2418", "#EFFFF4", "#9BFFB0"),
        wallpaper = WallpaperSpec("particles", listOf("#04140C", "#9BFFB0", "#E6FF8A", "#3DDC84"), speed = 0.8f, density = 1.2f),
        icons = IconStyle(shape = "circle"),
        notifications = NotifStyle(false, "outline", "#00000000", "#EFFFF4", "#CCEFFFF4", "#3DDC84", 24f),
        widgets = listOf(Templates.ringBattery(accent = "#3DDC84", y = 0.2f), Templates.quote(color = "#E6FF8A", y = 0.4f))
    )

    private fun ocean() = AuraTheme(
        id = "preset_ocean", name = "زجاج المحيط", author = "Aura",
        palette = Palette("#1E6BFF", "#22D3EE", "#061826", "#0C2A44", "#EAF6FF", "#7DD3FC"),
        wallpaper = WallpaperSpec("rain", listOf("#061826", "#7DD3FC"), speed = 1f, density = 1f),
        icons = IconStyle(shape = "square"),
        notifications = NotifStyle(false, "glass", "#B30C2A44", "#EAF6FF", "#CCEAF6FF", "#22D3EE", 20f),
        widgets = listOf(Templates.neonClock(color = "#7DD3FC", border = "#1E6BFF", y = 0.2f), Templates.dateCard(accent = "#22D3EE", y = 0.4f))
    )

    private fun gold() = AuraTheme(
        id = "preset_gold", name = "ذهبي فاخر", author = "Aura",
        palette = Palette("#FFD27A", "#B8860B", "#0A0804", "#1A140A", "#FFF6E0", "#FFD27A"),
        wallpaper = WallpaperSpec("aurora", listOf("#0A0804", "#B8860B", "#FFD27A", "#6B4E16"), speed = 0.5f),
        icons = IconStyle(shape = "hexagon"),
        notifications = NotifStyle(false, "solid", "#F01A140A", "#FFF6E0", "#CCFFF6E0", "#FFD27A", 26f),
        widgets = listOf(Templates.giantClock(accent = "#FFD27A", y = 0.2f), Templates.dateCard(accent = "#FFD27A", y = 0.44f))
    )

    fun all(): List<AuraTheme> = listOf(neon(), amoled(), sunset(), forest(), ocean(), gold())
}
