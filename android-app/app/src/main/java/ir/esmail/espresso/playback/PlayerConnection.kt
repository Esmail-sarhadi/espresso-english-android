package ir.esmail.espresso.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import ir.esmail.espresso.data.Episode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class PlayerUiState(
    val connected: Boolean = false,
    val episodeId: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    /** True while the offline voice is generating this lesson's audio. */
    val preparing: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val ended: Boolean = false,
    val error: String? = null,
    val sleepTimerEndsAt: Long? = null,
)

/**
 * Process wide bridge to [PlaybackService]. The service owns the player, this class only holds a
 * [MediaController] so every screen (and the mini player) observes exactly one source of truth.
 */
class PlayerConnection(private val context: Context, private val ttsCache: TtsCache) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainExecutor = ContextCompat.getMainExecutor(context)

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val ready = CompletableDeferred<MediaController>()
    private var controller: MediaController? = null
    private var ticker: Job? = null
    private var sleepJob: Job? = null
    private var connecting = false

    private val artworkCache = HashMap<String, ByteArray?>()

    /** When the user disables auto play, reaching the end of a lesson must stop playback. */
    var autoPlayNext: Boolean = true

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) syncPosition() else syncFromController()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val buffering = playbackState == Player.STATE_BUFFERING
            val ended = playbackState == Player.STATE_ENDED
            _state.update { it.copy(isBuffering = buffering, ended = ended) }
            syncFromController()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (!autoPlayNext && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                controller?.pause()
            }
            syncFromController()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _state.update { it.copy(speed = playbackParameters.speed) }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(error = error.errorCodeName, isBuffering = false) }
        }
    }

    init {
        connect()
    }

    private fun connect() {
        if (controller != null || connecting) return
        connecting = true
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                val result = runCatching { future.get() }
                connecting = false
                result.onSuccess { mediaController ->
                    controller = mediaController
                    mediaController.addListener(listener)
                    _state.update { it.copy(connected = true) }
                    ready.complete(mediaController)
                    syncFromController()
                    startTicker()
                }.onFailure { error ->
                    _state.update { it.copy(connected = false, error = error.message) }
                    ready.completeExceptionally(error)
                }
            },
            mainExecutor,
        )
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                if (controller?.isPlaying == true) syncPosition()
                // 50 ms keeps word highlighting in step with speech; 200 ms made text visibly lag.
                delay(50)
            }
        }
    }

    private fun syncPosition() {
        val c = controller ?: return
        _state.update {
            it.copy(
                positionMs = c.currentPosition.coerceAtLeast(0L),
                durationMs = c.duration.coerceAtLeast(0L),
            )
        }
    }

    private fun syncFromController() {
        val c = controller ?: return
        val item = c.currentMediaItem
        _state.update {
            it.copy(
                connected = true,
                episodeId = item?.mediaId,
                isPlaying = c.isPlaying,
                isBuffering = c.playbackState == Player.STATE_BUFFERING,
                positionMs = c.currentPosition.coerceAtLeast(0L),
                durationMs = c.duration.coerceAtLeast(0L),
                speed = c.playbackParameters.speed,
                hasNext = c.hasNextMediaItem(),
                hasPrevious = c.hasPreviousMediaItem(),
                ended = c.playbackState == Player.STATE_ENDED,
            )
        }
    }

    /* ------------------------------------------------------------------------------------ */
    /* Commands                                                                             */
    /* ------------------------------------------------------------------------------------ */

    fun playQueue(episodes: List<Episode>, startIndex: Int, startMs: Long = 0L) {
        if (episodes.isEmpty()) return
        val index = startIndex.coerceIn(0, episodes.lastIndex)
        val episode = episodes[index]
        scope.launch {
            val mediaController = withTimeoutOrNull(8_000) { ready.await() } ?: return@launch

            // Already loaded this lesson? Just resume — no synthesis, no delay.
            if (currentEpisodeId == episode.id && mediaController.currentMediaItem?.mediaId == episode.id) {
                val target = startMs.coerceAtMost((mediaController.duration).coerceAtLeast(0L))
                mediaController.seekTo(target)
                mediaController.play()
                return@launch
            }

            // TTS synthesis can take seconds. Show progress BEFORE starting the heavy work so the
            // UI never looks frozen, and generate on IO (never on the main thread).
            _state.update {
                it.copy(
                    preparing = true,
                    isPlaying = false,
                    isBuffering = false,
                    error = null,
                    episodeId = episode.id,
                )
            }
            val item = runCatching { withContext(Dispatchers.IO) { episode.toMediaItem() } }
                .getOrElse { error ->
                    _state.update {
                        it.copy(
                            preparing = false,
                            isPlaying = false,
                            error = error.message ?: "صوت درس ساخته نشد.",
                        )
                    }
                    return@launch
                }

            mediaController.setMediaItems(listOf(item), 0, 0L)
            mediaController.prepare()
            _state.update { it.copy(preparing = false, isBuffering = true) }

            // startMs is in the AUTHORED (source) timeline; the generated WAV has its own length.
            // Scale it before seeking, otherwise the lesson starts at a random later sentence.
            val sourceDuration = episode.durationMs.coerceAtLeast(1L)
            val wavDuration = runCatching { mediaController.duration }.getOrDefault(0L)
            val target = if (wavDuration > 0L && startMs > 0L) {
                (startMs.toFloat() / sourceDuration * wavDuration).toLong()
                    .coerceIn(0L, (wavDuration - 1_000L).coerceAtLeast(0L))
            } else {
                0L
            }
            if (target > 0L) mediaController.seekTo(target)
            currentEpisodeId = episode.id
            mediaController.play()
        }
    }

    private var currentEpisodeId: String? = null

    fun toggle() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0)
            c.play()
        }
    }

    fun play() = controller?.play()

    fun pause() = controller?.pause()

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
        syncPosition()
    }

    fun seekBy(deltaMs: Long) {
        val c = controller ?: return
        val target = (c.currentPosition + deltaMs).coerceIn(0L, c.duration.coerceAtLeast(0L))
        c.seekTo(target)
        syncPosition()
    }

    fun next() = controller?.takeIf { it.hasNextMediaItem() }?.seekToNextMediaItem()

    fun previous() {
        val c = controller ?: return
        if (c.currentPosition > 3_000L) c.seekTo(0) else c.seekToPreviousMediaItem()
    }

    fun setSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
    }

    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _state.update { it.copy(sleepTimerEndsAt = null) }
            return
        }
        val endsAt = System.currentTimeMillis() + minutes * 60_000L
        _state.update { it.copy(sleepTimerEndsAt = endsAt) }
        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            controller?.pause()
            _state.update { it.copy(sleepTimerEndsAt = null) }
        }
    }

    /** Stops playback and removes the queue, which also hides the mini player. */
    fun stopAndClear() {
        val c = controller ?: return
        c.stop()
        c.clearMediaItems()
        _state.update {
            it.copy(episodeId = null, isPlaying = false, positionMs = 0L, durationMs = 0L, ended = false)
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    fun release() {
        ticker?.cancel()
        sleepJob?.cancel()
        controller?.release()
        controller = null
    }

    /* ------------------------------------------------------------------------------------ */

    private suspend fun Episode.toMediaItem(): MediaItem {
        val artwork = artworkCache.getOrPut(id) {
            runCatching { context.assets.open(coverAsset).readBytes() }.getOrNull()
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(titleFa.ifBlank { title })
            .setArtist("Espresso English · ${title}")
            .setAlbumTitle("Espresso English")
            .apply {
                artwork?.let { setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER) }
            }
            .build()

        val uri = if (ttsOnly) {
            ttsCache.audioFile(this).toURI().toString()
        } else {
            "asset:///${assetPath}"
        }
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }
}
