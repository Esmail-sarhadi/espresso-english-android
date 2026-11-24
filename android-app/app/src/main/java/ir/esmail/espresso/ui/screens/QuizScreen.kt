package ir.esmail.espresso.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.esmail.espresso.data.ContentBundle
import ir.esmail.espresso.data.Episode
import ir.esmail.espresso.data.QuizBuilder
import ir.esmail.espresso.data.QuizKind
import ir.esmail.espresso.playback.SoundManager
import ir.esmail.espresso.ui.AppViewModel
import ir.esmail.espresso.ui.Fa
import ir.esmail.espresso.ui.components.EmptyState
import ir.esmail.espresso.ui.components.ProgressRing
import ir.esmail.espresso.ui.components.SoftIconButton
import ir.esmail.espresso.ui.components.rememberSpeaker
import ir.esmail.espresso.ui.theme.Fraunces
import ir.esmail.espresso.ui.theme.espresso

@Composable
fun QuizScreen(
    vm: AppViewModel,
    episode: Episode,
    bundle: ContentBundle,
    onClose: () -> Unit,
) {
    val palette = espresso
    val questions = remember(episode.id) { QuizBuilder.build(episode, bundle.glossary, 14) }
    val speaker = rememberSpeaker()

    var index by rememberSaveable { mutableStateOf(0) }
    var selected by rememberSaveable { mutableStateOf(-1) }
    var correctCount by rememberSaveable { mutableStateOf(0) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var recorded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(finished) {
        if (finished && !recorded) {
            recorded = true
            vm.recordQuiz(episode, correctCount, questions.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SoftIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "بستن",
                onClick = onClose,
                background = Color.Transparent,
            )
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "آزمون درس",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textTertiary,
                )
                Text(
                    text = episode.titleFa.ifBlank { episode.title },
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(40.dp))
        }

        if (questions.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.EmojiEvents,
                title = "آزمونی برای این درس نیست",
                message = "برای ساختن آزمون به واژههای ترجمهشده نیاز است.",
                modifier = Modifier.weight(1f),
            )
            return@Column
        }

        if (!finished) {
            val question = questions[index.coerceIn(0, questions.lastIndex)]
            val locked = selected >= 0
            val progress by animateFloatAsState(
                targetValue = (index.toFloat() / questions.size).coerceIn(0f, 1f),
                animationSpec = tween(320),
                label = "quiz-progress",
            )

            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "سؤال ${Fa.num(index + 1)} از ${Fa.num(questions.size)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.textSecondary,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${Fa.num(correctCount)} درست",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.mint,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(palette.surfaceHigh),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(palette.accent),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                AnimatedContent(
                    targetState = question,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 14 }) togetherWith
                            (fadeOut(tween(110)) + slideOutHorizontally(tween(110)) { -it / 18 })
                    },
                    label = "question",
                ) { q ->
                    val promptRtl = q.kind == QuizKind.REVERSE || q.kind == QuizKind.TRANSLATE
                    Column {
                        Text(
                            text = q.hint,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.accent,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = q.prompt,
                                fontFamily = if (promptRtl) null else Fraunces,
                                fontSize = if (promptRtl) 21.sp else 23.sp,
                                lineHeight = 32.sp,
                                color = palette.textPrimary,
                                textAlign = TextAlign.Start,
                                style = TextStyle(
                                    textDirection = if (promptRtl) TextDirection.Rtl else TextDirection.Ltr,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            if (q.speakable.isNotBlank() && speaker.available &&
                                q.kind != QuizKind.CLOZE && q.kind != QuizKind.PRONUNCIATION) {
                                Spacer(Modifier.width(8.dp))
                                SoftIconButton(
                                    icon = Icons.Rounded.VolumeUp,
                                    contentDescription = "شنیدن",
                                    onClick = { speaker.say(q.speakable) },
                                    size = 38.dp,
                                    tint = palette.accent,
                                    background = palette.accentSoft,
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        q.options.forEachIndexed { optionIndex, option ->
                            val isCorrect = optionIndex == q.correctIndex
                            val isChosen = optionIndex == selected
                            val showResult = locked
                            val borderColor = when {
                                showResult && isCorrect -> palette.mint
                                showResult && isChosen -> palette.danger
                                else -> palette.outline
                            }
                            val bg by animateColorAsState(
                                when {
                                    showResult && isCorrect -> palette.mint.copy(alpha = 0.14f)
                                    showResult && isChosen -> palette.danger.copy(alpha = 0.14f)
                                    else -> palette.surface
                                },
                                label = "option",
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bg)
                                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                                    .clickable(enabled = !locked) {
                                        selected = optionIndex
                                        vm.playSound(if (isCorrect) SoundManager.CORRECT else SoundManager.WRONG)
                                        if (isCorrect) correctCount += 1
                                    }
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(palette.surfaceHigh),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = Fa.num(optionIndex + 1),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary,
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        textDirection = if (q.kind == QuizKind.MEANING) {
                                            TextDirection.Rtl
                                        } else {
                                            TextDirection.Ltr
                                        },
                                        fontWeight = if (showResult && isCorrect) FontWeight.Bold else FontWeight.Normal,
                                    ),
                                    color = palette.textPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = locked,
                    enter = fadeIn() + slideInVertically { it / 3 },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(palette.surfaceHigh)
                            .padding(16.dp)
                            .animateContentSize(),
                    ) {
                        Text(
                            text = if (selected == question.correctIndex) "درست بود" else "پاسخ درست مشخص شد",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selected == question.correctIndex) palette.mint else palette.danger,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (locked) palette.accent else palette.surfaceHigh)
                        .clickable(enabled = locked) {
                            if (index + 1 >= questions.size) {
                                vm.playSound(SoundManager.COMPLETE)
                                finished = true
                            } else {
                                vm.playSound(SoundManager.TAP)
                                index += 1
                                selected = -1
                            }
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (index + 1 >= questions.size) "دیدن نتیجه" else "سؤال بعد",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (locked) palette.accentInk else palette.textTertiary,
                    )
                }
                Spacer(Modifier.height(30.dp))
            }
        } else {
            ResultView(
                score = correctCount,
                total = questions.size,
                onRetry = {
                    index = 0
                    selected = -1
                    correctCount = 0
                    finished = false
                    recorded = false
                },
                onClose = onClose,
            )
        }
    }
}

@Composable
private fun ResultView(
    score: Int,
    total: Int,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    val palette = espresso
    val ratio = if (total == 0) 0f else score.toFloat() / total
    val message = when {
        ratio >= 0.9f -> "عالی بود! این درس را کامل گرفتی."
        ratio >= 0.6f -> "خوب بود. یک بار دیگر مرور کن تا کامل شود."
        else -> "نگران نباش، دوباره گوش بده و امتحان کن."
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 8 },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))
            Box(contentAlignment = Alignment.Center) {
                ProgressRing(
                    progress = ratio,
                    modifier = Modifier.size(168.dp),
                    stroke = 12.dp,
                    color = if (ratio >= 0.6f) palette.mint else palette.accent,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score/$total",
                        fontFamily = Fraunces,
                        fontSize = 40.sp,
                        color = palette.textPrimary,
                    )
                    Text(
                        text = Fa.digits("${(ratio * 100).toInt()}%"),
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.textTertiary,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(36.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.surfaceHigh)
                        .clickable(onClick = onRetry)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = palette.textSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "تلاش دوباره",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.textSecondary,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(palette.accent, palette.accent)))
                        .clickable(onClick = onClose)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "بازگشت به درس",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.accentInk,
                    )
                }
            }
        }
    }
}
