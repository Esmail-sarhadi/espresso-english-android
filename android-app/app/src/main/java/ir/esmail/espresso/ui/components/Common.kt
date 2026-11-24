package ir.esmail.espresso.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.esmail.espresso.data.Episode
import ir.esmail.espresso.data.ProgressEntity
import ir.esmail.espresso.ui.Fa
import ir.esmail.espresso.ui.theme.Fraunces
import ir.esmail.espresso.ui.theme.coverAccent
import ir.esmail.espresso.ui.theme.espresso
import kotlin.math.abs
import kotlin.math.max

/* ------------------------------------------------------------------------------------------
 * Cover art: generated from the episode id so every lesson has its own colour, with the lesson
 * number set in the display face. No bitmaps needed, no network, always crisp.
 * ------------------------------------------------------------------------------------------ */

@Composable
fun CoverArt(
    episode: Episode,
    index: Int,
    modifier: Modifier = Modifier,
    corner: Dp = 22.dp,
    numberFraction: Float = 0.26f,
    showArcs: Boolean = true,
) {
    val palette = espresso
    val accent = coverAccent(episode.id)
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = if (palette.isDark) 0.26f else 0.22f),
                        palette.surfaceHigh,
                        palette.surface,
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            ),
    ) {
        val size = minOf(maxWidth, maxHeight)
        if (showArcs) {
            Canvas(Modifier.fillMaxSize()) {
                val radius = this.size.minDimension
                for (factor in listOf(0.34f, 0.41f, 0.48f)) {
                    drawArc(
                        color = accent.copy(alpha = 0.22f),
                        startAngle = -52f,
                        sweepAngle = 104f,
                        useCenter = false,
                        topLeft = Offset(
                            x = (this.size.width - radius * factor * 2) / 2f,
                            y = (this.size.height - radius * factor * 2) / 2f,
                        ),
                        size = Size(radius * factor * 2, radius * factor * 2),
                        style = Stroke(width = max(1f, radius * 0.004f)),
                    )
                }
            }
        }
        Text(
            text = "%02d".format(index + 1),
            fontFamily = Fraunces,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            fontSize = (size.value * numberFraction).sp,
            lineHeight = (size.value * numberFraction).sp,
            modifier = Modifier.align(Alignment.Center),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(size * 0.07f)
                .size(size * 0.035f)
                .clip(CircleShape)
                .background(palette.mint),
        )
    }
}

/* ------------------------------------------------------------------------------------------
 * Waveform scrubber
 * ------------------------------------------------------------------------------------------ */

@Composable
fun Waveform(
    peaks: List<Float>,
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp,
    activeColor: Color? = null,
    rtl: Boolean = true,
    onSeek: (Float) -> Unit = {},
) {
    val palette = espresso
    val played = activeColor ?: palette.accent
    val idle = palette.textTertiary.copy(alpha = 0.32f)
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(140), label = "wave")

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(peaks) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(if (rtl) 1f - fraction else fraction)
                }
            }
            .pointerInput(peaks) {
                detectDragGestures { change, _ ->
                    val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    onSeek(if (rtl) 1f - fraction else fraction)
                }
            },
    ) {
        val bars = if (peaks.isEmpty()) List(60) { 0.4f } else peaks
        val gap = size.width * 0.004f
        val barWidth = ((size.width - gap * (bars.size - 1)) / bars.size).coerceAtLeast(1f)
        val playedUntil = animated * bars.size
        bars.forEachIndexed { index, value ->
            val normalized = value.coerceIn(0.06f, 1f)
            val barHeight = size.height * (0.18f + normalized * 0.82f)
            val x = if (rtl) {
                size.width - (index + 1) * (barWidth + gap) + gap
            } else {
                index * (barWidth + gap)
            }
            val color = if (index < playedUntil) played else idle
            drawRoundRect(
                color = color,
                topLeft = Offset(x, (size.height - barHeight) / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
            )
        }
    }
}

/* ------------------------------------------------------------------------------------------
 * Small building blocks
 * ------------------------------------------------------------------------------------------ */

@Composable
fun FilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = espresso
    val bg by animateColorAsState(
        if (selected) palette.accent else palette.surfaceHigh,
        tween(180),
        label = "pill-bg",
    )
    val fg by animateColorAsState(
        if (selected) palette.accentInk else palette.textSecondary,
        tween(180),
        label = "pill-fg",
    )
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
fun LevelBadge(level: String, modifier: Modifier = Modifier) {
    val palette = espresso
    val color = when (level.lowercase()) {
        "beginner" -> palette.mint
        "intermediate" -> palette.accent
        else -> palette.lilac
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(
            text = Fa.level(level),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = espresso.textPrimary)
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelLarge,
                color = espresso.accent,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(enabled = onTrailingClick != null) { onTrailingClick?.invoke() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun ThinProgress(progress: Float, modifier: Modifier = Modifier, height: Dp = 4.dp) {
    val palette = espresso
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(palette.surfaceHigh),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(palette.accent, palette.mint))),
        )
    }
}

