package ir.esmail.espresso.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.esmail.espresso.data.ContentBundle
import ir.esmail.espresso.data.Episode
import ir.esmail.espresso.data.Segment
import ir.esmail.espresso.data.VocabItem
import ir.esmail.espresso.data.WordMeaning
import ir.esmail.espresso.data.WordToken
import ir.esmail.espresso.ui.AppViewModel
import ir.esmail.espresso.ui.Fa
import ir.esmail.espresso.ui.components.CoverArt
import ir.esmail.espresso.ui.components.SoftIconButton
import ir.esmail.espresso.ui.components.TappableWords
import ir.esmail.espresso.ui.components.Waveform
import ir.esmail.espresso.ui.components.WordSheet
import ir.esmail.espresso.ui.components.decorativePeaks
import ir.esmail.espresso.ui.components.ltr
import ir.esmail.espresso.ui.components.rememberSpeaker
import ir.esmail.espresso.ui.theme.Fraunces
import ir.esmail.espresso.ui.theme.TranscriptStyle
import ir.esmail.espresso.ui.theme.TranscriptTranslationStyle
import ir.esmail.espresso.ui.theme.coverAccent
import ir.esmail.espresso.ui.theme.espresso
import java.util.Locale

private val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f)

@Composable
fun PlayerScreen(
    vm: AppViewModel,
    episode: Episode,
    bundle: ContentBundle,
    targetSegmentId: String? = null,
    onClose: () -> Unit,
    onOpenQuiz: () -> Unit,
) {
    val palette = espresso
    val playerState by vm.player.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val savedWords by vm.words.collectAsStateWithLifecycle()
    val quizzes by vm.quizzes.collectAsStateWithLifecycle()
    val favoriteIds by vm.favorites.collectAsStateWithLifecycle()
    var isFavorite by remember(episode.id) { mutableStateOf(false) }
    LaunchedEffect(episode.id) { isFavorite = vm.isFavorite(episode.id) }

    val accent = coverAccent(episode.id)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var tapped by remember { mutableStateOf<WordToken?>(null) }
    var speedMenu by remember { mutableStateOf(false) }
    var timerMenu by remember { mutableStateOf(false) }

    val savedKeys = remember(savedWords) { savedWords.map { it.normalized }.toHashSet() }
    val episodeIndex = bundle.episodes.indexOf(episode).coerceAtLeast(0)
    // TTS WAVs have a different duration from the authored transcript. Keep the displayed times
    // and word highlighting in the source timeline, and only map to the playback timeline on seek.
    val sourceDurationMs = episode.durationMs
    val playbackDurationMs = if (playerState.episodeId == episode.id && playerState.durationMs > 0) {
        playerState.durationMs
    } else {
        sourceDurationMs
    }
    val positionMs = if (playerState.episodeId == episode.id) playerState.positionMs else 0L
    val sourcePositionMs = if (playbackDurationMs > 0) {
        (positionMs * sourceDurationMs / playbackDurationMs).coerceIn(0L, sourceDurationMs)
    } else {
        0L
    }
    val progress = if (playbackDurationMs > 0) positionMs.toFloat() / playbackDurationMs else 0f

    val activeSegment = remember(sourcePositionMs, episode) {
        if (episode.segments.isEmpty()) {
            -1
        } else {
            episode.segments.indexOfLast { it.startMs <= sourcePositionMs }.coerceAtLeast(0)
        }
    }
    val activeWord = remember(activeSegment, sourcePositionMs, episode) {
        val segment = episode.segments.getOrNull(activeSegment) ?: return@remember -1
        val seconds = sourcePositionMs / 1000.0
        segment.words.indexOfLast { it.s <= seconds }
    }

    LaunchedEffect(episode.id) {
        if (vm.player.state.value.episodeId != episode.id) {
            val saved = vm.progress.value[episode.id]
            val fraction = if (saved != null && saved.durationMs > 0L) {
                saved.positionMs.toFloat() / saved.durationMs
            } else {
                0f
            }
            val start = if (fraction in 0.01f..0.98f) {
                (fraction * episode.durationMs).toLong()
            } else {
                0L
            }
            vm.player.playQueue(bundle.episodes, episodeIndex, start)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to accent.copy(alpha = 0.22f),
                    0.42f to palette.background,
                    1f to palette.background,
                ),
            ),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val coverSize = minOf(maxWidth * 0.50f, maxHeight * 0.195f).coerceAtLeast(112.dp)

            Column(Modifier.fillMaxSize()) {
                TopBar(
                    subtitle = "درس ${Fa.num(episodeIndex + 1)} از ${Fa.num(bundle.episodes.size)}",
                    sourceUrl = episode.sourceUrl,
                    onClose = onClose,
                )

                Spacer(Modifier.height(6.dp))
                CoverArt(
                    episode = episode,
                    index = episodeIndex,
                    modifier = Modifier.size(coverSize).align(Alignment.CenterHorizontally),
                    corner = 28.dp,
                    numberFraction = 0.24f,
                )

                Spacer(Modifier.height(14.dp))
                Text(
                    text = episode.title,
                    fontFamily = Fraunces,
                    fontSize = 20.sp,
                    lineHeight = 27.sp,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center,
                    style = TextStyle(textDirection = TextDirection.Ltr),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = episode.titleFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    androidx.compose.material3.IconButton(onClick = {
                        vm.toggleFavorite(episode.id) { saved -> isFavorite = saved }
                    }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Bookmark
                            else Icons.Rounded.BookmarkBorder,
                            contentDescription = "علاقهمندی",
                            tint = if (isFavorite) palette.accent else palette.textTertiary,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                if (playerState.preparing) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = palette.accent,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "در حال ساخت صوت درس…",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.accent,
                        )
                    }
                }
                Waveform(
                    peaks = if (episode.peaks.isNotEmpty()) episode.peaks else decorativePeaks(episode.id),
                    progress = progress,
                    modifier = Modifier.padding(horizontal = 22.dp),
                    height = 38.dp,
                    activeColor = accent,
                    onSeek = { fraction ->
                        vm.player.seekTo((fraction * playbackDurationMs).toLong())
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = Fa.clock(sourcePositionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary,
                    )
                    Text(
                        text = "−" + Fa.clock((sourceDurationMs - sourcePositionMs).coerceAtLeast(0)),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textTertiary,
                    )
                }

                Spacer(Modifier.height(10.dp))
                Controls(
                    isPlaying = playerState.isPlaying && playerState.episodeId == episode.id,
                    isBuffering = playerState.isBuffering,
                    speed = playerState.speed,
                    sleepEndsAt = playerState.sleepTimerEndsAt,
                    speedMenu = speedMenu,
                    onSpeedMenu = { speedMenu = it },
                    timerMenu = timerMenu,
                    onTimerMenu = { timerMenu = it },
                    onToggle = { vm.player.toggle() },
                    onBack = { vm.player.seekBy(-10_000) },
                    onForward = { vm.player.seekBy(10_000) },
                    onSpeed = { vm.setSpeed(it) },
                    onSleep = { vm.player.setSleepTimer(it) },
                )

                Spacer(Modifier.height(10.dp))
                Tabs(selected = tab, onSelect = { tab = it })

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        0 -> TranscriptTab(
                            episode = episode,
                            activeSegment = activeSegment,
                            activeWord = activeWord,
                            savedKeys = savedKeys,
                            autoScroll = settings.autoScroll,
                            targetSegmentId = targetSegmentId,
                            onWordTap = { tapped = it },
                            onSeek = { sourceMs ->
                                val target = if (sourceDurationMs > 0) {
                                    sourceMs * playbackDurationMs / sourceDurationMs
                                } else {
                                    sourceMs
                                }
                                vm.player.seekTo(target)
                            },
                        )

                        1 -> VocabTab(episode = episode, onOpenQuiz = onOpenQuiz)

                        else -> QuizTab(
                            episode = episode,
                            lastResult = quizzes.firstOrNull { it.episodeId == episode.id },
                            onOpenQuiz = onOpenQuiz,
                            savedCount = savedWords.count { it.episodeId == episode.id },
                        )
                    }
                }
            }
        }
    }

    tapped?.let { token ->
        val meaning = bundle.meaning(token.w, episode)
        var saved by remember(token) { mutableStateOf(false) }
        LaunchedEffect(token) {
            vm.isWordSaved(meaning?.term ?: token.w) { saved = it }
        }
        WordSheet(
            token = token.w,
            meaning = meaning,
            saved = saved,
            onDismiss = { tapped = null },
            onToggleSave = {
                // Every tapped token is saveable. When no authored meaning exists we keep the raw
                // word with a clear label; the learner can fill in the meaning via online translate.
                val found = meaning ?: WordMeaning(
                    term = token.w,
                    fa = "معنی ثبت نشده — قابل ویرایش با ترجمهٔ آنلاین",
                    source = "ذخیرهٔ سریع",
                )
                vm.toggleWord(found, episode) { now -> saved = now }
            },
        )
    }
}

