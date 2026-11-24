package ir.esmail.espresso.ui.components

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.esmail.espresso.data.WordMeaning
import ir.esmail.espresso.playback.PlayerUiState
import ir.esmail.espresso.ui.Fa
import ir.esmail.espresso.ui.theme.Fraunces
import ir.esmail.espresso.ui.theme.espresso
import ir.esmail.espresso.ui.components.ltrStart
import java.util.Locale

/* ------------------------------------------------------------------------------------------
 * Text to speech – used to hear a single word. Created lazily, released with the composition.
 * ------------------------------------------------------------------------------------------ */

class Speaker(private val engine: TextToSpeech?) {

    val available: Boolean get() = engine != null

    fun say(text: String, rate: Float = 0.9f) {
        val tts = engine ?: return
        runCatching {
            tts.setSpeechRate(rate)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "espresso-word")
        }
    }
}

@Composable
fun rememberSpeaker(): Speaker {
    val context = LocalContext.current
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        var created: TextToSpeech? = null
        created = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                runCatching {
                    created?.language = Locale.US
                    created?.setPitch(1f)
                }
            }
        }
        engine = created
        onDispose {
            runCatching {
                created?.stop()
                created?.shutdown()
            }
            engine = null
        }
    }
    return remember(engine) { Speaker(engine) }
}

/* ------------------------------------------------------------------------------------------
 * Mini player – always docked above the navigation bar.
 * ------------------------------------------------------------------------------------------ */

@Composable
fun MiniPlayer(
    state: PlayerUiState,
    title: String,
    subtitle: String,
    cover: (@Composable () -> Unit)?,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = espresso
    val progress = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface)
            .clickable(onClick = onOpen),
    ) {
        ThinProgress(progress = progress, height = 2.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (cover != null) {
                cover()
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.ltrStart(),
                    color = palette.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(palette.accent)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = palette.accentInk,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "بستن",
                    tint = palette.textTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------------------------
 * Word sheet
 * ------------------------------------------------------------------------------------------ */

@Composable
fun WordSheet(
    token: String,
    meaning: WordMeaning?,
    saved: Boolean,
    onDismiss: () -> Unit,
    onToggleSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = espresso
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val speaker = rememberSpeaker()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        contentColor = palette.textPrimary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(palette.outline),
            )
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = meaning?.term ?: token,
                    fontFamily = Fraunces,
                    fontSize = 30.sp,
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(10.dp))
                if (speaker.available) {
                    SoftIconButton(
                        icon = Icons.Rounded.VolumeUp,
                        contentDescription = "شنیدن تلفظ",
                        onClick = { speaker.say(meaning?.term ?: token) },
                        size = 38.dp,
                        tint = palette.accent,
                        background = palette.accentSoft,
                    )
                }
            }

            meaning?.ipa?.takeIf { it.isNotBlank() }?.let { ipa ->
                Spacer(Modifier.height(2.dp))
                Text(text = ipa, style = MaterialTheme.typography.bodyMedium, color = palette.textTertiary)
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!meaning?.pos.isNullOrBlank()) {
                    Tag(text = Fa.partOfSpeech(meaning?.pos ?: ""), color = palette.lilac)
                }
                if (!meaning?.source.isNullOrBlank()) {
                    Tag(text = meaning?.source ?: "", color = palette.mint)
                }
            }

            Spacer(Modifier.height(18.dp))

            if (meaning == null || meaning.fa.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.surfaceHigh)
                        .padding(16.dp),
                ) {
                    Text(
                        text = "برای این واژه معنی ثبت نشده است. می‌توانی معنی‌اش را از واژه‌نامه‌های آنلاین ببینی.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                    )
                }
            } else {
                Text(
                    text = meaning.fa,
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.textPrimary,
                )
            }

            meaning?.en?.takeIf { it.isNotBlank() }?.let { definition ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = definition,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                )
            }

            meaning?.example?.takeIf { it.isNotBlank() }?.let { example ->
                Spacer(Modifier.height(16.dp))
                Row {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(palette.accent),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = example,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                        color = palette.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onToggleSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (saved) palette.surfaceHigh else palette.accent,
                    contentColor = if (saved) palette.textPrimary else palette.accentInk,
                ),
            ) {
                Icon(
                    imageVector = if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (saved) "حذف از دفترچه" else "افزودن به دفترچه",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun Tag(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
