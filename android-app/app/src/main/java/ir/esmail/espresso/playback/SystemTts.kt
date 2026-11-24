package ir.esmail.espresso.playback

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale

/**
 * Uses the device's own speech engine (Google TTS on most phones). Its neural voices sound far
 * more natural than the small bundled Piper model and add ZERO bytes to the APK. Falls back
 * silently when the device has no English voice installed.
 */
class SystemTts(context: Context) {

    private val ready = CompletableDeferred<Boolean>()
    private val tts: TextToSpeech

    @Volatile
    private var available = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val language = tts.setLanguage(Locale.US)
                available = language == TextToSpeech.LANG_AVAILABLE ||
                    language == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                    language == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
            }
            ready.complete(available)
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                pending[utteranceId]?.complete(true)
            }
            override fun onError(utteranceId: String?) {
                pending[utteranceId]?.complete(false)
            }
        })
    }

    private val pending = java.util.concurrent.ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    suspend fun isAvailable(): Boolean = withTimeoutOrNull(3_000) { ready.await() } ?: false

    /** Synthesizes text to a WAV file. Returns false if the engine cannot do it. */
    suspend fun synthesize(text: String, file: File): Boolean = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext false
        val id = "espresso-${System.nanoTime()}"
        val done = CompletableDeferred<Boolean>()
        pending[id] = done
        val params = hashMapOf(
            TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to id,
        )
        @Suppress("DEPRECATION")
        val result = tts.synthesizeToFile(text, params, file.absolutePath)
        if (result != TextToSpeech.SUCCESS) {
            pending.remove(id)
            return@withContext false
        }
        val ok = withTimeoutOrNull(20_000) { done.await() } ?: false
        pending.remove(id)
        ok && file.exists() && file.length() > 1_024L
    }

    fun shutdown() {
        runCatching { tts.shutdown() }
    }
}
