package ir.esmail.espresso.data

import kotlin.math.max

data class WordScore(val word: String, val correct: Boolean)

data class PronunciationScore(
    val overall: Int,
    val words: List<WordScore>,
    val recognized: String,
) {
    val correctCount: Int get() = words.count { it.correct }
}

/**
 * Scores what the learner said against the target phrase. Words are normalised (case, punctuation,
 * common contractions) then aligned with a token-level Levenshtein so insertions and deletions do
 * not shift every later word. The result is a 0-100 score plus a per-word correctness map.
 */
object PronunciationScorer {

    private val punctuation = Regex("[^a-z0-9'\\s]")
    private val contractions = mapOf(
        "we're" to "we are", "they're" to "they are", "you're" to "you are",
        "it's" to "it is", "that's" to "that is", "i'm" to "i am",
        "don't" to "do not", "doesn't" to "does not", "didn't" to "did not",
        "can't" to "cannot", "won't" to "will not", "let's" to "let us",
        "i've" to "i have", "you've" to "you have", "there's" to "there is",
    )

    fun normalise(text: String): List<String> {
        var clean = text.lowercase().replace(punctuation, " ")
        contractions.forEach { (from, to) -> clean = clean.replace(from, to) }
        return clean.split(Regex("\\s+")).filter { it.isNotEmpty() }
    }

    fun score(target: String, recognized: String): PronunciationScore {
        val expected = normalise(target)
        val actual = normalise(recognized)
        if (expected.isEmpty()) return PronunciationScore(0, emptyList(), recognized)

        // Dynamic-programming alignment over tokens: match, substitute or skip.
        val n = expected.size
        val m = actual.size
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) dp[i][0] = i
        for (j in 0..m) dp[0][j] = j
        for (i in 1..n) {
            for (j in 1..m) {
                val cost = if (expected[i - 1] == actual[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }

        // Backtrace: mark which expected words were matched exactly.
        val correct = BooleanArray(n)
        var i = n
        var j = m
        while (i > 0 && j > 0) {
            val cost = if (expected[i - 1] == actual[j - 1]) 0 else 1
            if (dp[i][j] == dp[i - 1][j - 1] + cost) {
                if (cost == 0) correct[i - 1] = true
                i--
                j--
            } else if (dp[i][j] == dp[i - 1][j] + 1) {
                i--
            } else {
                j--
            }
        }
        // Near-matches (one edit away) count as partially understood but not exact.
        val words = expected.mapIndexed { index, word ->
            WordScore(word, correct[index] || actual.any { it == word })
        }
        val distance = dp[n][m]
        val accuracy = ((n - distance).toFloat() / max(n, m).toFloat()).coerceIn(0f, 1f)
        val overall = (accuracy * 100).toInt()
        return PronunciationScore(overall, words, recognized)
    }
}
