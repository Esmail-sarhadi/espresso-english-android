package ir.esmail.espresso.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.esmail.espresso.BuildConfig
import ir.esmail.espresso.data.ProviderReport
import ir.esmail.espresso.data.Reach
import ir.esmail.espresso.data.ThemeMode
import ir.esmail.espresso.ui.AppViewModel
import ir.esmail.espresso.ui.Fa
import ir.esmail.espresso.ui.components.FilterPill
import ir.esmail.espresso.ui.theme.espresso
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    vm: AppViewModel,
    bottomInset: Dp,
) {
    val palette = espresso
    val settings by vm.settings.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    var localScale by remember(settings.fontScale) { mutableStateOf(settings.fontScale) }
    var checking by remember { mutableStateOf(false) }
    var showTour by remember { mutableStateOf(false) }
    var reports by remember { mutableStateOf<List<ProviderReport>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(top = 18.dp, bottom = bottomInset + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                Text(
                    text = "تنظیمات",
                    style = MaterialTheme.typography.headlineLarge,
                    color = palette.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "همهٔ تغییرها بلافاصله ذخیره می‌شوند",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textTertiary,
                )
                Spacer(Modifier.height(6.dp))
            }
        }

        item {
            SettingsCard(title = "ظاهر") {
                Text(
                    text = "پوسته",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterPill("تیره", settings.theme == ThemeMode.DARK, onClick = { vm.setTheme(ThemeMode.DARK) })
                    FilterPill("روشن", settings.theme == ThemeMode.LIGHT, onClick = { vm.setTheme(ThemeMode.LIGHT) })
                    FilterPill("سیستم", settings.theme == ThemeMode.SYSTEM, onClick = { vm.setTheme(ThemeMode.SYSTEM) })
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "اندازهٔ متن",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = Fa.digits("${(localScale * 100).toInt()}%"),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textTertiary,
                    )
                }
                Slider(
                    value = localScale,
                    onValueChange = { localScale = it },
                    onValueChangeFinished = { vm.setFontScale(localScale) },
                    valueRange = 0.85f..1.35f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = palette.accent,
                        activeTrackColor = palette.accent,
                        inactiveTrackColor = palette.surfaceHigh,
                    ),
                )
            }
        }

        item {
            SettingsCard(title = "پخش") {
                Text(
                    text = "سرعت پیش‌فرض",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.75f, 1f, 1.25f, 1.5f).forEach { value ->
                        FilterPill(
                            text = Fa.speed(value),
                            selected = settings.speed == value,
                            onClick = { vm.setSpeed(value) },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                ToggleRow(
                    title = "پخش خودکار درس بعدی",
                    subtitle = "وقتی درس تمام شد، درس بعدی شروع شود",
                    checked = settings.autoPlayNext,
                    onCheckedChange = { vm.setAutoPlayNext(it) },
                )
                ToggleRow(
                    title = "همراهی خودکار متن",
                    subtitle = "متن با صدا هم‌زمان جلو برود",
                    checked = settings.autoScroll,
                    onCheckedChange = { vm.setAutoScroll(it) },
                )
                ToggleRow(
                    title = "صداهای رابط کاربری",
                    subtitle = "صدای درست، نادرست و ذخیرهٔ واژه",
                    checked = settings.soundEffects,
                    onCheckedChange = { vm.setSoundEffects(it) },
                )
                ToggleRow(
                    title = "صدای طبیعی دستگاه",
                    subtitle = "بهجای صدای جنریت شده، از صدای گویندهٔ خود گوشی استفاده شود",
                    checked = settings.useNaturalVoice,
                    onCheckedChange = { vm.setNaturalVoice(it) },
                )
            }
        }

        item {
            SettingsCard(title = "یادگیری تو") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat(Fa.num(stats.wordsSaved), "واژه", Modifier.weight(1f))
                    MiniStat(Fa.num(stats.lessonsFinished), "درس کامل", Modifier.weight(1f))
                    MiniStat(Fa.num(stats.averageScore) + "٪", "میانگین آزمون", Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.danger.copy(alpha = 0.10f))
                        .border(1.dp, palette.danger.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                        .clickable { confirmClear = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = palette.danger,
                        modifier = Modifier.width(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "پاک کردن همهٔ داده‌های یادگیری",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.danger,
                    )
                }
            }
        }

        item {
            SettingsCard(title = "راهنما") {
                Text(
                    text = "تور آموزشی برنامه را یک‌بار دیگر ببین؛ با امکانات اصلی در چهار صفحه آشنا می‌شوی.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.accentSoft)
                        .clickable { showTour = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text("دیدن دوبارهٔ تور آموزشی", style = MaterialTheme.typography.labelLarge, color = palette.accent)
                }
            }
        }

        item {
            SettingsCard(title = "پشتیبانگیری") {
                Text(
                    text = "کپی از تمام واژهها، عبارتها، پیشرفت و آمار خود بگیر و با هر برنامهٔ دیگری به اشتراک بگذار.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.accentSoft)
                        .clickable {
                            vm.exportData { result ->
                                result.onSuccess { json ->
                                    val file = java.io.File(context.cacheDir, "espresso-backup.json")
                                    file.writeText(json, Charsets.UTF_8)
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context, "${context.packageName}.fileprovider", file,
                                    )
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    runCatching {
                                        context.startActivity(Intent.createChooser(share, "اشتراکگذاری پشتیبان"))
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text("صدور پشتیبان (JSON)", style = MaterialTheme.typography.labelLarge, color = palette.accent)
                }
            }
        }

        item {
            SettingsCard(title = "اتصال به سرویس‌های آنلاین") {
                Text(
                    text = "بررسی کن کدام سرویس ترجمه و ترانه از شبکهٔ تو در دسترس است. اگر همه قرمز بودند، VPN یا اینترنت دیگری را امتحان کن.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.accentSoft)
                        .clickable(enabled = !checking) {
                            scope.launch {
                                checking = true
                                reports = vm.lyrics.diagnose()
                                checking = false
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        if (checking) "در حال بررسی…" else "بررسی اتصال",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.accent,
                    )
                }
                if (reports.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    reports.forEach { report ->
                        val color = when (report.reach) {
                            Reach.OK -> palette.mint
                            Reach.EMPTY -> palette.textTertiary
                            Reach.BLOCKED -> palette.danger
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(report.name, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                            Spacer(Modifier.weight(1f))
                            Text(
                                when (report.reach) {
                                    Reach.OK -> "در دسترس · ${report.detail}"
                                    Reach.EMPTY -> "خالی"
                                    Reach.BLOCKED -> "مسدود / بی‌پاسخ"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = color,
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(title = "درباره") {
                Text(
                    text = "Espresso English",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "نسخهٔ ${Fa.digits(BuildConfig.VERSION_NAME)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textTertiary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "درس‌ها از پادکست رسمی Espresso English نوشتهٔ Shayna Oliveira گرفته شده‌اند. " +
                        "متن‌ها با تشخیص گفتار هم‌زمان شده و ترجمهٔ فارسی، واژه‌نامه و آزمون‌ها در همین برنامه ساخته شده‌اند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.accentSoft)
                        .clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.espressoenglish.net/")),
                                )
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "espressoenglish.net",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.accent,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.width(14.dp),
                    )
                }
            }
        }
    }

    if (showTour) {
        OnboardingTour(onFinish = { showTour = false })
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("پاک کردن داده‌ها") },
            text = {
                Text("همهٔ واژه‌های ذخیره‌شده، پیشرفت درس‌ها و نتیجهٔ آزمون‌ها پاک می‌شود. این کار برگشت‌پذیر نیست.")
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearLearningData()
                    confirmClear = false
                }) {
                    Text("پاک کن", color = palette.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("انصراف", color = palette.textSecondary)
                }
            },
            containerColor = palette.surface,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    val palette = espresso
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(22.dp))
            .padding(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = palette.textTertiary,
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val palette = espresso
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = palette.textTertiary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.accentInk,
                checkedTrackColor = palette.accent,
                uncheckedThumbColor = palette.textTertiary,
                uncheckedTrackColor = palette.surfaceHigh,
            ),
        )
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    val palette = espresso
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceHigh)
            .padding(12.dp),
    ) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = palette.textTertiary)
    }
}
