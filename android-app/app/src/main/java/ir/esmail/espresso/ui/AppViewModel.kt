package ir.esmail.espresso.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ir.esmail.espresso.AppContainer
import ir.esmail.espresso.data.AppSettings
import ir.esmail.espresso.data.ContentBundle
import ir.esmail.espresso.data.DailyListenEntity
import ir.esmail.espresso.data.Episode
import ir.esmail.espresso.data.FavoriteEntity
import ir.esmail.espresso.data.LearnerStats
import ir.esmail.espresso.data.PhraseItem
import ir.esmail.espresso.data.PronunciationAttemptEntity
import ir.esmail.espresso.data.Rating
import ir.esmail.espresso.data.SavedPhraseEntity
import ir.esmail.espresso.data.ProgressEntity
import ir.esmail.espresso.data.QuizResultEntity
import ir.esmail.espresso.data.SavedWordEntity
import ir.esmail.espresso.data.ThemeMode
import ir.esmail.espresso.data.WordMeaning
import ir.esmail.espresso.playback.PlayerConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ContentState {
    data object Loading : ContentState
    data class Ready(val bundle: ContentBundle) : ContentState
    data class Failed(val message: String) : ContentState
}

class AppViewModel(private val container: AppContainer) : ViewModel() {

    private val users = container.users

    val player: PlayerConnection = container.player

    val sounds = container.sounds

    val translator = container.translator

    private val _content = MutableStateFlow<ContentState>(ContentState.Loading)
    val content: StateFlow<ContentState> = _content.asStateFlow()

