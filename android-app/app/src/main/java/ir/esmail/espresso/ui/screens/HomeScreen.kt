package ir.esmail.espresso.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.InsertChart
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.esmail.espresso.R
import ir.esmail.espresso.data.ContentBundle
import ir.esmail.espresso.data.Episode
import ir.esmail.espresso.data.LearnerStats
import ir.esmail.espresso.data.ProgressEntity
import ir.esmail.espresso.ui.AppViewModel
import ir.esmail.espresso.ui.Fa
import ir.esmail.espresso.ui.components.CoverArt
import ir.esmail.espresso.ui.components.EpisodeCard
import ir.esmail.espresso.ui.components.LevelBadge
import ir.esmail.espresso.ui.components.SectionTitle
import ir.esmail.espresso.ui.components.SoftIconButton
import ir.esmail.espresso.ui.components.StatTile
import ir.esmail.espresso.ui.components.ThinProgress
import ir.esmail.espresso.ui.components.ltrStart
import ir.esmail.espresso.ui.theme.Fraunces
import ir.esmail.espresso.ui.theme.coverAccent
import ir.esmail.espresso.ui.theme.espresso
import java.util.Calendar

@Composable
fun HomeScreen(
    vm: AppViewModel,
    bundle: ContentBundle,
    bottomInset: Dp,
    onOpenEpisode: (Episode) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenPodcasts: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenTranslate: () -> Unit,
    onOpenSongs: () -> Unit,
    onOpenScan: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSpeak: () -> Unit,
    onOpenStats: () -> Unit,
) {
    val palette = espresso
    val progress by vm.progress.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()

    val resume = resumeTarget(bundle, progress)
    val newLessons = bundle.episodes.take(8)

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(top = 12.dp, bottom = bottomInset + 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Header(onOpenSettings = onOpenSettings, onOpenTranslate = onOpenTranslate)
        }

        item {
            if (resume != null) {
                ResumeCard(
                    episode = resume.first,
                    index = resume.second,
                    progress = progress[resume.first.id],
                    onClick = { onOpenEpisode(resume.first) },
                )
            }
        }

        item {
            StatsRow(stats = stats)
        }

        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                SectionTitle(title = "ابزارهای هوشمند", trailing = null, onTrailingClick = null)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToolCard(
                        title = "تشخیص آهنگ",
                        subtitle = "متن آهنگ با ترجمه",
                        icon = Icons.Rounded.GraphicEq,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenSongs,
                    )
                    ToolCard(
                        title = "اسکن صفحه",
                        subtitle = "ترجمهٔ متن روی صفحه",
                        icon = Icons.Rounded.DocumentScanner,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenScan,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToolCard(
                        title = "جستجوی واژهها",
                        subtitle = "در تمام ۲۰۸ درس",
                        icon = Icons.Rounded.Search,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenSearch,
                    )
                    ToolCard(
                        title = "تمرین تلفظ",
                        subtitle = "بگو و نمره بگیر",
                        icon = Icons.Rounded.Mic,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenSpeak,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToolCard(
                        title = "آمار پیشرفت",
                        subtitle = "استریگ و اهداف",
                        icon = Icons.Rounded.InsertChart,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenStats,
                    )
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                SectionTitle(title = "درس‌های Espresso English", trailing = "همه", onTrailingClick = onOpenLibrary)
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(newLessons, key = { _, item -> item.id }) { index, episode ->
                    EpisodeCard(
                        episode = episode,
                        index = bundle.episodes.indexOf(episode),
                        progress = progress[episode.id],
                        onClick = { onOpenEpisode(episode) },
                    )
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                WordOfDayCard(bundle = bundle)
            }
        }

        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                ReviewTeaser(dueCount = stats.wordsDue, onClick = onOpenReview)
            }
        }

        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                SectionTitle(title = "پادکست‌های پیشنهادی", trailing = "همه", onTrailingClick = onOpenPodcasts)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "${Fa.num(bundle.podcasts.size)} پادکست معتبر برای تقویت گوش دادن",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(bundle.podcasts.take(6)) { podcast ->
                        PodcastChip(name = podcast.name, host = podcast.host, onClick = onOpenPodcasts)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = espresso
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = palette.textTertiary)
        }
    }
}

@Composable
private fun Header(onOpenSettings: () -> Unit, onOpenTranslate: () -> Unit) {
    val palette = espresso
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.brand_mark),
            contentDescription = null,
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Espresso English",
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary,
            )
            Text(
                text = "گوش بده، بفهم، به‌خاطر بسپار",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textTertiary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SoftIconButton(
                icon = Icons.Rounded.Translate,
                contentDescription = "ترجمه آنلاین",
                onClick = onOpenTranslate,
                background = palette.accentSoft,
                tint = palette.accent,
            )
            SoftIconButton(
                icon = Icons.Rounded.Settings,
                contentDescription = "تنظیمات",
                onClick = onOpenSettings,
            )
        }
    }
}

