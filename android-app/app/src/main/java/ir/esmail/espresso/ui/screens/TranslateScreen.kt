package ir.esmail.espresso.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.esmail.espresso.data.OnlineTranslator
import ir.esmail.espresso.data.WordMeaning
import ir.esmail.espresso.playback.SoundManager
import ir.esmail.espresso.ui.AppViewModel
import ir.esmail.espresso.ui.components.SoftIconButton
import ir.esmail.espresso.ui.components.rememberSpeaker
import ir.esmail.espresso.ui.theme.Fraunces
import ir.esmail.espresso.ui.theme.espresso
import kotlinx.coroutines.launch

@Composable
fun TranslateScreen(vm: AppViewModel, bottomInset: Dp, onClose: () -> Unit) {
    val palette = espresso
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val speaker = rememberSpeaker()

    var sourceText by remember { mutableStateOf("") }
    var englishToPersian by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    val sourceLang = if (englishToPersian) "en" else "fa"
    val targetLang = if (englishToPersian) "fa" else "en"

    fun runTranslation() {
        scope.launch {
            loading = true
            error = null
            result = null
            saved = false
            val outcome = vm.translator.translate(sourceText, sourceLang, targetLang)
            loading = false
            outcome.onSuccess {
                result = it.text
                vm.playSound(SoundManager.SAVE)
            }.onFailure { failure ->
                error = failure.message ?: "خطای ناشناخته"
                vm.playSound(SoundManager.WRONG)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
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
                    text = "ترجمهٔ آنلاین",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary,
                )
                Text(
                    text = "به هر جملهای که در درس میبینی، سریع معنی بگیر",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textTertiary,
                )
            }
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(palette.accent, palette.mint))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Translate, contentDescription = null, tint = palette.accentInk)
            }
        }

        Spacer(Modifier.height(22.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surface)
                .padding(18.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (englishToPersian) "انگلیسی → فارسی" else "فارسی → انگلیسی",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.accent,
                    )
                    Spacer(Modifier.weight(1f))
                    SoftIconButton(
                        icon = Icons.Rounded.SwapHoriz,
                        contentDescription = "تغییر جهت ترجمه",
                        onClick = {
                            englishToPersian = !englishToPersian
                            result = null
                            error = null
                            vm.playSound(SoundManager.TAP)
                        },
                        size = 36.dp,
                        background = palette.accentSoft,
                        tint = palette.accent,
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = sourceText,
                    onValueChange = { if (it.length <= 500) sourceText = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = {
                        Text(
                            if (englishToPersian) "جملهای را از درس اینجا وارد کن..."
                            else "یک جمله یا واژهٔ فارسی بنویس..."
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.outline,
                        cursorColor = palette.accent,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary,
                        focusedPlaceholderColor = palette.textTertiary,
                        unfocusedPlaceholderColor = palette.textTertiary,
                    ),
                    textStyle = TextStyle(
                        textDirection = if (englishToPersian) TextDirection.Ltr else TextDirection.Rtl,
                        textAlign = if (englishToPersian) TextAlign.Start else TextAlign.Right,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${sourceText.length} / ۵۰۰",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textTertiary,
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { runTranslation() },
                    enabled = sourceText.isNotBlank() && !loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accent,
                        contentColor = palette.accentInk,
                    ),
                ) {
                    if (loading) {
                        Box(Modifier.size(20.dp).clip(RoundedCornerShape(10.dp)).border(2.dp, palette.accentInk, RoundedCornerShape(10.dp)))
                        Spacer(Modifier.width(10.dp))
                    }
                    Text("ترجمه کن", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn() + slideInVertically { it / 4 },
        ) {
            error?.let { message ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.danger.copy(alpha = 0.12f))
                        .padding(16.dp),
                ) {
                    Text(text = message, color = palette.danger, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        AnimatedVisibility(
            visible = result != null,
            enter = fadeIn() + slideInVertically { it / 5 },
        ) {
            result?.let { translated ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(palette.accentSoft, palette.surface, palette.surface),
                            ),
                        )
                        .padding(18.dp),
                ) {
                    Column {
                        Text(
                            text = "ترجمه",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.accent,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = translated,
                            fontFamily = if (englishToPersian) null else Fraunces,
                            fontSize = 20.sp,
                            lineHeight = 30.sp,
                            color = palette.textPrimary,
                            style = TextStyle(
                                textDirection = if (englishToPersian) TextDirection.Rtl else TextDirection.Ltr,
                                textAlign = if (englishToPersian) TextAlign.Right else TextAlign.Start,
                            ),
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SoftIconButton(
                                icon = Icons.Rounded.ContentCopy,
                                contentDescription = "کپی",
                                onClick = {
                                    clipboard.setText(AnnotatedString(translated))
                                    vm.playSound(SoundManager.TAP)
                                },
                                size = 40.dp,
                                background = palette.surfaceHigh,
                                tint = palette.textSecondary,
                            )
                            if (!englishToPersian && speaker.available && sourceText.isNotBlank()) {
                                SoftIconButton(
                                    icon = Icons.Rounded.VolumeUp,
                                    contentDescription = "تلفظ",
                                    onClick = { speaker.say(translated) },
                                    size = 40.dp,
                                    background = palette.surfaceHigh,
                                    tint = palette.accent,
                                )
                            }
                            SoftIconButton(
                                icon = Icons.Rounded.SaveAlt,
                                contentDescription = "ذخیره در دفترچه",
                                onClick = {
                                    vm.toggleWord(
                                        WordMeaning(
                                            term = sourceText.trim(),
                                            fa = translated,
                                            source = "ترجمهٔ آنلاین",
                                        ),
                                        null,
                                    ) { nowSaved ->
                                        saved = nowSaved
                                        vm.playSound(if (nowSaved) SoundManager.SAVE else SoundManager.WRONG)
                                    }
                                },
                                size = 40.dp,
                                background = if (saved) palette.mint.copy(alpha = 0.18f) else palette.surfaceHigh,
                                tint = if (saved) palette.mint else palette.textSecondary,
                            )
                        }
                        if (saved) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "در دفترچه ذخیره شد",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.mint,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.surfaceHigh)
                .clickable {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://translate.google.com/?sl=$sourceLang&tl=$targetLang"),
                    )
                    runCatching { context.startActivity(intent) }
                }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = "باز کردن ترجمهٔ گوگل در مرورگر",
                style = MaterialTheme.typography.labelLarge,
                color = palette.textSecondary,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "سرویس: ${OnlineTranslator.SOURCE_NAME} — برای ترجمه به اینترنت نیاز است.",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textTertiary,
        )
        Spacer(Modifier.height(bottomInset + 24.dp))
    }
}
