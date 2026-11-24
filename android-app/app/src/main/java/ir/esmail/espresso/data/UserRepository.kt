package ir.esmail.espresso.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Single writer for everything the learner creates. The UI never touches the DAO directly so
 * every rule (spaced repetition, streak bookkeeping, de-duplication of saved words) lives here.
 */
class UserRepository(private val dao: AppDao) {

    val progress: Flow<Map<String, ProgressEntity>> =
        dao.progressFlow().map { list -> list.associateBy { it.episodeId } }

    val words: Flow<List<SavedWordEntity>> = dao.wordsFlow()

    val quizzes: Flow<List<QuizResultEntity>> = dao.quizFlow()

    val daily: Flow<List<DailyListenEntity>> = dao.dailyFlow()

    val settings: Flow<AppSettings> = dao.settingsFlow().map { AppSettings.from(it) }

    val phrases: Flow<List<SavedPhraseEntity>> = dao.phrasesFlow()

    val favorites: Flow<List<FavoriteEntity>> = dao.favoritesFlow()

    val attempts: Flow<List<PronunciationAttemptEntity>> = dao.attemptsFlow()

    val stats: Flow<LearnerStats> = combine(
        words, phrases, favorites, attempts, daily, quizzes, progress, settings,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val words = values[0] as List<SavedWordEntity>
        @Suppress("UNCHECKED_CAST")
        val phrases = values[1] as List<SavedPhraseEntity>
        @Suppress("UNCHECKED_CAST")
        val favorites = values[2] as List<FavoriteEntity>
        @Suppress("UNCHECKED_CAST")
        val attempts = values[3] as List<PronunciationAttemptEntity>
        @Suppress("UNCHECKED_CAST")
        val daily = values[4] as List<DailyListenEntity>
        @Suppress("UNCHECKED_CAST")
        val quizzes = values[5] as List<QuizResultEntity>
        @Suppress("UNCHECKED_CAST")
        val progress = values[6] as Map<String, ProgressEntity>
        @Suppress("UNCHECKED_CAST")
        val settings = values[7] as AppSettings

        val now = System.currentTimeMillis()
        val finished = progress.values.count { it.completed }
        val average = if (quizzes.isEmpty()) {
            0
        } else {
            quizzes.sumOf { if (it.total == 0) 0 else (it.score * 100) / it.total } / quizzes.size
        }
        val today = Dates.dayKey(now)
        val minutesToday = (daily.firstOrNull { it.day == today }?.millis ?: 0L) / 60_000L
        LearnerStats(
            wordsSaved = words.size,
            wordsMastered = words.count { it.mastered },
            wordsDue = words.count { !it.mastered && it.dueAt <= now },
            minutesListened = (daily.sumOf { it.millis } / 60_000L).toInt(),
            streakDays = Dates.streak(daily),
            lessonsFinished = finished,
            quizzesTaken = quizzes.size,
            averageScore = average,
            phrasesSaved = phrases.size,
            phrasesMastered = phrases.count { it.mastered },
            phrasesDue = phrases.count { !it.mastered && it.dueAt <= now },
            favorites = favorites.size,
            speakingAttempts = attempts.size,
            averageSpeakingScore = if (attempts.isEmpty()) 0 else attempts.sumOf { it.score } / attempts.size,
            todayMinutes = minutesToday.toInt(),
            goalMinutes = settings.dailyGoalMinutes,
        )
    }

    suspend fun progressOf(episodeId: String): ProgressEntity? = dao.progressOf(episodeId)