@Composable
private fun ResumeCard(
    episode: Episode,
    index: Int,
    progress: ProgressEntity?,
    onClick: () -> Unit,
) {
    val palette = espresso
    val accent = coverAccent(episode.id)
    val fraction = if (progress != null && progress.durationMs > 0) {
        progress.positionMs.toFloat() / progress.durationMs
    } else {
        0f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.30f), palette.surface, palette.surface),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArt(
                episode = episode,
                index = index,
                modifier = Modifier.size(112.dp),
                corner = 20.dp,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (fraction > 0.01f) "ادامه بده" else "شروع کن",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold).ltrStart(),
                    color = palette.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = episode.titleFa,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = palette.accentInk,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        ThinProgress(progress = fraction, height = 3.dp)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = if (fraction > 0.01f) {
                                "${Fa.clock(progress?.positionMs ?: 0L)} از ${Fa.clock(episode.durationMs)}"
                            } else {
                                "${Fa.clock(episode.durationMs)}، ${Fa.num(episode.vocabCount)} واژه"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textTertiary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(stats: LearnerStats) {
    val palette = espresso
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatTile(
            value = Fa.num(stats.streakDays),
            label = "روز پشت‌سرهم",
            modifier = Modifier.weight(1f),
            accent = palette.accent,
        )
        StatTile(
            value = Fa.num(stats.wordsSaved),
            label = "واژهٔ ذخیره‌شده",
            modifier = Modifier.weight(1f),
            accent = palette.mint,
        )
        StatTile(
            value = Fa.num(stats.minutesListened),
            label = "دقیقه گوش دادن",
            modifier = Modifier.weight(1f),
            accent = palette.lilac,
        )
    }
}

@Composable
private fun WordOfDayCard(bundle: ContentBundle) {
    val palette = espresso
    val entry = rememberWordOfDay(bundle)
    if (entry == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "واژهٔ امروز",
                style = MaterialTheme.typography.labelLarge,
                color = palette.accent,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = entry.term,
                fontFamily = Fraunces,
                fontSize = 28.sp,
                color = palette.textPrimary,
            )
            Spacer(Modifier.width(8.dp))
            if (entry.ipa.isNotBlank()) {
                Text(
                    text = entry.ipa,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textTertiary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = entry.meaning,
            style = MaterialTheme.typography.titleMedium,
            color = palette.textSecondary,
        )
        if (entry.definition.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = entry.definition,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textTertiary,
            )
        }
    }
}

private data class DailyWord(
    val term: String,
    val meaning: String,
    val ipa: String,
    val definition: String,
)

@Composable
private fun rememberWordOfDay(bundle: ContentBundle): DailyWord? {
    val day = Calendar.getInstance().let { it.get(Calendar.DAY_OF_YEAR) + it.get(Calendar.YEAR) * 366 }
    val entries = bundle.glossary.entries.filter { it.value.fa.isNotBlank() }.toList()
    if (entries.isEmpty()) return null
    val picked = entries[day % entries.size]
    return DailyWord(
        term = picked.key,
        meaning = picked.value.fa,
        ipa = picked.value.ipa,
        definition = picked.value.en,
    )
}

@Composable
private fun ReviewTeaser(dueCount: Int, onClick: () -> Unit) {
    val palette = espresso
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(listOf(palette.mint.copy(alpha = 0.18f), palette.surface)),
            )
            .border(1.dp, palette.mint.copy(alpha = 0.24f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(palette.mint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Style,
                contentDescription = null,
                tint = palette.mint,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (dueCount > 0) "${Fa.num(dueCount)} واژه آمادهٔ مرور است" else "مرور روزانه",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
            )
            Text(
                text = "با تکرار فاصله‌دار، واژه‌ها در حافظهٔ بلندمدت می‌مانند",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textTertiary,
            )
        }
        Icon(
            imageVector = Icons.Rounded.Headphones,
            contentDescription = null,
            tint = palette.mint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun PodcastChip(name: String, host: String, onClick: () -> Unit) {
    val palette = espresso
    Column(
        modifier = Modifier
            .width(168.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = palette.textPrimary,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = host,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textTertiary,
            maxLines = 1,
        )
    }
}

/** Most recently opened lesson, or the first lesson for a brand new learner. */
private fun resumeTarget(
    bundle: ContentBundle,
    progress: Map<String, ProgressEntity>,
): Pair<Episode, Int>? {
    if (bundle.episodes.isEmpty()) return null
    val latest = progress.values.maxByOrNull { it.updatedAt }
    val episode = latest?.let { bundle.byId[it.episodeId] } ?: bundle.episodes.first()
    val index = bundle.episodes.indexOf(episode).coerceAtLeast(0)
    return episode to index
}