@Composable
private fun TopBar(subtitle: String, sourceUrl: String, onClose: () -> Unit) {
    val palette = espresso
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SoftIconButton(
            icon = Icons.Rounded.KeyboardArrowDown,
            contentDescription = "بستن",
            onClick = onClose,
            background = Color.Transparent,
            tint = palette.textPrimary,
            size = 40.dp,
        )
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "در حال پخش",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textTertiary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
            )
        }
        if (sourceUrl.isNotBlank()) {
            SoftIconButton(
                icon = Icons.Rounded.OpenInNew,
                contentDescription = "منبع درس",
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl)))
                    }
                },
                background = Color.Transparent,
                tint = palette.textSecondary,
                size = 40.dp,
            )
        } else {
            Spacer(Modifier.width(40.dp))
        }
    }
}

@Composable
private fun Controls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    speed: Float,
    sleepEndsAt: Long?,
    speedMenu: Boolean,
    onSpeedMenu: (Boolean) -> Unit,
    timerMenu: Boolean,
    onTimerMenu: (Boolean) -> Unit,
    onToggle: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onSpeed: (Float) -> Unit,
    onSleep: (Int?) -> Unit,
) {
    val palette = espresso
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(palette.surfaceHigh)
                    .clickable { onSpeedMenu(true) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = Fa.speed(speed),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textPrimary,
                )
            }
            DropdownMenu(expanded = speedMenu, onDismissRequest = { onSpeedMenu(false) }) {
                SPEEDS.forEach { value ->
                    DropdownMenuItem(
                        text = { Text(Fa.speed(value)) },
                        onClick = {
                            onSpeed(value)
                            onSpeedMenu(false)
                        },
                    )
                }
            }
        }

        SoftIconButton(
            icon = Icons.Rounded.Replay10,
            contentDescription = "ده ثانیه عقب",
            onClick = onBack,
            background = Color.Transparent,
            size = 46.dp,
        )

        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(palette.accent)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when {
                    isBuffering -> Icons.Rounded.Timer
                    isPlaying -> Icons.Rounded.Pause
                    else -> Icons.Rounded.PlayArrow
                },
                contentDescription = null,
                tint = palette.accentInk,
                modifier = Modifier.size(32.dp),
            )
        }

        SoftIconButton(
            icon = Icons.Rounded.Forward10,
            contentDescription = "ده ثانیه جلو",
            onClick = onForward,
            background = Color.Transparent,
            size = 46.dp,
        )

        Box {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (sleepEndsAt != null) palette.accentSoft else palette.surfaceHigh)
                    .clickable { onTimerMenu(true) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = "زمان‌سنج خواب",
                    tint = if (sleepEndsAt != null) palette.accent else palette.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(expanded = timerMenu, onDismissRequest = { onTimerMenu(false) }) {
                listOf(15, 30, 45).forEach { minutes ->
                    DropdownMenuItem(
                        text = { Text("${Fa.num(minutes)} دقیقه") },
                        onClick = {
                            onSleep(minutes)
                            onTimerMenu(false)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text("خاموش کردن") },
                    onClick = {
                        onSleep(null)
                        onTimerMenu(false)
                    },
                )
            }
        }
    }
}

