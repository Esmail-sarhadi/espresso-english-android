package ir.esmail.espresso.data

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Metadata of the song currently playing in another app (Spotify, YouTube Music, ...). */
data class NowPlaying(
    val title: String,
    val artist: String,
    val album: String = "",
    val durationSeconds: Int = 0,
) {
    val query: String get() = listOf(trackOnly(), artist).filter { it.isNotBlank() }.joinToString(" ")
    private fun trackOnly(): String = title
}

/**
 * Live playback position of the song playing right now. Used to scroll the lyrics in step with
 * the real audio, so the app never has to own the music itself.
 */
data class PlaybackSnapshot(
    val title: String,
    val artist: String,
    val positionMs: Long,
    val isPlaying: Boolean,
    val speed: Float = 1f,
)

/**
 * Detects the user's current song by reading active media sessions. This needs notification
 * listener access (granted once by the user in Android Settings) but no paid recognition API.
 */
class NowPlayingDetector(private val context: Context) {

    private val manager: MediaSessionManager? by lazy {
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    }

    private val listenerComponent: ComponentName
        get() = ComponentName(context, EspressoNotificationListener::class.java)

    val hasNotificationPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE,
        ) == PackageManager.PERMISSION_GRANTED || isListenerEnabled()

    private fun isListenerEnabled(): Boolean {
        val flat = runCatching {
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        }.getOrNull().orEmpty()
        val cn = listenerComponent.flattenToString()
        val short = "${listenerComponent.packageName}/${listenerComponent.className}"
        return flat.contains(cn, ignoreCase = true) || flat.contains(short, ignoreCase = true)
    }

    fun settingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    fun current(): NowPlaying? {
        val controller = playingController() ?: return null
        val metadata = controller.metadata ?: return null
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
            ?: return null
        val durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        return NowPlaying(
            title = title,
            artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty(),
            album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
            durationSeconds = (durationMs / 1000L).toInt(),
        )
    }

    /**
     * Current position of the active session. [PlaybackState.getPosition] is only the value at
     * [PlaybackState.getLastPositionUpdateTime], so the elapsed time is added back for a live read.
     */
    fun snapshot(): PlaybackSnapshot? {
        val controller = playingController() ?: return null
        val state = controller.playbackState ?: return null
        val metadata = controller.metadata ?: return null
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
            ?: return null

        val playing = state.state == PlaybackState.STATE_PLAYING
        val speed = if (state.playbackSpeed > 0f) state.playbackSpeed else 1f
        val elapsed = (SystemClock.elapsedRealtime() - state.lastPositionUpdateTime).coerceAtLeast(0L)
        val position = if (playing) state.position + (elapsed * speed).toLong() else state.position

        return PlaybackSnapshot(
            title = title,
            artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty(),
            positionMs = position.coerceAtLeast(0L),
            isPlaying = playing,
            speed = speed,
        )
    }

    private fun playingController(): MediaController? {
        if (!isListenerEnabled()) return null
        val sessions = runCatching { manager?.getActiveSessions(listenerComponent) }.getOrNull() ?: return null
        for (controller in sessions) {
            val state = controller.playbackState?.state ?: continue
            if (state != PlaybackState.STATE_PLAYING &&
                state != PlaybackState.STATE_BUFFERING &&
                state != PlaybackState.STATE_PAUSED
            ) {
                continue
            }
            if (controller.metadata == null) continue
            return controller
        }
        return null
    }
}
