package ir.esmail.espresso.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------------------------------
 * Persistence. Everything the learner produces (position, saved words, quiz scores, streak)
 * lives in one SQLite database, written through the DAO below. All writes are idempotent so
 * a process kill at any moment can only lose the last few hundred milliseconds of progress.
 * ------------------------------------------------------------------------------------------ */

@Serializable
@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val episodeId: String,
    val positionMs: Long,
    val durationMs: Long,
    val completed: Boolean,
    val updatedAt: Long,
)

@Serializable
@Entity(
    tableName = "saved_words",
    indices = [Index(value = ["normalized"], unique = true)],
)
data class SavedWordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val term: String,
    val normalized: String,
    val meaning: String,
    val ipa: String = "",
    val partOfSpeech: String = "",
    val definition: String = "",
    val example: String = "",
    val episodeId: String? = null,
    val episodeTitle: String? = null,
    val createdAt: Long = 0L,
    val box: Int = 0,
    val dueAt: Long = 0L,
    val reviews: Int = 0,
    val lastReviewAt: Long = 0L,
    val mastered: Boolean = false,
)

@Serializable
@Entity(tableName = "quiz_results")
data class QuizResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val episodeId: String,
    val episodeTitle: String,
    val score: Int,
    val total: Int,
    val takenAt: Long,
)

@Serializable
@Entity(tableName = "daily_listen")
data class DailyListenEntity(
    @PrimaryKey val day: String,
    val millis: Long,
)

@Serializable
@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Dao
interface AppDao {

    // ---- progress -------------------------------------------------------------------------

    @Query("SELECT * FROM progress")
    fun progressFlow(): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE episodeId = :episodeId LIMIT 1")
    suspend fun progressOf(episodeId: String): ProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putProgress(progress: ProgressEntity)

    @Query("DELETE FROM progress")
    suspend fun clearProgress()

    // ---- saved words ----------------------------------------------------------------------

    @Query("SELECT * FROM saved_words ORDER BY createdAt DESC")
    fun wordsFlow(): Flow<List<SavedWordEntity>>

    @Query("SELECT * FROM saved_words WHERE normalized = :normalized LIMIT 1")
    suspend fun wordByKey(normalized: String): SavedWordEntity?

    @Query("SELECT * FROM saved_words WHERE id = :id LIMIT 1")
    suspend fun wordById(id: Long): SavedWordEntity?

    @Insert
    suspend fun insertWord(word: SavedWordEntity): Long

    @Update
    suspend fun updateWord(word: SavedWordEntity)

    @Delete
    suspend fun deleteWord(word: SavedWordEntity)

    @Query("SELECT * FROM saved_words WHERE mastered = 0 AND dueAt <= :now ORDER BY dueAt ASC LIMIT :limit")
    suspend fun dueWords(now: Long, limit: Int): List<SavedWordEntity>

    @Query("SELECT COUNT(*) FROM saved_words WHERE mastered = 0 AND dueAt <= :now")
    fun dueCountFlow(now: Long): Flow<Int>

    @Query("DELETE FROM saved_words")
    suspend fun clearWords()

    // ---- quizzes --------------------------------------------------------------------------

    @Query("SELECT * FROM quiz_results ORDER BY takenAt DESC")
    fun quizFlow(): Flow<List<QuizResultEntity>>

    @Insert
    suspend fun insertQuiz(result: QuizResultEntity)

    @Query("DELETE FROM quiz_results")
    suspend fun clearQuizzes()

    // ---- listening activity ---------------------------------------------------------------

    @Query("SELECT * FROM daily_listen ORDER BY day DESC LIMIT 400")
    fun dailyFlow(): Flow<List<DailyListenEntity>>

    @Query("SELECT * FROM daily_listen WHERE day = :day LIMIT 1")
    suspend fun dayOf(day: String): DailyListenEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putDay(entry: DailyListenEntity)

    @Query("DELETE FROM daily_listen")
    suspend fun clearDays()

    // ---- settings -------------------------------------------------------------------------

    @Query("SELECT * FROM settings")
    fun settingsFlow(): Flow<List<SettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSetting(setting: SettingEntity)

    @Query("DELETE FROM settings")
    suspend fun clearSettings()

    // ---- saved phrases --------------------------------------------------------------------

    @Query("SELECT * FROM saved_phrases ORDER BY createdAt DESC")
    fun phrasesFlow(): Flow<List<SavedPhraseEntity>>

    @Query("SELECT * FROM saved_phrases WHERE normalized = :normalized LIMIT 1")
    suspend fun phraseByKey(normalized: String): SavedPhraseEntity?

    @Query("SELECT * FROM saved_phrases WHERE id = :id LIMIT 1")
    suspend fun phraseById(id: Long): SavedPhraseEntity?

    @Insert
    suspend fun insertPhrase(phrase: SavedPhraseEntity): Long

    @Update
    suspend fun updatePhrase(phrase: SavedPhraseEntity)

    @androidx.room.Delete
    suspend fun deletePhrase(phrase: SavedPhraseEntity)

    @Query("SELECT * FROM saved_phrases WHERE mastered = 0 AND dueAt <= :now ORDER BY dueAt ASC LIMIT :limit")
    suspend fun duePhrases(now: Long, limit: Int): List<SavedPhraseEntity>

    @Query("DELETE FROM saved_phrases")
    suspend fun clearPhrases()

    // ---- favourites -----------------------------------------------------------------------

    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun favoritesFlow(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE episodeId = :episodeId LIMIT 1")
    suspend fun favoriteOf(episodeId: String): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE episodeId = :episodeId")
    suspend fun removeFavorite(episodeId: String)

    @Query("DELETE FROM favorites")
    suspend fun clearFavorites()

    // ---- pronunciation attempts -----------------------------------------------------------

    @Query("SELECT * FROM pronunciation_attempts ORDER BY createdAt DESC LIMIT 200")
    fun attemptsFlow(): Flow<List<PronunciationAttemptEntity>>

    @Insert
    suspend fun insertAttempt(attempt: PronunciationAttemptEntity)

    @Query("DELETE FROM pronunciation_attempts")
    suspend fun clearAttempts()

    // ---- aggregate export -----------------------------------------------------------------

    @Query("SELECT * FROM progress")
    suspend fun allProgress(): List<ProgressEntity>

    @Query("SELECT * FROM saved_words")
    suspend fun allWords(): List<SavedWordEntity>

    @Query("SELECT * FROM saved_phrases")
    suspend fun allPhrases(): List<SavedPhraseEntity>

    @Query("SELECT * FROM quiz_results")
    suspend fun allQuizzes(): List<QuizResultEntity>

    @Query("SELECT * FROM daily_listen")
    suspend fun allDays(): List<DailyListenEntity>

    @Query("SELECT * FROM favorites")
    suspend fun allFavorites(): List<FavoriteEntity>

    @Query("SELECT * FROM pronunciation_attempts")
    suspend fun allAttempts(): List<PronunciationAttemptEntity>

    @Query("SELECT * FROM settings")
    suspend fun allSettings(): List<SettingEntity>
}

@Database(
    entities = [
        ProgressEntity::class,
        SavedWordEntity::class,
        QuizResultEntity::class,
        DailyListenEntity::class,
        SettingEntity::class,
        SavedPhraseEntity::class,
        FavoriteEntity::class,
        PronunciationAttemptEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "espresso.db")
                // The schema is authored by this app only; destructive migration is a last resort
                // guard so a future version bump can never leave the app stuck on launch.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