@Composable
private fun Tabs(selected: Int, onSelect: (Int) -> Unit) {
    val palette = espresso
    val labels = listOf("متن", "واژه‌ها", "آزمون")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surfaceHigh)
            .padding(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val active = index == selected
            val bg by animateColorAsState(
                if (active) palette.surface else Color.Transparent,
                label = "tab",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(bg)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) palette.accent else palette.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun TranscriptTab(
    episode: Episode,
    activeSegment: Int,
    activeWord: Int,
    savedKeys: Set<String>,
    autoScroll: Boolean,
    targetSegmentId: String? = null,
    onWordTap: (WordToken) -> Unit,
    onSeek: (Long) -> Unit,
) {
    val palette = espresso
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(targetSegmentId) {
        if (targetSegmentId != null) {
            val targetIndex = episode.segments.indexOfFirst { it.id == targetSegmentId }
            if (targetIndex >= 0) {
                runCatching { listState.scrollToItem(targetIndex, scrollOffset = -40) }
            }
        }
    }

    LaunchedEffect(activeSegment) {
        if (autoScroll && activeSegment >= 0 && !listState.isScrollInProgress) {
            runCatching { listState.animateScrollToItem(activeSegment, scrollOffset = -40) }
        }
    }

    if (episode.segments.isEmpty()) {
        Text(
            text = "متنی برای این درس ثبت نشده است.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textTertiary,
            modifier = Modifier.padding(24.dp),
        )
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(episode.segments, key = { _, segment -> segment.id }) { index, segment ->
            SegmentRow(
                segment = segment,
                active = index == activeSegment,
                activeWord = if (index == activeSegment) activeWord else -1,
                savedKeys = savedKeys,
                onWordTap = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onWordTap(it)
                },
                onSeek = { onSeek(segment.startMs) },
            )
        }
    }
}