    suspend fun saveProgress(episodeId: String, positionMs: Long, durationMs: Long, listenedDeltaMs: Long) {
        if (durationMs <= 0L) return
        val now = System.currentTimeMillis()
        val position = positionMs.coerceIn(0L, durationMs)
        val previous = dao.progressOf(episodeId)
        // Completion is STICKY: replaying a finished lesson from the start must never erase it.
        // Resume position is also kept at the furthest point reached, so replaying does not
        // reset the learner back to zero.
        val reachedEnd = position >= durationMs - 2_000L
        val completed = (previous?.completed == true) || reachedEnd
        val storedPosition = if (completed) durationMs else maxOf(position, previous?.positionMs ?: 0L)
        dao.putProgress(
            ProgressEntity(
                episodeId = episodeId,
                positionMs = storedPosition,
                durationMs = durationMs,
                completed = completed,
                updatedAt = now,
            ),
        )
        // Accumulate every positive delta (the player reports ~50 ms ticks). A threshold here
        // used to discard all of them, which is why daily minutes and the streak stayed at 0.
        if (listenedDeltaMs > 0L) {
            val day = Dates.dayKey(now)
            val existing = dao.dayOf(day)
            dao.putDay(DailyListenEntity(day, (existing?.millis ?: 0L) + listenedDeltaMs))
        }
    }

    /** @return true when the word is now in the notebook. */
    suspend fun toggleWord(meaning: WordMeaning, episode: Episode?): Boolean {
        val key = WordForm.normalize(meaning.term)
        if (key.isEmpty()) return false
        val existing = dao.wordByKey(key)
        if (existing != null) {
            dao.deleteWord(existing)
            return false
        }
        val now = System.currentTimeMillis()
        dao.insertWord(
            SavedWordEntity(
                term = meaning.term,
                normalized = key,
                meaning = meaning.fa,
                ipa = meaning.ipa,
                partOfSpeech = meaning.pos,
                definition = meaning.en,
                example = meaning.example,
                episodeId = episode?.id,
                episodeTitle = episode?.titleFa?.ifBlank { episode.title },
                createdAt = now,
                box = 0,
                dueAt = now,
            ),
        )
        return true
    }

    suspend fun isSaved(term: String): Boolean = dao.wordByKey(WordForm.normalize(term)) != null

    suspend fun deleteWord(word: SavedWordEntity) = dao.deleteWord(word)

    suspend fun rate(word: SavedWordEntity, rating: Rating) {
        val now = System.currentTimeMillis()
        val (box, dueAt) = Srs.nextDue(word.box, rating, now)
        val reviews = word.reviews + 1
        dao.updateWord(
            word.copy(
                box = box,
                dueAt = dueAt,
                reviews = reviews,
                lastReviewAt = now,
                mastered = box >= Srs.MASTERED_BOX && reviews >= 3,
            ),
        )
    }

    suspend fun dueWords(limit: Int = 40): List<SavedWordEntity> =
        dao.dueWords(System.currentTimeMillis(), limit)

