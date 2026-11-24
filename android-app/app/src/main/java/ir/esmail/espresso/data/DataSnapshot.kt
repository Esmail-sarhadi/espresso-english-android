package ir.esmail.espresso.data

import kotlinx.serialization.Serializable

/** Portable backup of everything the learner created. Shares cleanly as a JSON file. */
@Serializable
data class DataSnapshot(
    val exportedAt: Long,
    val appVersion: String,
    val progress: List<ProgressEntity>,
    val words: List<SavedWordEntity>,
    val phrases: List<SavedPhraseEntity>,
    val quizzes: List<QuizResultEntity>,
    val days: List<DailyListenEntity>,
    val favorites: List<FavoriteEntity>,
    val attempts: List<PronunciationAttemptEntity>,
    val settings: List<SettingEntity>,
)
