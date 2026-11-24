package ir.esmail.espresso.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.projection.MediaProjectionManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.BubbleChart
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.esmail.espresso.capture.ScreenCaptureBus
import ir.esmail.espresso.capture.FloatingTranslateService
import ir.esmail.espresso.capture.ScreenCaptureService
import ir.esmail.espresso.data.ScreenTextRecognizer
import ir.esmail.espresso.playback.SoundManager
import ir.esmail.espresso.ui.AppViewModel
import ir.esmail.espresso.ui.components.SoftIconButton
import ir.esmail.espresso.ui.theme.espresso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScannedLine(val english: String, val persian: String = "", val pending: Boolean = false)

/**
 * Reads text off the screen and translates it online.
 *
 * Three independent sources are offered because live capture is not guaranteed everywhere:
 * live capture uses MediaProjection, "انتخاب تصویر" reads any screenshot the user picks from
 * the gallery (no permission needed, works on every OEM), and "چسباندن متن" translates text
 * pasted from the clipboard.
 */
@Composable
fun ScreenScanScreen(vm: AppViewModel, bottomInset: Dp, onClose: () -> Unit) {
    val palette = espresso
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val recognizer = remember { ScreenTextRecognizer() }
    val captureState by ScreenCaptureBus.state.collectAsStateWithLifecycle()

    var lines by remember { mutableStateOf<List<ScannedLine>>(emptyList()) }
    var status by remember { mutableStateOf<String?>(null) }
    var translating by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showPaste by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose {
            ScreenCaptureBus.clear()
            recognizer.close()
        }
    }

    fun translateAll(englishLines: List<String>) {
        if (englishLines.isEmpty()) {
            status = "هیچ متنی پیدا نشد."
            vm.playSound(SoundManager.WRONG)
            return
        }
        scope.launch {
            translating = true
            lines = englishLines.map { ScannedLine(it, "", true) }
            vm.translator.translateLines(
                lines = englishLines,
                from = "en",
                to = "fa",
                onProgress = { _, _, map ->
                    withContext(Dispatchers.Main) {
                        lines = englishLines.mapIndexed { index, text ->
                            ScannedLine(text, map[index].orEmpty(), pending = !map.containsKey(index))
                        }
                    }
                },
            )
            lines = lines.map { it.copy(pending = false) }
            translating = false
            vm.playSound(SoundManager.SAVE)
        }
    }

    fun readBitmap(bitmap: Bitmap, origin: String) {
        scope.launch {
            busy = true
            status = "متن تصویر $origin خوانده شد؛ در حال ترجمه…"
            recognizer.read(bitmap)
                .onSuccess { found ->
                    if (found.isEmpty()) {
                        status = "در این تصویر متنی شناسایی نشد."
                        vm.playSound(SoundManager.WRONG)
                    } else {
                        status = "${found.size} خط شناسایی شد"
                        translateAll(found)
                    }
                }
                .onFailure {
                    status = "خواندن متن تصویر ناموفق بود: ${it.message ?: "خطا"}"
                    vm.playSound(SoundManager.WRONG)
                }
            busy = false
        }
    }

    val captureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
            }
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure { status = it.message ?: "سرویس ضبط صفحه آغاز نشد." }
        } else {
            ScreenCaptureBus.fail("اجازهٔ ضبط صفحه رد شد.")
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bitmap = runCatching { decodeForOcr(context, uri) }.getOrNull()
        if (bitmap == null) {
            status = "تصویر خوانده نشد. تصویر دیگری انتخاب کن."
            vm.playSound(SoundManager.WRONG)
        } else {
            readBitmap(bitmap, "اسکرین‌شات")
        }
    }

    val bubbleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = Intent(context, FloatingTranslateService::class.java).apply {
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
            }
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onSuccess {
                    status = "حباب فعال شد؛ حالا از هر برنامه‌ای روی آن بزن."
                    vm.playSound(SoundManager.SAVE)
                }
                .onFailure { status = it.message ?: "حباب باز نشد." }
        } else {
            status = "اجازهٔ ضبط صفحه رد شد."
        }
    }

    fun startBubble() {
        if (!android.provider.Settings.canDrawOverlays(context)) {
            status = "اجازهٔ «نمایش روی برنامه‌های دیگر» را روشن کن، بعد دوباره بزن."
            runCatching {
                context.startActivity(
                    Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}"),
                    ),
                )
            }
            return
        }
        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        runCatching { bubbleLauncher.launch(manager.createScreenCaptureIntent()) }
            .onFailure { status = it.message ?: "ضبط صفحه آغاز نشد." }
    }

    fun startScan() {
        lines = emptyList()
        status = null
        ScreenCaptureBus.clear()
        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        runCatching { captureLauncher.launch(manager.createScreenCaptureIntent()) }
            .onFailure {
                status = it.message ?: "ضبط صفحه آغاز نشد."
                showHelp = true
            }
    }

    LaunchedEffect(captureState.sequence, captureState.running, captureState.bitmap, captureState.error) {
        when {
            captureState.error != null -> {
                status = captureState.error
                showHelp = true
                vm.playSound(SoundManager.WRONG)
            }
            captureState.bitmap != null -> readBitmap(captureState.bitmap!!, "صفحه")
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
                    "ترجمهٔ متن صفحه",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Right,
                )
                Text(
                    "متن روی صفحه را بخوان و آنلاین ترجمه کن",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textTertiary,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SourceButton(
                icon = Icons.Rounded.DocumentScanner,
                label = "ضبط صفحه",
                selected = false,
                modifier = Modifier.weight(1f),
                enabled = !captureState.running && !busy,
                onClick = { startScan() },
            )
            SourceButton(
                icon = Icons.Rounded.Image,
                label = "انتخاب تصویر",
                selected = false,
                modifier = Modifier.weight(1f),
                enabled = !captureState.running && !busy,
                onClick = {
                    runCatching {
                        imageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }.onFailure { status = it.message ?: "گالری باز نشد." }
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SourceButton(
                icon = Icons.Rounded.ContentPaste,
                label = "چسباندن متن",
                selected = false,
                modifier = Modifier.weight(1f),
                enabled = !busy,
                onClick = { showPaste = true },
            )
            SourceButton(
                icon = Icons.Rounded.BubbleChart,
                label = "حباب شناور",
                selected = false,
                modifier = Modifier.weight(1f),
                enabled = !busy,
                onClick = { startBubble() },
            )
        }

        if (captureState.running || busy || translating) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = palette.accent, strokeWidth = 2.dp, modifier = Modifier.width(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        captureState.running -> "در حال گرفتن تصویر صفحه…"
                        busy -> "در حال خواندن متن…"
                        else -> "در حال ترجمه…"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            }
        }

        status?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { showHelp = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Translate, contentDescription = null, tint = palette.textTertiary)
            Spacer(Modifier.width(6.dp))
            Text(
                "اگر ضبط صفحه کار نکرد، اینجا را ببین",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textTertiary,
            )
        }

        Spacer(Modifier.height(12.dp))
        Divider(color = palette.outline)
        Spacer(Modifier.height(12.dp))

        if (lines.isEmpty() && !translating) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Translate, contentDescription = null, tint = palette.textTertiary)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "متن شناسایی شده در اینجا نمایش داده می‌شود",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textTertiary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                SoftIconButton(
                    icon = Icons.Rounded.ContentCopy,
                    contentDescription = "کپی متن",
                    background = palette.accentSoft,
                    tint = palette.accent,
                    onClick = {
                        val text = lines.joinToString("\n") { "${it.english}\n${it.persian}" }
                        clipboard.setText(AnnotatedString(text))
                        vm.playSound(SoundManager.SAVE)
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = bottomInset + 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(lines, key = { index, _ -> index }) { _, line ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.surface)
                            .border(1.dp, palette.outline, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                    ) {
                        Column {
                            Text(
                                line.english,
                                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                                color = palette.textPrimary,
                            )
                            Spacer(Modifier.height(6.dp))
                            if (line.pending) {
                                Text("در حال ترجمه…", style = MaterialTheme.typography.bodySmall, color = palette.textTertiary)
                            } else if (line.persian.isNotBlank()) {
                                Text(line.persian, style = MaterialTheme.typography.bodySmall, color = palette.accent)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPaste) {
        AlertDialog(
            onDismissRequest = { showPaste = false },
            containerColor = palette.surface,
            title = { Text("متن را بچسبان", color = palette.textPrimary) },
            text = {
                OutlinedTextField(
                    value = pasteText,
                    onValueChange = { pasteText = it },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.outline,
                        focusedTextColor = palette.textPrimary,
                        unfocusedTextColor = palette.textPrimary,
                        cursorColor = palette.accent,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val chunks = pasteText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                    showPaste = false
                    if (chunks.isEmpty()) {
                        status = "متنی وارد نشده است."
                    } else {
                        lines = emptyList()
                        translateAll(chunks)
                    }
                }) { Text("ترجمه", color = palette.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showPaste = false }) { Text("بستن", color = palette.textSecondary) }
            },
        )
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            containerColor = palette.surface,
            title = { Text("راهنمای اسکن صفحه", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "۱) روی بیشتر گوشی‌های شیائومی (MIUI/HyperOS) ضبط صفحه فقط وقتی کار می‌کند که مجوز «نمایش پنجره‌های بازشو هنگام اجرا در پس‌زمینه» روشن باشد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                    Text(
                        "تنظیمات ← برنامه‌ها ← Espresso English ← سایر مجوزها ← نمایش پنجره‌های بازشو در پس‌زمینه را روشن کن.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textTertiary,
                    )
                    Text(
                        "۲) راه مطمئن و بدون مجوز: یک اسکرین‌شات بگیر، بعد دکمهٔ «انتخاب تصویر» را بزن و همان عکس را انتخاب کن.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                    Text(
                        "۳) اگر متن را خودت داری، دکمهٔ «چسباندن متن» سریع‌ترین راه است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                    Text(
                        "نکته: شناساگر متن خط لاتین (انگلیسی) را می‌خواند.",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textTertiary,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text("فهمیدم", color = palette.accent) }
            },
        )
    }
}

@Composable
private fun SourceButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val palette = espresso
    val tint = if (enabled) palette.accent else palette.textTertiary
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) palette.accentSoft else palette.surface)
            .border(1.dp, if (selected) palette.accent else palette.outline, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) palette.textPrimary else palette.textTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

/** Loads a picked image at a size ML Kit can chew through quickly. */
private fun decodeForOcr(context: Context, uri: Uri, maxDim: Int = 1800): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val widest = maxOf(bounds.outWidth, bounds.outHeight)
    var sample = 1
    while (widest / sample > maxDim) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}