    val progress: StateFlow<Map<String, ProgressEntity>> = users.progress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val words: StateFlow<List<SavedWordEntity>> = users.words
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val quizzes: StateFlow<List<QuizResultEntity>> = users.quizzes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val daily: StateFlow<List<DailyListenEntity>> = users.daily
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<LearnerStats> = users.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LearnerStats())

    val settings: StateFlow<AppSettings> = users.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val phrases: StateFlow<List<SavedPhraseEntity>> = users.phrases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites: StateFlow<List<FavoriteEntity>> = users.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val attempts: StateFlow<List<PronunciationAttemptEntity>> = users.attempts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val search = container.search
    val lyrics = container.lyrics
    val screenText = container.screenText
    val nowPlaying = container.nowPlaying

    init {
        loadContent()
        viewModelScope.launch {
            users.settings.collect {
                player.autoPlayNext = it.autoPlayNext
                sounds.enabled = it.soundEffects
                container.useNaturalVoice = it.useNaturalVoice
            }
        }
        viewModelScope.launch { persistPlayback() }
    }

    /**
     * Mirrors the player position into the database while the app is alive: every 5 seconds
     * during playback and once more whenever playback pauses or the track changes. Writing on a
     * threshold (instead of on every 200 ms tick) keeps the database quiet without losing
     * more than a few seconds of progress if the process is killed.
     */
    private suspend fun persistPlayback() {
        var lastSavedFraction = -1f
        var lastPosition = 0L
        var pendingDelta = 0L
        var activeEpisode: String? = null
        player.state.collect { state ->
            val episodeId = state.episodeId
            if (episodeId == null || state.durationMs <= 0L) {
                pendingDelta = 0L
                return@collect
            }
            if (activeEpisode != episodeId) {
                activeEpisode = episodeId
                lastSavedFraction = -1f
                lastPosition = state.positionMs
                pendingDelta = 0L
            }
            // Persist a FRACTION of playback progress. TTS WAVs and the authored transcript have
            // different durations, so storing an absolute millisecond value resumes at the wrong
            // sentence. A fraction works for both timelines.
            val fraction = (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
            // Sum the small 50 ms ticks between saves. Comparing only consecutive ticks produced
            // ~50 ms deltas which were discarded — the reason "0 minutes" and an empty streak.
            val rawDelta = state.positionMs - lastPosition
            val tickDelta = rawDelta.coerceIn(0L, 500L)
            if (state.isPlaying) pendingDelta += tickDelta
            lastPosition = state.positionMs

            val moved = kotlin.math.abs(fraction - lastSavedFraction) >= 0.02f
            val settled = !state.isPlaying && kotlin.math.abs(fraction - lastSavedFraction) > 0.001f
            val reachedEnd = fraction >= 0.985f
            if (moved || settled || reachedEnd) {
                val episode = bundle()?.episode(episodeId)
                val sourceDuration = episode?.durationMs ?: state.durationMs
                val sourcePosition = (fraction * sourceDuration).toLong()
                users.saveProgress(episodeId, sourcePosition, sourceDuration, pendingDelta)
                pendingDelta = 0L
                lastSavedFraction = fraction
            }
        }
    }

    suspend fun dueWords(limit: Int = 30): List<SavedWordEntity> = users.dueWords(limit)

    fun loadContent() {
        if (_content.value is ContentState.Ready) return
        _content.value = ContentState.Loading
        viewModelScope.launch {
            runCatching { container.content.load() }
                .onSuccess { bundle ->
                    _content.value = if (bundle.episodes.isEmpty()) {
                        ContentState.Failed("محتوای درس‌ها پیدا نشد.")
                    } else {
                        ContentState.Ready(bundle)
                    }
                }
                .onFailure { error ->
                    _content.value = ContentState.Failed(error.message ?: "خطای ناشناخته")
                }
        }
    }

    fun bundle(): ContentBundle? = (_content.value as? ContentState.Ready)?.bundle

    fun episode(id: String?): Episode? = bundle()?.episode(id)

    fun saveProgress(episodeId: String, positionMs: Long, durationMs: Long, deltaMs: Long) {
        viewModelScope.launch { users.saveProgress(episodeId, positionMs, durationMs, deltaMs) }
    }

    fun toggleWord(meaning: WordMeaning, episode: Episode?, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch { onResult(users.toggleWord(meaning, episode)) }
    }

    fun isWordSaved(term: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(runCatching { users.isSaved(term) }.getOrDefault(false)) }
    }

    fun deleteWord(word: SavedWordEntity) {
        viewModelScope.launch { users.deleteWord(word) }
    }

    fun rateWord(word: SavedWordEntity, rating: Rating) {
        viewModelScope.launch { users.rate(word, rating) }
    }

    fun recordQuiz(episode: Episode, score: Int, total: Int) {
        viewModelScope.launch { users.recordQuiz(episode, score, total) }
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { users.setTheme(mode) }

    fun setFontScale(scale: Float) = viewModelScope.launch { users.setFontScale(scale) }

    fun setSpeed(speed: Float) {
        player.setSpeed(speed)
        viewModelScope.launch { users.setSpeed(speed) }
    }

    fun setAutoPlayNext(enabled: Boolean) = viewModelScope.launch { users.setAutoPlayNext(enabled) }

    fun setAutoScroll(enabled: Boolean) = viewModelScope.launch { users.setAutoScroll(enabled) }

    fun setSoundEffects(enabled: Boolean) {
        sounds.enabled = enabled
        sounds.play(ir.esmail.espresso.playback.SoundManager.TAP)
        viewModelScope.launch { users.setSoundEffects(enabled) }
    }

    fun playSound(name: String) = sounds.play(name)

    // ---- phrases --------------------------------------------------------------------------

    fun togglePhrase(phrase: PhraseItem, episode: Episode?, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch { onResult(users.togglePhrase(phrase, episode)) }
    }

    fun isPhraseSaved(en: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(runCatching { users.isPhraseSaved(en) }.getOrDefault(false)) }
    }

    fun deletePhrase(phrase: SavedPhraseEntity) {
        viewModelScope.launch { users.deletePhrase(phrase) }
    }

    fun ratePhrase(phrase: SavedPhraseEntity, rating: Rating) {
        viewModelScope.launch { users.ratePhrase(phrase, rating) }
    }

    suspend fun duePhrases(limit: Int = 40): List<SavedPhraseEntity> = users.duePhrases(limit)

    // ---- favourites -----------------------------------------------------------------------

    fun toggleFavorite(episodeId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch { onResult(users.toggleFavorite(episodeId)) }
    }

    suspend fun isFavorite(episodeId: String): Boolean = runCatching { users.isFavorite(episodeId) }.getOrDefault(false)

    // ---- speaking / goals / export --------------------------------------------------------

    fun recordAttempt(target: String, recognized: String, score: Int) {
        viewModelScope.launch { users.recordAttempt(target, recognized, score) }
    }

    fun setDailyGoal(minutes: Int) = viewModelScope.launch { users.setDailyGoal(minutes) }

    fun setNaturalVoice(enabled: Boolean) {
        viewModelScope.launch { users.setNaturalVoice(enabled) }
    }

    fun exportData(onResult: (Result<String>) -> Unit) {
        viewModelScope.launch { onResult(runCatching { users.exportSnapshot() }) }
    }

    fun clearLearningData(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            users.clearLearningData()
            onDone()
        }
    }
}

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(container) as T
}