    suspend fun recordQuiz(episode: Episode, score: Int, total: Int) {
        dao.insertQuiz(
            QuizResultEntity(
                episodeId = episode.id,
                episodeTitle = episode.titleFa.ifBlank { episode.title },
                score = score,
                total = total,
                takenAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun setTheme(mode: ThemeMode) = dao.putSetting(SettingEntity(AppSettings.KEY_THEME, mode.name))

    suspend fun setFontScale(scale: Float) =
        dao.putSetting(SettingEntity(AppSettings.KEY_FONT, scale.toString()))

    suspend fun setSpeed(speed: Float) =
        dao.putSetting(SettingEntity(AppSettings.KEY_SPEED, speed.toString()))

    suspend fun setAutoPlayNext(enabled: Boolean) =
        dao.putSetting(SettingEntity(AppSettings.KEY_AUTOPLAY, if (enabled) "1" else "0"))

    suspend fun setAutoScroll(enabled: Boolean) =
        dao.putSetting(SettingEntity(AppSettings.KEY_AUTOSCROLL, if (enabled) "1" else "0"))

    suspend fun setSoundEffects(enabled: Boolean) =
        dao.putSetting(SettingEntity(AppSettings.KEY_SOUND, if (enabled) "1" else "0"))

    // ---- phrases --------------------------------------------------------------------------

    /** @return true when the phrase is now saved. */
    suspend fun togglePhrase(phrase: PhraseItem, episode: Episode?): Boolean {
        val key = WordForm.normalize(phrase.en)
        if (key.isEmpty()) return false
        val existing = dao.phraseByKey(key)
        if (existing != null) {
            dao.deletePhrase(existing)
            return false
        }
        dao.insertPhrase(
            SavedPhraseEntity(
                en = phrase.en,
                normalized = key,
                fa = phrase.fa,
                note = phrase.note,
                episodeId = episode?.id,
                episodeTitle = episode?.titleFa?.ifBlank { episode.title },
                createdAt = System.currentTimeMillis(),
                dueAt = System.currentTimeMillis(),
            ),
        )
        return true
    }

    suspend fun isPhraseSaved(en: String): Boolean = dao.phraseByKey(WordForm.normalize(en)) != null

    suspend fun deletePhrase(phrase: SavedPhraseEntity) = dao.deletePhrase(phrase)

    suspend fun ratePhrase(phrase: SavedPhraseEntity, rating: Rating) {
        val now = System.currentTimeMillis()
        val (box, dueAt) = Srs.nextDue(phrase.box, rating, now)
        dao.updatePhrase(
            phrase.copy(
                box = box,
                dueAt = dueAt,
                reviews = phrase.reviews + 1,
                lastReviewAt = now,
                mastered = box >= Srs.MASTERED_BOX && phrase.reviews + 1 >= 3,
            ),
        )
    }

    suspend fun duePhrases(limit: Int = 40): List<SavedPhraseEntity> =
        dao.duePhrases(System.currentTimeMillis(), limit)

    // ---- favourites -----------------------------------------------------------------------

    /** @return true when the lesson is now a favourite. */
    suspend fun toggleFavorite(episodeId: String): Boolean {
        if (dao.favoriteOf(episodeId) != null) {
            dao.removeFavorite(episodeId)
            return false
        }
        dao.putFavorite(FavoriteEntity(episodeId, System.currentTimeMillis()))
        return true
    }

    suspend fun isFavorite(episodeId: String): Boolean = dao.favoriteOf(episodeId) != null

    // ---- speaking -------------------------------------------------------------------------

    suspend fun recordAttempt(target: String, recognized: String, score: Int) {
        dao.insertAttempt(
            PronunciationAttemptEntity(
                target = target,
                recognized = recognized,
                score = score.coerceIn(0, 100),
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun setDailyGoal(minutes: Int) =
        dao.putSetting(SettingEntity(AppSettings.KEY_DAILY_GOAL, minutes.coerceIn(1, 240).toString()))

    suspend fun setNaturalVoice(enabled: Boolean) =
        dao.putSetting(SettingEntity(AppSettings.KEY_NATURAL_VOICE, if (enabled) "1" else "0"))

    /** Snapshot of every learner-created record, serialised for the export/share feature. */
    suspend fun exportSnapshot(): String {
        val snapshot = DataSnapshot(
            exportedAt = System.currentTimeMillis(),
            appVersion = "4.0.0",
            progress = dao.allProgress(),
            words = dao.allWords(),
            phrases = dao.allPhrases(),
            quizzes = dao.allQuizzes(),
            days = dao.allDays(),
            favorites = dao.allFavorites(),
            attempts = dao.allAttempts(),
            settings = dao.allSettings(),
        )
        return kotlinx.serialization.json.Json { prettyPrint = true }
            .encodeToString(DataSnapshot.serializer(), snapshot)
    }

    suspend fun clearLearningData() {
        dao.clearWords()
        dao.clearPhrases()
        dao.clearQuizzes()
        dao.clearProgress()
        dao.clearDays()
        dao.clearFavorites()
        dao.clearAttempts()
    }
}
