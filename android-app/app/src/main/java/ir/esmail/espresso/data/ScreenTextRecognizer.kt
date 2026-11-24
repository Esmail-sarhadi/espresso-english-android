package ir.esmail.espresso.data

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Reads printed text from a captured screen frame entirely on-device. Translation of the
 * extracted lines still happens online through [OnlineTranslator], as requested.
 */
class ScreenTextRecognizer {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun read(bitmap: Bitmap): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val image = InputImage.fromBitmap(bitmap, 0)
            suspendCancellableCoroutine<List<String>> { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { vision ->
                        val lines = vision.textBlocks
                            .flatMap { it.lines }
                            .map { it.text.trim() }
                            .filter { it.isNotEmpty() }
                        cont.resume(keepUsefulLines(lines))
                    }
                    .addOnFailureListener { error ->
                        if (cont.isActive) cont.resume(emptyList())
                        Unit
                    }
            }
        }
    }

    fun close() = recognizer.close()
}

private val latinLetter = Regex("[A-Za-z]")

/**
 * Drops the junk a screen OCR pass inevitably picks up: clock times, battery percentages, icon
 * glyphs and stray single letters. A line survives only if it looks like real Latin words,
 * which is what this feature translates.
 */
internal fun keepUsefulLines(raw: List<String>): List<String> {
    val seen = HashSet<String>()
    val kept = ArrayList<String>(raw.size)
    for (line in raw) {
        val text = line.trim()
        if (text.length < 3) continue
        val letters = latinLetter.findAll(text).count()
        if (letters < 3) continue
        if (letters.toFloat() / text.length < 0.45f) continue
        if (!seen.add(text.lowercase())) continue
        kept.add(text)
    }
    return kept
}
