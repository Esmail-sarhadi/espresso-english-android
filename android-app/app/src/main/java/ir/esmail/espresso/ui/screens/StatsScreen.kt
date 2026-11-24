package ir.esmail.espresso.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.esmail.espresso.data.ContentBundle
import ir.esmail.espresso.data.Dates
import ir.esmail.espresso.data.DailyListenEntity
import ir.esmail.espresso.data.LearnerStats
import ir.esmail.espresso.data.QuizResultEntity
import ir.esmail.espresso.ui.AppViewModel
import ir.esmail.espresso.ui.Fa
import ir.esmail.espresso.ui.components.SoftIconButton
import ir.esmail.espresso.ui.theme.espresso
import java.util.Calendar
import kotlin.math.max

/** Progress analytics: streaks, listening, quiz trends, course completion and daily goals. */
@Composable
fun StatsScreen(
    vm: AppViewModel,
    bundle: ContentBundle,
    bottomInset: Dp,
    onClose: () -> Unit,
) {
    val palette = espresso
    val stats by vm.stats.collectAsStateWithLifecycle()
    val daily by vm.daily.collectAsStateWithLifecycle()
    val quizzes by vm.quizzes.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    var showGoalEditor by remember { mutableStateOf(false) }
    var goalDraft by remember { mutableStateOf(stats.goalMinutes.toFloat()) }

    Column(
        Modifier.fillMaxSize().background(palette.background).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SoftIconButton(Icons.Rounded.Close, "بستن", onClose, background = Color.Transparent)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("آمار پیشرفت", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
                Text("روند یادگیری تو در یک نگاه", style = MaterialTheme.typography.bodySmall, color = palette.textTertiary)
            }
        }
        Spacer(Modifier.height(18.dp))

        // Daily goal ring
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(palette.surface)
                .border(1.dp, palette.outline, RoundedCornerShape(20.dp)).padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GoalRing(
                    progress = if (stats.goalMinutes > 0) (stats.todayMinutes.toFloat() / stats.goalMinutes).coerceIn(0f, 1f) else 0f,
                    color = palette.accent,
                    track = palette.outline,
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("هدف امروز", style = MaterialTheme.typography.labelMedium, color = palette.textTertiary)
                    Text(
                        "${Fa.num(stats.todayMinutes)} / ${Fa.num(stats.goalMinutes)} دقیقه",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary,
                    )
                    Text(
                        if (stats.todayMinutes >= stats.goalMinutes) "هدف امروز تمام شد — عالی!" else "هر دقیقه گوش دادن حساب میشود",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (stats.todayMinutes >= stats.goalMinutes) palette.mint else palette.textSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ButtonLike("تنظیم هدف روزانه", onClick = { goalDraft = stats.goalMinutes.toFloat(); showGoalEditor = true }, palette = palette)

        if (showGoalEditor) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(palette.surface)
                    .border(1.dp, palette.outline, RoundedCornerShape(16.dp)).padding(14.dp),
            ) {
                Column {
                    Text("هدف روزانه: ${Fa.num(goalDraft.toInt())} دقیقه", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                    Slider(
                        value = goalDraft,
                        onValueChange = { goalDraft = it },
                        valueRange = 5f..120f,
                        steps = 22,
                        colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ButtonLike("ذخیره", onClick = {
                            vm.setDailyGoal(goalDraft.toInt())
                            showGoalEditor = false
                        }, palette = palette, modifier = Modifier.weight(1f))
                        ButtonLike("انصراف", onClick = { showGoalEditor = false }, palette = palette, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("استریگ و فعالیgut", palette)
        StreakCalendar(daily = daily, palette = palette)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatBox("استریگ", Fa.num(stats.streakDays) + " روز", palette, Modifier.weight(1f))
            StatBox(" کل شنیده", Fa.num(stats.minutesListened) + " دقیقه", palette, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox("درسهای تمامشده", Fa.num(stats.lessonsFinished), palette, Modifier.weight(1f))
            StatBox("آزمونها", Fa.num(stats.quizzesTaken), palette, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("دقائق شنیدن — ۱۴ روز اخیر", palette)
        BarChart(daily = daily, palette = palette)

        Spacer(Modifier.height(20.dp))
        SectionHeader("نتیجهٔ آزمونها", palette)
        QuizTrend(quizzes = quizzes, palette = palette)

        Spacer(Modifier.height(20.dp))
        SectionHeader("پیشرفت دوره بر اساس سطح", palette)
        LevelProgress(bundle = bundle, progress = progress, palette = palette)

        Spacer(Modifier.height(20.dp))
        SectionHeader("لغت و تلفظ", palette)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox("واژهها", Fa.num(stats.wordsSaved), palette, Modifier.weight(1f))
            StatBox("عبارتها", Fa.num(stats.phrasesSaved), palette, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox("تمرین تلفظ", Fa.num(stats.speakingAttempts), palette, Modifier.weight(1f))
            StatBox("میانگین تلفظ", Fa.num(stats.averageSpeakingScore) + "٪", palette, Modifier.weight(1f))
        }

        Spacer(Modifier.height(bottomInset + 32.dp))
    }
}

@Composable
private fun SectionHeader(title: String, palette: ir.esmail.espresso.ui.theme.EspressoPalette) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun StatBox(label: String, value: String, palette: ir.esmail.espresso.ui.theme.EspressoPalette, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(14.dp)).padding(14.dp),
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = palette.textTertiary)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
        }
    }
}

@Composable
private fun ButtonLike(text: String, onClick: () -> Unit, palette: ir.esmail.espresso.ui.theme.EspressoPalette, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(palette.accentSoft)
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = palette.accent)
    }
}

@Composable
private fun GoalRing(progress: Float, color: Color, track: Color) {
    Canvas(Modifier.size(72.dp)) {
        val stroke = 8.dp.toPx()
        drawArc(track, 0f, 360f, false, style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        if (progress > 0f) {
            drawArc(color, -90f, 360f * progress, false, style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
    }
}

@Composable
private fun StreakCalendar(daily: List<DailyListenEntity>, palette: ir.esmail.espresso.ui.theme.EspressoPalette) {
    val days = remember(daily) {
        val active = daily.filter { it.millis >= 60_000L }.map { it.day }.toSet()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -34)
        (0..34).map { index ->
            val key = Dates.dayKey(cal.timeInMillis)
            val activeDay = key in active
            cal.add(Calendar.DAY_OF_YEAR, 1)
            key to activeDay
        }
    }
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(16.dp)).padding(14.dp),
    ) {
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                days.chunked(7).forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        week.forEach { (_, active) ->
                            Box(
                                Modifier.size(18.dp).clip(RoundedCornerShape(5.dp))
                                    .background(if (active) palette.accent else palette.outline),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("هر خانه یک روز است — رنگی یعنی حداقل یک دقیقه گوش دادی", style = MaterialTheme.typography.labelSmall, color = palette.textTertiary)
        }
    }
}

@Composable
private fun BarChart(daily: List<DailyListenEntity>, palette: ir.esmail.espresso.ui.theme.EspressoPalette) {
    val values = remember(daily) {
        val map = daily.associate { it.day to (it.millis / 60_000L).toInt() }
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -13)
        (0..13).map {
            val key = Dates.dayKey(cal.timeInMillis)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            map[key] ?: 0
        }
    }
    val max = max(1, values.maxOrNull() ?: 1)
    Box(
        Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(16.dp)).background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(16.dp)).padding(12.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val barWidth = size.width / values.size * 0.6f
            val gap = size.width / values.size
            values.forEachIndexed { index, value ->
                val h = (value.toFloat() / max * size.height * 0.8f).coerceAtMost(size.height)
                drawRect(
                    color = if (value > 0) palette.accent else palette.outline,
                    topLeft = Offset(index * gap + (gap - barWidth) / 2, size.height - h),
                    size = Size(barWidth, h),
                )
            }
        }
    }
}

