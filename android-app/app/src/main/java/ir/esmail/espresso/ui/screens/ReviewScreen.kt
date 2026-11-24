package ir.esmail.espresso.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.esmail.espresso.data.Rating
import ir.esmail.espresso.data.SavedWordEntity
import ir.esmail.espresso.ui.AppViewModel
import ir.esmail.espresso.ui.Fa
import ir.esmail.espresso.ui.components.EmptyState
import ir.esmail.espresso.ui.components.SoftIconButton
import ir.esmail.espresso.ui.components.ThinProgress
import ir.esmail.espresso.ui.components.rememberSpeaker
import ir.esmail.espresso.ui.theme.Fraunces
import ir.esmail.espresso.ui.theme.espresso

@Composable
fun ReviewScreen(
    vm: AppViewModel,
    bottomInset: Dp,
    onGoToNotebook: () -> Unit,
) {
    val palette = espresso
    val speaker = rememberSpeaker()

    var due by remember { mutableStateOf<List<SavedWordEntity>?>(null) }
    var duePhrases by remember { mutableStateOf<List<ir.esmail.espresso.data.SavedPhraseEntity>?>(null) }
    var mode by remember { mutableStateOf(0) } // 0 = words, 1 = phrases
    var index by remember { mutableStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var reviewed by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        due = vm.dueWords(40)
        duePhrases = vm.duePhrases(40)
    }

    val cards = due.orEmpty()
    val current = cards.getOrNull(index)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottom = bottomInset),
    ) {
        Column(Modifier.padding(horizontal = 18.dp)) {
            Text(
                text = "مرور",
                style = MaterialTheme.typography.headlineLarge,
                color = palette.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "تکرار فاصله‌دار: هر واژه درست وقتی که نزدیک فراموشی‌اش هستی برمی‌گردد.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textTertiary,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ir.esmail.espresso.ui.components.FilterPill("واژهها", mode == 0, onClick = {
                    mode = 0; index = 0; revealed = false; reviewed = 0
                })
                ir.esmail.espresso.ui.components.FilterPill("عبارتها", mode == 1, onClick = {
                    mode = 1; index = 0; revealed = false; reviewed = 0
                })
            }
            Spacer(Modifier.height(12.dp))
        }

        if (mode == 1) {
            PhraseReview(
                due = duePhrases,
                index = index,
                revealed = revealed,
                reviewed = reviewed,
                onRate = { phrase, rating ->
                    vm.ratePhrase(phrase, rating)
                    reviewed += 1
                    index += 1
                    revealed = false
                },
                onReveal = { revealed = true },
                onGoToNotebook = onGoToNotebook,
                bottomInset = bottomInset,
            )
            return@Column
        }

        when {
            due == null -> {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(text = "…", color = palette.textTertiary)
                }
            }

            current == null -> {
                EmptyState(
                    icon = if (cards.isEmpty()) Icons.Rounded.Style else Icons.Rounded.CheckCircle,
                    title = if (cards.isEmpty()) "چیزی برای مرور نیست" else "مرور امروز تمام شد",
                    message = if (cards.isEmpty()) {
                        "واژه‌های تازه را از متن درس‌ها ذخیره کن تا این‌جا مرور شوند."
                    } else {
                        "${Fa.num(reviewed)} واژه را مرور کردی. فردا دوباره سر بزن."
                    },
                    modifier = Modifier.weight(1f),
                    action = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.accent)
                                .clickable(onClick = onGoToNotebook)
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = "رفتن به دفترچه",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.accentInk,
                            )
                        }
                    },
                )
            }

            else -> {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${Fa.num(index + 1)} از ${Fa.num(cards.size)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.textSecondary,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${Fa.num(reviewed)} مرور شده",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textTertiary,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    ThinProgress(progress = index.toFloat() / cards.size)
                }

                Spacer(Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = current.id to revealed,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "card",
                    ) { (_, isRevealed) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(28.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(palette.surfaceHigh, palette.surface),
                                    ),
                                )
                                .border(1.dp, palette.outline, RoundedCornerShape(28.dp))
                                .clickable { revealed = true }
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "واژه",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textTertiary,
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = current.term,
                                    fontFamily = Fraunces,
                                    fontSize = 30.sp,
                                    color = palette.textPrimary,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.width(8.dp))
                                if (speaker.available) {
                                    SoftIconButton(
                                        icon = Icons.Rounded.VolumeUp,
                                        contentDescription = "شنیدن",
                                        onClick = { speaker.say(current.term) },
                                        size = 34.dp,
                                        tint = palette.accent,
                                        background = palette.accentSoft,
                                    )
                                }
                            }
                            if (current.ipa.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = current.ipa,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textTertiary,
                                )
                            }

                            Spacer(Modifier.height(22.dp))

                            if (isRevealed) {
                                Text(
                                    text = current.meaning,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = palette.accent,
                                    textAlign = TextAlign.Center,
                                )
                                if (current.definition.isNotBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = current.definition,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textSecondary,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                if (current.example.isNotBlank()) {
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        text = current.example,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textTertiary,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            } else {
                                Spacer(Modifier.height(30.dp))
                                Text(
                                    text = "برای دیدن معنی، روی کارت بزن",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textTertiary,
                                )
                                Spacer(Modifier.height(30.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                if (revealed) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RatingButton("دوباره", palette.danger, Modifier.weight(1f)) {
                            vm.rateWord(current, Rating.AGAIN)
                            reviewed += 1
                            index += 1
                            revealed = false
                        }
                        RatingButton("خوب", palette.accent, Modifier.weight(1f)) {
                            vm.rateWord(current, Rating.GOOD)
                            reviewed += 1
                            index += 1
                            revealed = false
                        }
                        RatingButton("آسان", palette.mint, Modifier.weight(1f)) {
                            vm.rateWord(current, Rating.EASY)
                            reviewed += 1
                            index += 1
                            revealed = false
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.accent)
                            .clickable { revealed = true }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "نمایش معنی",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.accentInk,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun RatingButton(label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun PhraseReview(
    due: List<ir.esmail.espresso.data.SavedPhraseEntity>?,
    index: Int,
    revealed: Boolean,
    reviewed: Int,
    onRate: (ir.esmail.espresso.data.SavedPhraseEntity, ir.esmail.espresso.data.Rating) -> Unit,
    onReveal: () -> Unit,
    onGoToNotebook: () -> Unit,
    bottomInset: Dp,
) {
    val palette = espresso
    val speaker = rememberSpeaker()
    val current = due?.getOrNull(index)

    when {
        due == null -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Text("…", color = palette.textTertiary)
        }

        current == null -> EmptyState(
            icon = if (due.isEmpty()) Icons.Rounded.Style else Icons.Rounded.CheckCircle,
            title = if (due.isEmpty()) "عبارتی برای مرور نیست" else "مرور عبارتها تمام شد",
            message = if (due.isEmpty()) {
                "عبارتها را از صفحهٔ درس ذخیره کن تا اینجا مرور شوند."
            } else {
                "${Fa.num(reviewed)} عبارت را مرور کردی."
            },
            modifier = Modifier.fillMaxWidth().height(220.dp),
            action = {
                Box(
                    Modifier.clip(RoundedCornerShape(14.dp)).background(palette.accent)
                        .clickable(onClick = onGoToNotebook).padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Text("رفتن به دفترچه", style = MaterialTheme.typography.labelLarge, color = palette.accentInk)
                }
            },
        )

        else -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${Fa.num(index + 1)} از ${Fa.num(due.size)}", style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
                Spacer(Modifier.weight(1f))
                Text("${Fa.num(reviewed)} مرور شده", style = MaterialTheme.typography.labelSmall, color = palette.textTertiary)
            }
            Spacer(Modifier.height(8.dp))
            ThinProgress(progress = index.toFloat() / due.size)
            Spacer(Modifier.height(18.dp))

            Box(
                Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(palette.surfaceHigh, palette.surface)))
                    .border(1.dp, palette.outline, RoundedCornerShape(28.dp))
                    .clickable(onClick = onReveal).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("عبارت", style = MaterialTheme.typography.labelSmall, color = palette.textTertiary)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            current.en,
                            fontFamily = Fraunces,
                            fontSize = 24.sp,
                            color = palette.textPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                        if (speaker.available) {
                            SoftIconButton(
                                icon = Icons.Rounded.VolumeUp,
                                contentDescription = "شنیدن",
                                onClick = { speaker.say(current.en) },
                                size = 34.dp,
                                tint = palette.accent,
                                background = palette.accentSoft,
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    if (revealed) {
                        Text(current.fa, style = MaterialTheme.typography.headlineSmall, color = palette.accent, textAlign = TextAlign.Center)
                        if (current.note.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(current.note, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary, textAlign = TextAlign.Center)
                        }
                    } else {
                        Text("برای دیدن معنی، روی کارت بزن", style = MaterialTheme.typography.bodyMedium, color = palette.textTertiary)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            if (revealed) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RatingButton("دوباره", palette.danger, Modifier.weight(1f)) { onRate(current, ir.esmail.espresso.data.Rating.AGAIN) }
                    RatingButton("خوب", palette.accent, Modifier.weight(1f)) { onRate(current, ir.esmail.espresso.data.Rating.GOOD) }
                    RatingButton("آسان", palette.mint, Modifier.weight(1f)) { onRate(current, ir.esmail.espresso.data.Rating.EASY) }
                }
            } else {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(palette.accent)
                        .clickable(onClick = onReveal).padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("نمایش معنی", style = MaterialTheme.typography.titleMedium, color = palette.accentInk)
                }
            }
            Spacer(Modifier.height(bottomInset + 12.dp))
        }
    }
}
