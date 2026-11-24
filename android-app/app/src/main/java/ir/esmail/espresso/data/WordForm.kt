package ir.esmail.espresso.data

import java.util.Locale

/**
 * English word form helper. Learners tap whatever the transcript shows ("bottles", "looked"),
 * so lookups must survive inflection. We keep it deliberately conservative: no stemming
 * library, just the handful of suffixes that actually appear in the lessons.
 */
object WordForm {

    private val PUNCTUATION = charArrayOf(
        '.', ',', '!', '?', ';', ':', '"', '\'', '(', ')', '[', ']', '…', '—', '–', '“', '”', '‘', '’', '/', '\\',
    )

    private val SUFFIXES = listOf("s", "es", "ed", "d", "ing", "ly", "er", "est", "'s", "n't")

    fun normalize(raw: String): String {
        var value = raw.lowercase(Locale.US).trim()
        value = value.trim { it in PUNCTUATION || it.isWhitespace() }
        value = value.replace('’', '\'').replace('‘', '\'')
        value = value.trim { it in PUNCTUATION }
        return value
    }

    /** Ordered candidate keys, most specific first. */
    fun candidates(raw: String): List<String> {
        val base = normalize(raw)
        if (base.isEmpty()) return emptyList()

        val out = LinkedHashSet<String>(6)
        out += base

        // "don't" -> "do", "it's" -> "it"
        if (base.endsWith("n't")) out += base.dropLast(3)
        if (base.endsWith("'s")) out += base.dropLast(2)

        for (suffix in SUFFIXES) {
            if (base.length > suffix.length + 2 && base.endsWith(suffix)) {
                val stem = base.dropLast(suffix.length)
                out += stem
                // "studies" -> "study", "carried" -> "carry"
                if (stem.endsWith("i")) out += stem.dropLast(1) + "y"
                // "stopped" -> "stop", "running" -> "run"
                if (stem.length > 2 && stem.last() == stem[stem.length - 2]) out += stem.dropLast(1)
                // "making" -> "make"
                if (suffix == "ing" || suffix == "ed") out += stem + "e"
            }
        }
        return out.filter { it.isNotEmpty() }
    }

    /** True when the token carries no letters at all (punctuation only). */
    fun isPunctuation(raw: String): Boolean = raw.none { it.isLetterOrDigit() }
}
