package ir.esmail.espresso.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Leitner style scheduling: 0 → 1 → 3 → 7 → 16 → 35 → 70 days. */
object Srs {

    val intervalsDays = intArrayOf(0, 1, 3, 7, 16, 35, 70)

    const val MASTERED_BOX = 5

    fun nextDue(box: Int, rating: Rating, now: Long): Pair<Int, Long> {
        val next = when (rating) {
            Rating.AGAIN -> 0
            Rating.GOOD -> (box + 1).coerceAtMost(intervalsDays.lastIndex)
            Rating.EASY -> (box + 2).coerceAtMost(intervalsDays.lastIndex)
        }
        val dueAt = when (next) {
            0 -> now + TimeUnit.MINUTES.toMillis(if (rating == Rating.AGAIN) 10 else 1)
            else -> now + TimeUnit.DAYS.toMillis(intervalsDays[next].toLong())
        }
        return next to dueAt
    }
}

enum class Rating { AGAIN, GOOD, EASY }

enum class ThemeMode { SYSTEM, DARK, LIGHT }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.DARK,
    val fontScale: Float = 1f,
    val speed: Float = 1f,
    val autoPlayNext: Boolean = true,
    val autoScroll: Boolean = true,
    val soundEffects: Boolean = true,
    val dailyGoalMinutes: Int = 15,
    val useNaturalVoice: Boolean = true,
) {
    companion object {
        const val KEY_THEME = "theme"
        const val KEY_FONT = "font_scale"
        const val KEY_SPEED = "speed"
        const val KEY_AUTOPLAY = "auto_play_next"
        const val KEY_AUTOSCROLL = "auto_scroll"
        const val KEY_SOUND = "sound_effects"
        const val KEY_DAILY_GOAL = "daily_goal_minutes"
        const val KEY_NATURAL_VOICE = "natural_voice"

        fun from(rows: List<SettingEntity>): AppSettings {
            val map = rows.associate { it.key to it.value }
            return AppSettings(
                theme = runCatching { ThemeMode.valueOf(map[KEY_THEME] ?: "DARK") }.getOrDefault(ThemeMode.DARK),
                fontScale = map[KEY_FONT]?.toFloatOrNull()?.coerceIn(0.85f, 1.35f) ?: 1f,
                speed = map[KEY_SPEED]?.toFloatOrNull()?.coerceIn(0.5f, 1.5f) ?: 1f,
                autoPlayNext = map[KEY_AUTOPLAY]?.let { it == "1" } ?: true,
                autoScroll = map[KEY_AUTOSCROLL]?.let { it == "1" } ?: true,
                soundEffects = map[KEY_SOUND]?.let { it == "1" } ?: true,
                dailyGoalMinutes = map[KEY_DAILY_GOAL]?.toIntOrNull()?.coerceIn(1, 240) ?: 15,
                useNaturalVoice = map[KEY_NATURAL_VOICE]?.let { it == "1" } ?: true,
            )
        }
    }
}

data class LearnerStats(
    val wordsSaved: Int = 0,
    val wordsMastered: Int = 0,
    val wordsDue: Int = 0,
    val minutesListened: Int = 0,
    val streakDays: Int = 0,
    val lessonsFinished: Int = 0,
    val quizzesTaken: Int = 0,
    val averageScore: Int = 0,
    val phrasesSaved: Int = 0,
    val phrasesMastered: Int = 0,
    val phrasesDue: Int = 0,
    val favorites: Int = 0,
    val speakingAttempts: Int = 0,
    val averageSpeakingScore: Int = 0,
    val todayMinutes: Int = 0,
    val goalMinutes: Int = 15,
)

object Dates {
    private val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val display = SimpleDateFormat("d MMMM", Locale("fa"))

    fun dayKey(millis: Long = System.currentTimeMillis()): String = iso.format(Date(millis))

    fun parseDay(key: String): Long? = runCatching { iso.parse(key)?.time }.getOrNull()

    fun prettyDay(key: String): String = parseDay(key)?.let { display.format(Date(it)) } ?: key

    fun isSameDay(a: Long, b: Long): Boolean = dayKey(a) == dayKey(b)

    /** Consecutive days with at least [thresholdMs] of listening, counting back from today. */
    fun streak(days: List<DailyListenEntity>, thresholdMs: Long = TimeUnit.MINUTES.toMillis(1)): Int {
        val active = days.filter { it.millis >= thresholdMs }.map { it.day }.toHashSet()
        if (active.isEmpty()) return 0

        val calendar = Calendar.getInstance()
        if (!active.contains(dayKey(calendar.timeInMillis))) {
            // Today may simply not have started yet – a streak is still alive if yesterday was active.
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            if (!active.contains(dayKey(calendar.timeInMillis))) return 0
        }
        var streak = 0
        while (active.contains(dayKey(calendar.timeInMillis))) {
            streak++
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }
}
