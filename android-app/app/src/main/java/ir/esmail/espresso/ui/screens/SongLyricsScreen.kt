package ir.esmail.espresso.ui.screens

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.esmail.espresso.data.LyricLine
import ir.esmail.espresso.data.LyricsRepository
import ir.esmail.espresso.data.NowPlaying
import ir.esmail.espresso.data.NowPlayingDetector
import ir.esmail.espresso.data.ProviderReport
import ir.esmail.espresso.data.Reach
import ir.esmail.espresso.data.TrackHit
import ir.esmail.espresso.playback.SoundManager
import ir.esmail.espresso.ui.AppViewModel
import ir.esmail.espresso.ui.Fa
import ir.esmail.espresso.ui.components.SoftIconButton
import ir.esmail.espresso.ui.theme.espresso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Lines activate slightly early so the highlight lands with the voice, not after it. */
private const val DEFAULT_LEAD_MS = 350L

/**
 * Detects the song currently playing on the device (via media sessions) or finds one by name,
 * then shows its lyrics in a popup with line-by-line Persian translation.
 *
 * When the song really is playing the popup follows the media session's live position: the
 * current line glows, the list scrolls itself, and a small equaliser pulses with the track.
 */
@Composable
fun SongLyricsScreen(vm: AppViewModel, bottomInset: Dp, onClose: () -> Unit) {
    val palette = espresso
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val repository = remember { LyricsRepository() }
    val detector = remember { NowPlayingDetector(context) }

    var query by remember { mutableStateOf("") }
    var nowPlaying by remember { mutableStateOf<NowPlaying?>(null) }
    var permissionHint by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<TrackHit>>(emptyList()) }
    var reports by remember { mutableStateOf<List<ProviderReport>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<TrackHit?>(null) }
    var lines by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var translated by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var translating by remember { mutableStateOf(false) }
    var translatingDone by remember { mutableIntStateOf(0) }
    var translatingTotal by remember { mutableIntStateOf(0) }
    var resolving by remember { mutableStateOf(false) }

    fun detectNowPlaying() {
        val current = detector.current()
        if (current != null) {
            nowPlaying = current
            query = current.query
            permissionHint = false
            vm.playSound(SoundManager.TAP)
        } else {
            permissionHint = true
        }
    }

    fun runSearch() {
        if (loading) return
        scope.launch {
            loading = true
            error = null
            results = emptyList()
            reports = emptyList()
            repository.search(query)
                .onSuccess { outcome ->
                    results = outcome.hits
                    reports = outcome.reports
                    when {
                        outcome.hits.isNotEmpty() -> Unit
                        outcome.allBlocked -> error =
                            "سرویس‌های ترانه از این شبکه در دسترس نیستند. VPN یا اینترنت دیگری را امتحان کن."
                        else -> error = "هیچ آهنگی با این نام پیدا نشد. نام خواننده را هم اضافه کن."
                    }
                    vm.playSound(if (outcome.hits.isEmpty()) SoundManager.WRONG else SoundManager.TAP)
                }
                .onFailure { error = it.message ?: "جستجو ناموفق بود." }
            loading = false
        }
    }

    fun openTrack(track: TrackHit) {
        selected = track
        translated = emptyMap()
        translatingDone = 0
        translatingTotal = 0
        lines = LyricsRepository.parseLines(track)
        scope.launch {
            if (!track.hasLyrics) {
                resolving = true
                repository.resolve(track).onSuccess { resolved ->
                    selected = resolved
                    lines = LyricsRepository.parseLines(resolved)
                }.onFailure { error = it.message }
                resolving = false
            }
            if (lines.isEmpty()) {
                vm.playSound(SoundManager.WRONG)
                return@launch
            }

            translating = true
            translatingTotal = lines.size
            vm.translator.translateLines(
                lines = lines.map { it.english },
                from = "en",
                to = "fa",
                onProgress = { done, total, map ->
                    withContext(Dispatchers.Main) {
                        translated = map
                        translatingDone = done
                        translatingTotal = total
                    }
                },
            ).onFailure { error = it.message }
            translating = false
            vm.playSound(SoundManager.SAVE)
        }
    }

    LaunchedEffect(Unit) { detectNowPlaying() }

    LaunchedEffect(selected) {
        if (selected == null) return@LaunchedEffect
        while (true) {
            delay(3_000)
            val current = withContext(Dispatchers.IO) { runCatching { detector.current() }.getOrNull() }
            if (current != null) nowPlaying = current
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SoftIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "بستن",
                onClick = onClose,
                background = Color.Transparent,
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "تشخیص آهنگ و ترانه",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Right,
                )
                Text(
                    text = "ترانه همراه با آهنگ جلو می‌رود",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textTertiary,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        nowPlaying?.let { song ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.accentSoft)
                    .border(1.dp, palette.accent.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = palette.accent)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                        Text(song.artist, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    }
                    SoftIconButton(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = "تازه‌سازی",
                        onClick = { detectNowPlaying() },
                        background = Color.Transparent,
                        tint = palette.accent,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (permissionHint) {
            Text(
                text = "برای اینکه ترانه همراه با آهنگ جلو برود، دسترسی اعلان‌ها را فعال کن:",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { runCatching { context.startActivity(detector.settingsIntent()) } },
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.accentInk),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("فعال‌سازی دسترسی")
            }
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("نام آهنگ یا خواننده") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = palette.outline,
                focusedTextColor = palette.textPrimary,
                unfocusedTextColor = palette.textPrimary,
                cursorColor = palette.accent,
            ),
            trailingIcon = {
                androidx.compose.material3.IconButton(onClick = { runSearch() }, enabled = !loading) {
                    Icon(Icons.Rounded.Search, contentDescription = "جستجو", tint = espresso.accent)
                }
            },
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { runSearch() },
            enabled = query.isNotBlank() && !loading,
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.accentInk),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (loading) CircularProgressIndicator(color = palette.accentInk, strokeWidth = 2.dp)
            else Text("جستجوی ترانه")
        }

        if (reports.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                reports.forEach { report -> ProviderChip(report) }
            }
        }

        error?.let {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = palette.danger)
                Spacer(Modifier.width(6.dp))
                Text(it, color = palette.danger, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(14.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = bottomInset + 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(results, key = { it.uid }) { hit ->
                ResultRow(hit = hit, onClick = { openTrack(hit) })
            }
        }
    }

    selected?.let { track ->
        LyricsPopup(
            track = track,
            lines = lines,
            translations = translated,
            translating = translating,
            translatingDone = translatingDone,
            translatingTotal = translatingTotal,
            resolving = resolving,
            detector = detector,
            onCopy = {
                val text = lines.mapIndexed { index, line ->
                    val fa = translated[index]?.let { " — $it" }.orEmpty()
                    "${line.english}$fa"
                }.joinToString("\n")
                clipboard.setText(AnnotatedString(text))
                vm.playSound(SoundManager.SAVE)
            },
            onDismiss = { selected = null },
        )
    }
}