@Composable
fun SoftIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color? = null,
    background: Color? = null,
) {
    val palette = espresso
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background ?: palette.surfaceHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: palette.textPrimary,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color? = null,
) {
    val palette = espresso
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = accent ?: palette.textPrimary,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = palette.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val palette = espresso
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(palette.surfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textTertiary,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            action()
        }
    }
}

/* ------------------------------------------------------------------------------------------
 * Episode cards
 * ------------------------------------------------------------------------------------------ */

@Composable
fun EpisodeCard(
    episode: Episode,
    index: Int,
    progress: ProgressEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 190.dp,
) {
    val palette = espresso
    Column(
        modifier = modifier
            .width(width)
            .clip(RoundedCornerShape(24.dp))
            .background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        CoverArt(episode = episode, index = index, modifier = Modifier.fillMaxWidth().height(width - 20.dp))
        Spacer(Modifier.height(10.dp))
        Text(
            text = episode.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold).ltrStart(),
            color = palette.textPrimary,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LevelBadge(episode.level)
            Text(
                text = Fa.clock(episode.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = palette.textTertiary,
            )
        }
        if (progress != null && progress.positionMs > 0) {
            Spacer(Modifier.height(8.dp))
            ThinProgress(
                progress = if (progress.durationMs > 0) progress.positionMs.toFloat() / progress.durationMs else 0f,
            )
        }
    }
}

@Composable
fun EpisodeRow(
    episode: Episode,
    index: Int,
    progress: ProgressEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = espresso
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArt(episode = episode, index = index, modifier = Modifier.size(66.dp), corner = 16.dp, numberFraction = 0.30f)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold).ltrStart(),
                color = palette.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = episode.titleFa.ifBlank { episode.subtitle },
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LevelBadge(episode.level)
                Text(
                    text = Fa.clock(episode.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textTertiary,
                )
                Text(
                    text = "${Fa.num(episode.vocabCount)} واژه",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textTertiary,
                )
            }
            if (progress != null && progress.positionMs > 0) {
                Spacer(Modifier.height(8.dp))
                ThinProgress(
                    progress = if (progress.durationMs > 0) progress.positionMs.toFloat() / progress.durationMs else 0f,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    stroke: Dp = 8.dp,
    color: Color? = null,
) {
    val palette = espresso
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600), label = "ring")
    Canvas(modifier) {
        val width = stroke.toPx()
        drawArc(
            color = palette.surfaceHigh,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = width, cap = StrokeCap.Round),
            size = Size(size.width - width, size.height - width),
            topLeft = Offset(width / 2f, width / 2f),
        )
        drawArc(
            color = color ?: palette.accent,
            startAngle = -90f,
            sweepAngle = 360f * animated,
            useCenter = false,
            style = Stroke(width = width, cap = StrokeCap.Round),
            size = Size(size.width - width, size.height - width),
            topLeft = Offset(width / 2f, width / 2f),
        )
    }
}

/** English snippets must keep left-to-right order inside the RTL layout. */
fun TextStyle.ltr(): TextStyle = copy(textDirection = TextDirection.Ltr)

/** English line inside an RTL column: LTR order, aligned to the start of the row. */
fun TextStyle.ltrStart(): TextStyle = copy(
    textDirection = TextDirection.Ltr,
    // TextAlign.Start/End resolve against the layout direction (RTL here), so the physical
    // side has to be spelled out to keep English titles next to their cover.
    textAlign = TextAlign.Right,
)

/** Smooth pseudo-random bars used as a decorative element when no real peaks exist. */
fun decorativePeaks(seed: String, count: Int = 64): List<Float> {
    var hash = seed.hashCode()
    return List(count) { index ->
        hash = hash * 1103515245 + 12345
        val noise = abs(hash % 1000) / 1000f
        val wave = 0.45f + 0.35f * kotlin.math.sin(index * 0.5f)
        (wave * 0.6f + noise * 0.5f).coerceIn(0.15f, 1f)
    }
}

@Composable
fun HSpacer(width: Dp) = Spacer(Modifier.width(width))

@Composable
fun VSpacer(height: Dp) = Spacer(Modifier.height(height))

@Composable
fun ContentPadding(): PaddingValues = PaddingValues(horizontal = 18.dp)

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(espresso.outline),
    )
}

@Suppress("unused")
private val unusedTextUnit: TextUnit = 12.sp
