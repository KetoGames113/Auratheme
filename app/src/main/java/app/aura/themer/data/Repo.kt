package app.aura.themer.data

import android.content.Context
import android.content.SharedPreferences
import app.aura.themer.model.AuraTheme
import app.aura.themer.model.Presets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

object Prefs {
    fun sp(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences("aura", Context.MODE_PRIVATE)

    fun bindWidget(ctx: Context, appWidgetId: Int, specId: String) =
        sp(ctx).edit().putString("wb_$appWidgetId", specId).apply()

    fun widgetBinding(ctx: Context, appWidgetId: Int): String? =
        sp(ctx).getString("wb_$appWidgetId", null)

    fun unbindWidget(ctx: Context, appWidgetId: Int) =
        sp(ctx).edit().remove("wb_$appWidgetId").apply()

    fun setPendingWidget(ctx: Context, specId: String?) {
        val e = sp(ctx).edit()
        if (specId == null) e.remove("pending_widget").remove("pending_ts")
        else e.putString("pending_widget", specId).putLong("pending_ts", System.currentTimeMillis())
        e.apply()
    }

    /** Pending widget spec id requested from inside the app (valid for 2 minutes). */
    fun pendingWidget(ctx: Context): String? {
        val sp = sp(ctx)
        val ts = sp.getLong("pending_ts", 0L)
        return if (System.currentTimeMillis() - ts < 120_000L) sp.getString("pending_widget", null) else null
    }
}

object Repo {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
    }

    private lateinit var ctx: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: Job? = null
    @Volatile private var ready = false

    private val _themes = MutableStateFlow<List<AuraTheme>>(emptyList())
    val themes: StateFlow<List<AuraTheme>> = _themes.asStateFlow()

    private val _active = MutableStateFlow(Presets.all().first())
    val active: StateFlow<AuraTheme> = _active.asStateFlow()

    private fun dir(): File = File(ctx.filesDir, "themes").apply { mkdirs() }
    private fun fileFor(id: String) = File(dir(), id.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json")

    @Synchronized
    fun init(context: Context) {
        if (ready) return
        ctx = context.applicationContext
        val sp = Prefs.sp(ctx)
        val loaded = dir().listFiles { f -> f.extension == "json" }
            ?.mapNotNull { f -> runCatching { json.decodeFromString(AuraTheme.serializer(), f.readText()) }.getOrNull() }
            .orEmpty()
            .toMutableList()
        if (!sp.getBoolean("seeded", false)) {
            for (p in Presets.all()) {
                if (loaded.none { it.id == p.id }) {
                    loaded.add(p)
                    write(p)
                }
            }
            sp.edit().putBoolean("seeded", true).apply()
        }
        val sorted = loaded.sortedWith(compareBy({ !it.id.startsWith("preset_") }, { it.name }))
        _themes.value = sorted
        val activeId = sp.getString("active", null)
        _active.value = sorted.firstOrNull { it.id == activeId } ?: sorted.firstOrNull() ?: Presets.all().first()
        ready = true
    }

    private fun write(t: AuraTheme) {
        runCatching { fileFor(t.id).writeText(json.encodeToString(AuraTheme.serializer(), t)) }
    }

    private fun scheduleSave(t: AuraTheme) {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(350)
            write(t)
        }
    }

    fun setActive(id: String) {
        val t = _themes.value.firstOrNull { it.id == id } ?: return
        _active.value = t
        Prefs.sp(ctx).edit().putString("active", id).apply()
        IconEngine.clearCache()
    }

    fun updateActive(block: (AuraTheme) -> AuraTheme) {
        val n = block(_active.value)
        _active.value = n
        _themes.value = _themes.value.map { if (it.id == n.id) n else it }
        scheduleSave(n)
    }

    fun addTheme(theme: AuraTheme, activate: Boolean = true): AuraTheme {
        val t = theme.copy(id = UUID.randomUUID().toString())
        _themes.value = _themes.value + t
        write(t)
        if (activate) setActive(t.id)
        return t
    }

    fun duplicate(id: String) {
        val t = _themes.value.firstOrNull { it.id == id } ?: return
        addTheme(t.copy(name = t.name + " (نسخة)"), activate = false)
    }

    fun delete(id: String) {
        if (_themes.value.size <= 1) return
        _themes.value = _themes.value.filter { it.id != id }
        runCatching { fileFor(id).delete() }
        if (_active.value.id == id) setActive(_themes.value.first().id)
    }

    fun restorePresets() {
        for (p in Presets.all()) {
            if (_themes.value.none { it.id == p.id }) {
                _themes.value = _themes.value + p
                write(p)
            }
        }
        _themes.value = _themes.value.sortedWith(compareBy({ !it.id.startsWith("preset_") }, { it.name }))
    }

    fun export(theme: AuraTheme): String = json.encodeToString(AuraTheme.serializer(), theme)
}