@Composable
private fun ProviderChip(report: ProviderReport) {
    val palette = espresso
    val color = when (report.reach) {
        Reach.OK -> palette.mint
        Reach.EMPTY -> palette.textTertiary
        Reach.BLOCKED -> palette.danger
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(report.name, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun ResultRow(hit: TrackHit, onClick: () -> Unit) {
    val palette = espresso
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = palette.accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    hit.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Right,
                )
                Text(hit.displaySubtitle, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            }
            if (hit.hasLyrics) {
                Icon(Icons.Rounded.Lyrics, contentDescription = "ترانه آماده است", tint = palette.mint)
            } else {
                Icon(Icons.Rounded.Translate, contentDescription = "واکشی ترانه", tint = palette.textTertiary)
            }
        }
    }
}

@Composable
private fun LyricsPopup(
    track: TrackHit,
    lines: List<LyricLine>,
    translations: Map<Int, String>,
    translating: Boolean,
    translatingDone: Int,
    translatingTotal: Int,
    resolving: Boolean,
    detector: NowPlayingDetector,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = espresso
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val timed = remember(lines) { lines.any { it.isTimed } }

    var activeIndex by remember(track.uid) { mutableIntStateOf(-1) }
    var following by remember(track.uid) { mutableStateOf(false) }
    var offsetMs by remember(track.uid) { mutableLongStateOf(DEFAULT_LEAD_MS) }
    var positionMs by remember(track.uid) { mutableLongStateOf(0L) }
    var playing by remember(track.uid) { mutableStateOf(false) }

    LaunchedEffect(track.uid, timed) {
        if (!timed) return@LaunchedEffect
        while (true) {
            val snapshot = withContext(Dispatchers.IO) {
                runCatching { detector.snapshot() }.getOrNull()
            }
            if (snapshot != null && LyricsRepository.matchesPlayback(snapshot.title, snapshot.artist, track.trackName)) {
                following = true
                playing = snapshot.isPlaying
                positionMs = snapshot.positionMs
                val index = LyricsRepository.activeLineIndex(lines, snapshot.positionMs + offsetMs)
                if (index >= 0) activeIndex = index
            } else {
                following = false
                playing = false
            }
            delay(180)
        }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            val lift = with(density) { 110.dp.roundToPx() }
            runCatching { listState.animateScrollToItem(activeIndex, scrollOffset = -lift) }
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        NeonLyricsFrame(playing = following && playing) {
        Surface(shape = RoundedCornerShape(24.dp), color = palette.surface, tonalElevation = 8.dp) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(track.displayTitle, style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
                        Text(track.displaySubtitle, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                    }
                    SoftIconButton(
                        icon = Icons.Rounded.ContentCopy,
                        contentDescription = "کپی",
                        onClick = onCopy,
                        background = palette.accentSoft,
                        tint = palette.accent,
                    )
                    SoftIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "بستن",
                        onClick = onDismiss,
                        background = Color.Transparent,
                    )
                }

                if (timed) {
                    Spacer(Modifier.height(10.dp))
                    SyncBar(
                        following = following,
                        playing = playing,
                        positionMs = positionMs,
                        durationMs = (track.duration * 1000).toLong(),
                        offsetMs = offsetMs,
                        onOffsetChange = { offsetMs = (offsetMs + it).coerceIn(-5_000L, 5_000L) },
                    )
                }

                if (translating) {
                    Spacer(Modifier.height(8.dp))
                    TranslationProgress(done = translatingDone, total = translatingTotal, resolving = resolving)
                }

                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    state = listState,
                    modifier = Modifier.height(392.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(lines.size) { index ->
                        LyricRow(
                            line = lines[index],
                            translation = translations[index],
                            isActive = index == activeIndex,
                            dimmed = activeIndex >= 0,
                        )
                    }
                    if (lines.isEmpty() && !translating) {
                        item {
                            Text(
                                "ترانه‌ای برای این آهنگ در پایگاه داده یافت نشد.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "منابع: LRCLIB · iTunes · Deezer · lyrics.ovh — ترجمه: Google / MyMemory",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        }
    }
}

/**
 * One lyric line. English is forced left-to-right and left-aligned; the Persian translation is
 * right-aligned, so the two scripts never fight over the same edge.
 */
@Composable
private fun LyricRow(
    line: LyricLine,
    translation: String?,
    isActive: Boolean,
    dimmed: Boolean,
) {
    val palette = espresso
    val background by animateFloatAsState(if (isActive) 1f else 0f, tween(260), label = "bg")
    val bar by animateFloatAsState(if (isActive) 1f else 0f, tween(260), label = "bar")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.accentSoft.copy(alpha = palette.accentSoft.alpha * background))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        // Accent bar that grows in on the active line.
        if (bar > 0.01f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height((34 * bar).dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.accent),
            )
        }
        Column(Modifier.fillMaxWidth().padding(start = 10.dp)) {
            if (line.time.isNotEmpty()) {
                Text(
                    text = line.time,
                    style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.Ltr),
                    color = if (isActive) palette.accent else palette.textTertiary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Left,
                )
            }
            Text(
                text = line.english,
                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                color = when {
                    isActive -> palette.accent
                    dimmed -> palette.textPrimary.copy(alpha = 0.45f)
                    else -> palette.textPrimary
                },
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Left,
                modifier = Modifier.fillMaxWidth(),
            )
            translation?.let { fa ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = fa,
                    style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Rtl),
                    color = if (isActive) palette.textPrimary else palette.textSecondary.copy(alpha = if (dimmed) 0.6f else 1f),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Playhead, follow state, drift correction and a slim progress rail. */
@Composable
private fun SyncBar(
    following: Boolean,
    playing: Boolean,
    positionMs: Long,
    durationMs: Long,
    offsetMs: Long,
    onOffsetChange: (Long) -> Unit,
) {
    val palette = espresso
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background((if (following) palette.mint else palette.textTertiary).copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (following) {
                        Equalizer(color = palette.mint, playing = playing, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        if (following) "هماهنگ با پخش · ${Fa.clock(positionMs)}" else "منتظر پخش آهنگ…",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (following) palette.mint else palette.textTertiary,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            OffsetChip(label = "زودتر", onClick = { onOffsetChange(250L) })
            Text(
                text = if (offsetMs == 0L) "۰" else (if (offsetMs > 0) "+" else "−") + Fa.num(abs(offsetMs) / 100) + "s",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textTertiary,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            OffsetChip(label = "دیرتر", onClick = { onOffsetChange(-250L) })
        }

        if (following && durationMs > 0L) {
            Spacer(Modifier.height(8.dp))
            val progress = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.outline),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(palette.accent, palette.mint))),
                )
            }
        }
    }
}

