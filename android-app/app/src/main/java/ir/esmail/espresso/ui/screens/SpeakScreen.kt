package ir.esmail.espresso.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.esmail.espresso.data.ContentBundle
import ir.esmail.espresso.data.PronunciationScorer
import ir.esmail.espresso.data.PronunciationScore
import ir.esmail.espresso.data.VocabItem
import ir.esmail.espresso.playback.SoundManager
import ir.esmail.espresso.ui.AppViewModel
import ir.esmail.espresso.ui.Fa
import ir.esmail.espresso.ui.components.SoftIconButton
import ir.esmail.espresso.ui.components.rememberSpeaker
import ir.esmail.espresso.ui.theme.espresso
import kotlinx.coroutines.launch

/**
 * Speaking practice: pick any word or phrase from the whole course, hear the reference voice,
 * say it into the mic, and get a word-by-word accuracy score backed by Android speech recognition.
 */
@Composable
fun SpeakScreen(vm: AppViewModel, bundle: ContentBundle, bottomInset: Dp, onClose: () -> Unit) {
    val palette = espresso
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val speaker = rememberSpeaker()

    data class Practice(val id: String, val text: String, val meaning: String, val source: String)
    val practiceItems = remember(bundle) {
        val list = ArrayList<Practice>()
        bundle.episodes.forEach { ep ->
            ep.vocab.filter { it.term.isNotBlank() }.forEach {
                list += Practice("w:${ep.id}:${it.term}", it.term, it.fa, ep.titleFa.ifBlank { ep.title })
            }
            ep.phrases.filter { it.en.isNotBlank() }.forEach {
                list += Practice("p:${ep.id}:${it.en}", it.en, it.fa, ep.titleFa.ifBlank { ep.title })
            }
        }
        list.distinctBy { it.text.lowercase() }
    }

    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<Practice?>(null) }
    var score by remember { mutableStateOf<PronunciationScore?>(null) }
    var listening by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val attempts by vm.attempts.collectAsStateWithLifecycle()

    fun handleResult(recognized: String) {
        val target = selected?.text ?: return
        val result = PronunciationScorer.score(target, recognized)
        score = result
        vm.recordAttempt(target, recognized, result.overall)
        vm.playSound(if (result.overall >= 70) SoundManager.CORRECT else SoundManager.WRONG)
        message = null
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        listening = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val heard = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (heard.isNullOrEmpty()) {
                message = "چیزی شنیده نشد. دوباره تلاش کن."
            } else {
                handleResult(heard.first())
            }
        } else {
            message = "تشخیص صوت متوقف شد."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && selected != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, selected!!.text)
            }
            runCatching { speechLauncher.launch(intent) }
                .onFailure { message = "تشخیص گفتار در این دستگاه در دسترس نیست." }
        } else {
            message = "برای تمرین تلفظ، اجازهٔ میکروفون لازم است."
        }
    }

    fun startListening() {
        if (selected == null) return
        score = null
        message = null
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, selected!!.text)
                }
                listening = true
                runCatching { speechLauncher.launch(intent) }
                    .onFailure { listening = false; message = "تشخیص گفتار در این دستگاه در دسترس نیست." }
            }
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (selected == null) {
        val filtered = remember(query, practiceItems) {
            val q = query.trim().lowercase()
            if (q.length < 2) practiceItems.take(40)
            else practiceItems.filter { it.text.lowercase().contains(q) || it.meaning.contains(query.trim()) }.take(80)
        }
        Column(
            Modifier.fillMaxSize().background(palette.background).statusBarsPadding().padding(horizontal = 20.dp),
        ) {
            ScreenHeader(title = "تمرین تلفظ", subtitle = "یک واژه یا عبارت انتخاب کن و آن را بگو", onClose = onClose)
            Spacer(Modifier.height(16.dp))
            SearchField(value = query, onValueChange = { query = it }, placeholder = "واژه یا عبارتی جستجو کن…")
            Spacer(Modifier.height(8.dp))
            Text(
                "${Fa.num(attempts.size)} تمرین ثبتشده · میانگین ${Fa.num(if (attempts.isEmpty()) 0 else attempts.sumOf { it.score } / attempts.size)}٪",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textTertiary,
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = bottomInset + 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(palette.surface).border(1.dp, palette.outline, RoundedCornerShape(14.dp))
                            .clickable {
                                selected = item
                                score = null
                                message = null
                            }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.text, style = MaterialTheme.typography.titleSmall.copy(textDirection = TextDirection.Ltr), color = palette.textPrimary)
                            Text(item.meaning, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                            Text(item.source, style = MaterialTheme.typography.labelSmall, color = palette.textTertiary)
                        }
                        if (speaker.available) {
                            SoftIconButton(Icons.Rounded.VolumeUp, "پخش تلفظ", { speaker.say(item.text) },
                                background = palette.accentSoft, tint = palette.accent)
                        }
                    }
                }
            }
        }
        return
    }

    val current = selected!!
    Column(
        Modifier.fillMaxSize().background(palette.background).statusBarsPadding().padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SoftIconButton(Icons.Rounded.Close, "بازگشت", {
                selected = null
                score = null
            }, background = Color.Transparent)
            Spacer(Modifier.width(8.dp))
            Text("تمرین تلفظ", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
        }
        Spacer(Modifier.height(28.dp))

        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(palette.surface)
                .border(1.dp, palette.outline, RoundedCornerShape(24.dp)).padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    current.text,
                    style = MaterialTheme.typography.headlineSmall.copy(textDirection = TextDirection.Ltr),
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(current.meaning, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            if (speaker.available) {
                Button(
                    onClick = { speaker.say(current.text) },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceHigh, contentColor = palette.textPrimary),
                ) {
                    Icon(Icons.Rounded.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(" بشنو")
                }
            }
        }

        AnimatedContent(score != null, label = "score") { hasScore ->
            if (hasScore && score != null) {
                Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(64.dp).clip(CircleShape)
                                .background(if (score!!.overall >= 70) palette.mint.copy(alpha = 0.2f) else palette.danger.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(Fa.num(score!!.overall) + "٪", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(if (score!!.overall >= 70) "عالی!" else "ادامه بده", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text("شنیده شد: ${score!!.recognized}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        score!!.words.forEach { word ->
                            Box(
                                Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(if (word.correct) palette.mint.copy(alpha = 0.18f) else palette.danger.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (word.correct) Icon(Icons.Rounded.Check, null, Modifier.size(13.dp), palette.mint)
                                    else Icon(Icons.Rounded.Close, null, Modifier.size(13.dp), palette.danger)
                                    Spacer(Modifier.width(4.dp))
                                    Text(word.word, style = MaterialTheme.typography.labelMedium, color = palette.textPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = palette.danger)
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = { startListening() },
            enabled = !listening,
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.accentInk),
            modifier = Modifier.fillMaxWidth().padding(bottom = bottomInset + 16.dp),
        ) {
            Icon(Icons.Rounded.Mic, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (listening) "در حال گوش دادن…" else if (score == null) "بگو" else "دوباره بگو")
        }
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String, onClose: () -> Unit) {
    val palette = espresso
    Row(verticalAlignment = Alignment.CenterVertically) {
        SoftIconButton(Icons.Rounded.Close, "بستن", onClose, background = Color.Transparent)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.textTertiary)
        }
    }
}
