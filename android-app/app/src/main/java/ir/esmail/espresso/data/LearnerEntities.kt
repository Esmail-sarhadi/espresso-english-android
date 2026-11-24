package ir.esmail.espresso.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** A phrase the learner saved from a lesson, scheduled with the same Leitner boxes as words. */
@Serializable
@Entity(
    tableName = "saved_phrases",
    indices = [Index(value = ["normalized"], unique = true)],
)
data class SavedPhraseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val en: String,
    val normalized: String,
    val fa: String = "",
    val note: String = "",
    val episodeId: String? = null,
    val episodeTitle: String? = null,
    val createdAt: Long = 0L,
    val box: Int = 0,
    val dueAt: Long = 0L,
    val reviews: Int = 0,
    val lastReviewAt: Long = 0L,
    val mastered: Boolean = false,
)

/** Lessons the learner marked as favourites. */
@Serializable
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val episodeId: String,
    val createdAt: Long = 0L,
)

/** One speech-practice result, kept so the learner can see improvement over time. */
@Serializable
@Entity(tableName = "pronunciation_attempts")
data class PronunciationAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val target: String,
    val recognized: String,
    val score: Int,
    val createdAt: Long = 0L,
)