@Composable
private fun SegmentRow(
    segment: Segment,
    active: Boolean,
    activeWord: Int,
    savedKeys: Set<String>,
    onWordTap: (WordToken) -> Unit,
    onSeek: () -> Unit,
) {
    val palette = espresso
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) palette.accent.copy(alpha = 0.09f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (active) palette.accent.copy(alpha = 0.22f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        TappableWords(
            text = segment.text,
            tokens = segment.words,
            style = TranscriptStyle,
            activeIndex = activeWord,
            savedWords = savedKeys,
            onWordTap = onWordTap,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = segment.fa,
            style = TranscriptTranslationStyle,
            color = if (active) palette.textSecondary else palette.textTertiary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSeek),
        )
    }
}

@Composable
private fun VocabTab(
    episode: Episode,
    onOpenQuiz: () -> Unit,
) {
    val palette = espresso
    val speaker = rememberSpeaker()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(episode.vocab, key = { index, _ -> index }) { _, item ->
            VocabCard(item = item, onSpeak = { if (speaker.available) speaker.say(item.term) })
        }

        if (episode.phrases.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "عبارت‌های کلیدی",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                )
            }
            itemsIndexed(episode.phrases, key = { index, _ -> index }) { _, phrase ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.outline, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = phrase.en,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold).ltr(),
                            color = palette.accent,
                            modifier = Modifier.weight(1f),
                        )
                        if (speaker.available) {
                            Icon(
                                imageVector = Icons.Rounded.VolumeUp,
                                contentDescription = null,
                                tint = palette.textTertiary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { speaker.say(phrase.en) },
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = phrase.fa,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                    )
                    if (phrase.note.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = phrase.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textTertiary,
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.surfaceHigh)
                    .clickable(onClick = onOpenQuiz)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Quiz,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "آزمون این درس را بده",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun VocabCard(item: VocabItem, onSpeak: () -> Unit) {
    val palette = espresso
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(18.dp))
            .clickable(onClick = onSpeak)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.term,
                fontFamily = Fraunces,
                fontSize = 19.sp,
                style = TextStyle(textDirection = TextDirection.Ltr),
                color = palette.textPrimary,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (item.ipa.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.ipa,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textTertiary,
                )
            }
            Spacer(Modifier.weight(1f))
            if (item.pos.isNotBlank()) {
                Text(
                    text = Fa.partOfSpeech(item.pos),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.lilac,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.fa,
            style = MaterialTheme.typography.bodyLarge,
            color = palette.textPrimary,
        )
        if (item.example.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.example,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textTertiary,
            )
        }
    }
}

@Composable
private fun QuizTab(
    episode: Episode,
    lastResult: ir.esmail.espresso.data.QuizResultEntity?,
    onOpenQuiz: () -> Unit,
    savedCount: Int,
) {
    val palette = espresso
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(palette.accentSoft, palette.surface)))
                .border(1.dp, palette.accent.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                .padding(20.dp),
        ) {
            Text(
                text = "چقدر یاد گرفتی؟",
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "آزمون کوتاه این درس از همان واژه‌ها و جمله‌هایی ساخته می‌شود که شنیدی.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.accent)
                    .clickable(onClick = onOpenQuiz)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "شروع آزمون",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.accentInk,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoChip(label = "واژه‌های درس", value = Fa.num(episode.vocab.size), modifier = Modifier.weight(1f))
            InfoChip(label = "واژه‌های ذخیره‌شده", value = Fa.num(savedCount), modifier = Modifier.weight(1f))
        }

        if (lastResult != null) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.outline, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${lastResult.score}/${lastResult.total}",
                    fontFamily = Fraunces,
                    fontSize = 24.sp,
                    color = palette.mint,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "نتیجهٔ آخرین آزمون",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary,
                    )
                    Text(
                        text = Fa.digits("${(lastResult.score * 100) / lastResult.total.coerceAtLeast(1)}%") + " درست",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = espresso
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = palette.textTertiary)
    }
}
