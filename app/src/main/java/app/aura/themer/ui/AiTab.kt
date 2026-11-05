package app.aura.themer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.aura.themer.ai.AiImport
import app.aura.themer.ai.AiMode
import app.aura.themer.ai.AiPrompts
import app.aura.themer.ai.ImportResult
import app.aura.themer.data.Repo

private val modes = listOf(
    AiMode.THEME.name to "ثيم كامل",
    AiMode.WIDGET.name to "ويدجت",
    AiMode.MODIFY.name to "تعديل ثيمي الحالي"
)

@Composable
fun AiTab(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val theme by Repo.active.collectAsState()
    var mode by rememberSaveable { mutableStateOf(AiMode.THEME.name) }
    var request by rememberSaveable { mutableStateOf("") }
    var reply by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val text = ctx.contentResolver.openInputStream(uri)?.use { s ->
                    s.readBytes().let { b -> String(if (b.size > 600_000) b.copyOf(600_000) else b, Charsets.UTF_8) }
                }.orEmpty()
                reply = text
                error = null
            } catch (e: Exception) {
                error = "تعذّرت قراءة الملف"
            }
        }
    }

    fun prompt(): String = AiPrompts.build(AiMode.valueOf(mode), request, theme)

    fun doImport() {
        when (val r = AiImport.parse(reply)) {
            is ImportResult.Failure -> error = r.message
            is ImportResult.ThemeResult -> {
                error = null
                if (mode == AiMode.MODIFY.name) {
                    Repo.updateActive { cur ->
                        r.theme.copy(
                            id = cur.id,
                            font = cur.font,
                            icons = r.theme.icons.copy(pack = cur.icons.pack, overrides = cur.icons.overrides),
                            launcher = r.theme.launcher.copy(dock = cur.launcher.dock)
                        )
                    }
                    Actions.toast(ctx, "تم تعديل الثيم ✓")
                } else {
                    Repo.addTheme(r.theme)
                    Actions.toast(ctx, "تمت إضافة الثيم وتفعيله ✓")
                }
                reply = ""
                onDone()
            }
            is ImportResult.WidgetResult -> {
                error = null
                Repo.updateActive { it.copy(widgets = it.widgets + r.widget) }
                Actions.toast(ctx, "تمت إضافة الويدجت ✓ (تبويب الويدجت)")
                reply = ""
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("١) جهّز البرومبت", "اوصف اللي تبيه، وانسخ البرومبت وأعطه لأي ذكاء اصطناعي (ChatGPT / Claude / Gemini…)")
                ChipsRow(modes, mode) { mode = it }
                TextInput(
                    request, { request = it },
                    when (mode) {
                        AiMode.WIDGET.name -> "اوصف الويدجت (مثال: ساعة نيون مع حلقة بطارية وردية)"
                        AiMode.MODIFY.name -> "وش تبي تغيّر؟ (مثال: خلّه أكثر دفئاً وزوّد حركة الخلفية)"
                        else -> "اوصف الثيم (مثال: سايبربانك وردي وأزرق، أيقونات دائرية، ويدجت ساعة)"
                    },
                    minLines = 3
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GradientButton("نسخ البرومبت", Icons.Rounded.ContentCopy) {
                        Actions.copy(ctx, "Aura prompt", prompt())
                    }
                    SoftButton("مشاركة", Icons.Rounded.Share) {
                        Actions.share(ctx, "أرسل البرومبت للذكاء الاصطناعي", prompt())
                    }
                }
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("٢) استورد النتيجة", "الصق رد الذكاء الاصطناعي، أو اختر ملف .json / .txt حفظته")
                TextInput(reply, { reply = it; error = null }, "الصق الـ JSON هنا", minLines = 6)
                error?.let { Text(it, color = AuraColors.Pink, style = MaterialTheme.typography.bodySmall) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SoftButton("لصق", Icons.Rounded.ContentPaste) {
                        reply = Actions.paste(ctx)
                        error = null
                    }
                    SoftButton("اختيار ملف", Icons.Rounded.FolderOpen) { pickFile.launch("*/*") }
                }
                GradientButton("تشغيل الثيم / الويدجت", Icons.Rounded.AutoAwesome, Modifier.fillMaxWidth(), warm = true) {
                    if (reply.isBlank()) error = "الصق رد الذكاء الاصطناعي أو اختر ملف أولاً" else doImport()
                }
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("نصائح")
                Hint("• الخلفيات الحية (شفق، نجوم، أمواج…) والويدجت المتحركة تنبني من الألوان فقط — الذكاء ما يحتاج ملفات.")
                Hint("• الصور والفيديو والخطوط تضيفها يدوياً من تبويب الخلفية والشكل.")
                Hint("• إذا الرد جاء ناقص أو فيه شرح، لا تقلق: التطبيق يلقط الـ JSON من داخل النص تلقائياً.")
                Hint("• تقدر تصدّر أي ثيم من تبويب الثيمات (زر المشاركة) وتعطيه لصديقك.")
            }
        }
        Box(Modifier.height(24.dp))
    }
}
