package ir.esmail.espresso.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log

/**
 * Tiny offline UI sound board. Sounds are bundled as PCM WAV in assets/sfx and mixed through a
 * low-priority SoundPool stream so they never interrupt podcast audio or lock-screen playback.
 */
class SoundManager(context: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val sounds: Map<String, Int> = mapOf(
        CORRECT to load(context, CORRECT),
        WRONG to load(context, WRONG),
        TAP to load(context, TAP),
        SAVE to load(context, SAVE),
        COMPLETE to load(context, COMPLETE),
    )

    @Volatile
    var enabled: Boolean = true

    private fun load(context: Context, name: String): Int = runCatching {
        context.assets.openFd("sfx/$name.wav").use { pool.load(it, 1) }
    }.onFailure { Log.w(TAG, "missing sound: $name", it) }.getOrDefault(0)

    fun play(name: String, volume: Float = 0.8f) {
        if (!enabled) return
        val id = sounds[name] ?: return
        if (id != 0) runCatching { pool.play(id, volume, volume, 2, 0, 1f) }
    }

    fun release() = pool.release()

    companion object {
        private const val TAG = "SoundManager"
        const val CORRECT = "correct"
        const val WRONG = "wrong"
        const val TAP = "tap"
        const val SAVE = "save"
        const val COMPLETE = "complete"
    }
}
