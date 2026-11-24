package ir.esmail.espresso.playback

import android.content.Context
import android.util.Log
import com.github.csukuangfj.piper.phonemize.piperPhonemizeInitialize
import com.github.csukuangfj.piper.phonemize.piperPhonemizeText
import com.k2fsa.sherpa.onnx.GeneratedAudio
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig

/**
 * On-device neural TTS. The Piper voice and the sherpa-onnx runtime are both bundled with the
 * APK, so synthesis never needs a network connection or pre-installed system voice data.
 *
 * Important: this model was trained with the espeak voice `en-us`. `OfflineTts.lang()` reports
 * the BCP-47 language `en_US`, which espeak-ng does not understand — passing it to the
 * phonemizer produced zero phonemes and therefore zero audio samples (the v3 bug). We always
 * phonemize with the model's espeak voice, never with [OfflineTts.lang].
 */
class BundledTts(private val context: Context) {

    private companion object {
        const val TAG = "BundledTts"
        const val MODEL = "tts/voice/en_US-lessac-medium.onnx"
        const val TOKENS = "tts/voice/tokens.txt"
        const val DATA_DIR = "tts/voice/espeak-ng-data"
        // The espeak voice this Piper model expects (from en_US-lessac-medium.onnx.json).
        const val PHONEMIZE_VOICE = "en-us"
        // Keep each synthesis call small: piper splits on punctuation, and very long blobs can
        // exhaust the native buffer. Segments are synthesized separately and joined by TtsCache.
        const val MAX_CHARS_PER_CHUNK = 320
        val WHITESPACE = Regex("\\s+")
    }

    private var tts: OfflineTts? = null
    private val lock = Any()

    val ready: Boolean get() = tts != null

    private fun ensure(): OfflineTts = synchronized(lock) {
        tts?.let { return@synchronized it }
        // The piper-phonemize AAR ships its own 17 MB copy of espeak-ng-data in assets/, which
        // would be merged into our APK. We already bundle the identical data under tts/voice/,
        // so pre-seed filesDir/espeak-ng-data from our copy: the library then skips its own
        // extraction path entirely and we can exclude the duplicate at packaging time.
        seedEspeakData()
        val initResult = piperPhonemizeInitialize(context.applicationContext)
        Log.i(TAG, "piper espeak init result=$initResult")
        val vits = OfflineTtsVitsModelConfig(
            model = MODEL,
            lexicon = "",
            tokens = TOKENS,
            dataDir = DATA_DIR,
            dictDir = "",
            noiseScale = 0.667f,
            noiseScaleW = 0.8f,
            lengthScale = 1f,
        )
        val model = OfflineTtsModelConfig(
            vits = vits,
            numThreads = 2,
            debug = false,
            provider = "cpu",
        )
        val config = OfflineTtsConfig(
            model = model,
            ruleFsts = "",
            ruleFars = "",
            maxNumSentences = 1,
            silenceScale = 0.2f,
        )
        OfflineTts(context.assets, config).also {
            tts = it
            Log.i(TAG, "offline voice ready: lang=${it.lang()} @ ${it.sampleRate()} Hz")
        }
    }

    /** Copies our bundled espeak data into the location piper-phonemize expects. */
    private fun seedEspeakData() {
        val target = java.io.File(context.applicationContext.filesDir, "espeak-ng-data")
        val marker = java.io.File(target, "en_dict")
        if (marker.exists() && marker.length() > 0L) return
        target.mkdirs()
        try {
            context.assets.list(DATA_DIR)?.forEach { name ->
                java.io.File(target, name).outputStream().use { out ->
                    context.assets.open("$DATA_DIR/$name").use { it.copyTo(out) }
                }
            }
            Log.i(TAG, "seeded espeak data (${target.list()?.size ?: 0} files)")
        } catch (error: Throwable) {
            Log.w(TAG, "espeak seed failed; library will use its own assets copy", error)
        }
    }

    val sampleRate: Int get() = ensure().sampleRate()

    /** Synthesizes one short piece of text into PCM samples. Called off the main thread. */
    fun synthesize(text: String, speed: Float = 1f): GeneratedAudio {
        val clean = text.trim().replace(WHITESPACE, " ")
        require(clean.isNotEmpty()) { "متنی برای ساخت صوت وجود ندارد." }

        // Fall back to the plain text API (sherpa-onnx phonemizes internally) if a chunk
        // somehow yields no phonemes — keeps audio available even if espeak data is damaged.
        val engine = ensure()
        val audio = synthesizeWithPhonemes(engine, clean, speed)
        if (audio != null && audio.samples.isNotEmpty()) return audio

        Log.w(TAG, "phoneme path produced no audio for: ${clean.take(80)}; trying plain generate")
        val plain = runCatching { engine.generate(clean, 0, speed.coerceIn(0.5f, 1.5f)) }
            .getOrNull()
        if (plain != null && plain.samples.isNotEmpty()) return plain

        error("صوت برای «${clean.take(40)}» ساخته نشد (هیچ نمونهای تولید نشد).")
    }

    private fun synthesizeWithPhonemes(
        engine: OfflineTts,
        text: String,
        speed: Float,
    ): GeneratedAudio? {
        val phonemes = piperPhonemizeText(text, PHONEMIZE_VOICE) ?: return null
        val codepoints = try {
            Array(phonemes.numSentences) { i -> phonemes.getPhonemes(i) }
        } finally {
            phonemes.close()
        }
        if (codepoints.isEmpty() || codepoints.all { it.isEmpty() }) {
            Log.w(TAG, "no phonemes for: ${text.take(80)}")
            return null
        }
        val config = GenerationConfig(
            speed = speed.coerceIn(0.5f, 1.5f),
            sid = 0,
            phonemeCodepoints = codepoints,
        )
        return runCatching { engine.generateWithConfig(text = "", config = config) }.getOrElse {
            Log.w(TAG, "generateWithConfig failed", it)
            null
        }
    }

}