@Composable
private fun QuizTrend(quizzes: List<QuizResultEntity>, palette: ir.esmail.espresso.ui.theme.EspressoPalette) {
    val values = remember(quizzes) {
        quizzes.takeLast(12).map { result ->
            if (result.total == 0) 0 else result.score * 100 / result.total
        }
    }
    if (values.isEmpty()) {
        Text("هنوز آزمونی ثبت نشده است.", style = MaterialTheme.typography.bodyMedium, color = palette.textTertiary)
        return
    }
    Box(
        Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(16.dp)).background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(16.dp)).padding(12.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val step = size.width / max(1, values.size - 1)
            val points = values.mapIndexed { index, pct ->
                Offset(index * step, size.height - (pct / 100f * size.height * 0.85f) - size.height * 0.05f)
            }
            for (i in 0 until points.size - 1) {
                drawLine(palette.mint, points[i], points[i + 1], strokeWidth = 3.dp.toPx())
            }
            points.forEach { point ->
                drawCircle(palette.accent, radius = 4.dp.toPx(), center = point)
            }
        }
    }
}

@Composable
private fun LevelProgress(
    bundle: ContentBundle,
    progress: Map<String, ir.esmail.espresso.data.ProgressEntity>,
    palette: ir.esmail.espresso.ui.theme.EspressoPalette,
) {
    val levels = listOf("beginner", "intermediate", "advanced")
    levels.forEach { level ->
        val total = bundle.episodes.count { it.level == level }
        val done = bundle.episodes.count { it.level == level && progress[it.id]?.completed == true }
        val fraction = if (total == 0) 0f else done.toFloat() / total
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(Fa.level(level), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary, modifier = Modifier.width(80.dp))
            Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(6.dp)).background(palette.outline)) {
                Box(Modifier.fillMaxWidth(fraction).height(10.dp).clip(RoundedCornerShape(6.dp)).background(palette.accent))
            }
            Spacer(Modifier.width(8.dp))
            Text("${Fa.num(done)}/${Fa.num(total)}", style = MaterialTheme.typography.labelSmall, color = palette.textTertiary)
        }
    }
}