/** Four bars that bounce while the track is playing and rest flat when paused. */
@Composable
private fun Equalizer(color: Color, playing: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "eq")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_100, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Canvas(modifier) {
        val bars = 4
        val slot = size.width / bars
        val barWidth = slot * 0.55f
        for (i in 0 until bars) {
            val wave = sin((phase * 2 * PI) + i * 1.3).toFloat()
            val level = if (playing) 0.35f + 0.65f * ((wave + 1f) / 2f) else 0.22f
            val barHeight = size.height * level
            drawRoundRect(
                color = color,
                topLeft = Offset(i * slot + (slot - barWidth) / 2f, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

@Composable
private fun OffsetChip(label: String, onClick: () -> Unit) {
    val palette = espresso
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(palette.surfaceHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
    }
}

@Composable
private fun TranslationProgress(done: Int, total: Int, resolving: Boolean) {
    val palette = espresso
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(color = palette.accent, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            when {
                resolving -> "در حال یافتن ترانه…"
                total > 0 -> "در حال ترجمه… ${Fa.num(done)} از ${Fa.num(total)} خط"
                else -> "در حال ترجمه…"
            },
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
        )
    }
}

/**
 * A live spectrum wrapped around the lyrics sheet. The border is a rotating rainbow sweep and
 * the bars along all four edges dance with the track, so the modal itself becomes the
 * "now playing" surface. Falls back to a calm static frame when nothing is playing.
 */
@Composable
private fun NeonLyricsFrame(
    playing: Boolean,
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    content: @Composable () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "neon")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(if (playing) 2_400 else 9_000, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "phase",
    )

    val inset = 22.dp
    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            val pad = inset.toPx()
            val radius = corner.toPx()
            val left = pad
            val top = pad
            val right = size.width - pad
            val bottom = size.height - pad
            if (right <= left || bottom <= top) return@Canvas

            val hueBase = phase * 360f
            val strength = if (playing) 1f else 0.62f

            fun rainbow(offset: Float) = List(13) { index ->
                Color.hsv((hueBase + offset + index * (360f / 12f)) % 360f, 0.62f, 1f)
            }

            // Soft outer glow: three widening strokes at falling opacity.
            for (i in 3 downTo 0) {
                val grow = (i + 1) * 7.dp.toPx()
                drawRoundRect(
                    brush = Brush.sweepGradient(rainbow(0f)),
                    topLeft = Offset(left - grow, top - grow),
                    size = Size(right - left + grow * 2, bottom - top + grow * 2),
                    cornerRadius = CornerRadius(radius + grow, radius + grow),
                    style = Stroke(width = 2.dp.toPx()),
                    alpha = (0.18f - i * 0.035f) * strength,
                )
            }

            // Crisp border.
            drawRoundRect(
                brush = Brush.sweepGradient(rainbow(0f)),
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = 3.dp.toPx()),
                alpha = 0.95f * strength,
            )

            // Spectrum bars hugging the four edges.
            val step = 15.dp.toPx()
            val base = 4.dp.toPx()
            val peak = 16.dp.toPx()
            val stroke = 2.5.dp.toPx()

            fun barColor(position: Float): Color =
                Color.hsv((hueBase + position * 220f) % 360f, 0.85f, 1f).copy(alpha = strength)

            var index = 0
            var x = left + radius
            while (x <= right - radius) {
                val wave = if (playing) (sin(phase * 2 * PI * 3 + index * 0.55) + 1).toFloat() / 2f else 0f
                val length = base + peak * wave
                val color = barColor((x - left) / (right - left))
                drawLine(color, Offset(x, top), Offset(x, top - length), stroke, StrokeCap.Round)
                drawLine(color, Offset(x, bottom), Offset(x, bottom + length), stroke, StrokeCap.Round)
                x += step
                index++
            }

            var y = top + radius
            while (y <= bottom - radius) {
                val wave = if (playing) (sin(phase * 2 * PI * 3 + index * 0.55) + 1).toFloat() / 2f else 0f
                val length = base + peak * wave
                val color = barColor((y - top) / (bottom - top))
                drawLine(color, Offset(left, y), Offset(left - length, y), stroke, StrokeCap.Round)
                drawLine(color, Offset(right, y), Offset(right + length, y), stroke, StrokeCap.Round)
                y += step
                index++
            }
        }

        Box(Modifier.padding(inset)) { content() }
    }
}
