package ir.esmail.espresso.data

import kotlin.random.Random

enum class QuizKind { MEANING, REVERSE, CLOZE, TRANSLATE, PRONUNCIATION }

data class QuizQuestion(
    val id: Int,
    val kind: QuizKind,
    val prompt: String,
    val hint: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val speakable: String = "",
)

/**
 * Builds a short, deterministic test from the material of a single episode: the vocabulary
 * list, the hand written translations and the transcript itself. The same episode always
 * yields the same quiz, which makes "try again" meaningful.
 */
object QuizBuilder {

    private const val OPTION_COUNT = 4

    fun build(episode: Episode, glossary: Map<String, GlossaryEntry>, count: Int = 8): List<QuizQuestion> {
        val random = Random(episode.id.hashCode().toLong())
        val pool = meaningPool(episode, glossary, random)
        if (pool.size < OPTION_COUNT) return emptyList()

        val questions = ArrayList<QuizQuestion>(count)
        val usedTerms = HashSet<String>()

        // 1) Context questions: the word is missing from a sentence the learner just heard.
        val clozeCandidates = episode.vocab.filter { it.term.isNotBlank() }.shuffled(random)
        for (item in clozeCandidates) {
            if (questions.size >= count / 2) break
            val segment = findSegment(episode, item.term) ?: continue
            val blanked = blankOut(segment.text, item.term) ?: continue
            if (!usedTerms.add(item.term.lowercase())) continue
            val distractors = pool.filter { it.first.lowercase() != item.term.lowercase() }
                .shuffled(random).take(OPTION_COUNT - 1).map { it.first }
            if (distractors.size < OPTION_COUNT - 1) continue
            val options = (distractors + item.term).shuffled(random)
            questions += QuizQuestion(
                id = questions.size,
                kind = QuizKind.CLOZE,
                prompt = blanked,
                hint = "جای خالی را با واژهٔ درست پر کن",
                options = options,
                correctIndex = options.indexOf(item.term),
                explanation = if (segment.fa.isNotBlank()) segment.fa else item.fa,
                speakable = segment.text,
            )
        }

        // 2) English term → Persian meaning.
        val meaningTargets = pool.shuffled(random)
        for ((term, meaning) in meaningTargets) {
            if (questions.size >= count - 2) break
            if (!usedTerms.add(term.lowercase())) continue
            val distractors = pool.filter { it.second != meaning }
                .shuffled(random).take(OPTION_COUNT - 1).map { it.second }
            if (distractors.size < OPTION_COUNT - 1) continue
            val options = (distractors + meaning).shuffled(random)
            questions += QuizQuestion(
                id = questions.size,
                kind = QuizKind.MEANING,
                prompt = term,
                hint = "معنی این واژه کدام است؟",
                options = options,
                correctIndex = options.indexOf(meaning),
                explanation = "«$term» یعنی «$meaning»",
                speakable = term,
            )
        }

        // 3) Persian meaning → English term.
        for ((term, meaning) in meaningTargets.shuffled(random)) {
            if (questions.size >= count) break
            if (!usedTerms.add("rev:" + term.lowercase())) continue
            val distractors = pool.filter { it.first.lowercase() != term.lowercase() }
                .shuffled(random).take(OPTION_COUNT - 1).map { it.first }
            if (distractors.size < OPTION_COUNT - 1) continue
            val options = (distractors + term).shuffled(random)
            questions += QuizQuestion(
                id = questions.size,
                kind = QuizKind.REVERSE,
                prompt = meaning,
                hint = "کدام واژهٔ انگلیسی این معنی را دارد؟",
                options = options,
                correctIndex = options.indexOf(term),
                explanation = "«$meaning» یعنی «$term»",
                speakable = term,
            )
        }

        // 3b) IPA pronunciation → match the word.
        val ipaCandidates = episode.vocab.filter { it.ipa.isNotBlank() && it.term.isNotBlank() }.shuffled(random)
        for (item in ipaCandidates) {
            if (questions.size >= count) break
            if (!usedTerms.add("ipa:" + item.term.lowercase())) continue
            val distractors = ipaCandidates
                .filter { it.term.lowercase() != item.term.lowercase() }
                .shuffled(random).take(OPTION_COUNT - 1).map { it.term }
            if (distractors.size < OPTION_COUNT - 1) continue
            val options = (distractors + item.term).shuffled(random)
            questions += QuizQuestion(
                id = questions.size,
                kind = QuizKind.PRONUNCIATION,
                prompt = item.ipa,
                hint = "کدام واژه با این تلفظ خوانده میشود؟",
                options = options,
                correctIndex = options.indexOf(item.term),
                explanation = "«${item.term}» تلفظ میشود ${item.ipa} — یعنی «${item.fa}»",
                speakable = item.term,
            )
        }

        // 4) Persian translation → the English sentence from the episode transcript.
        val sentenceCandidates = episode.segments
            .filter { it.fa.isNotBlank() && it.text.length in 18..130 && it.text.contains(' ') }
            .shuffled(random)
        val usedSegments = HashSet<String>()
        for (segment in sentenceCandidates) {
            if (questions.size >= count) break
            if (!usedSegments.add(segment.id)) continue
            val distractors = sentenceCandidates
                .filter { it.id != segment.id && it.text != segment.text }
                .shuffled(random)
                .take(OPTION_COUNT - 1)
                .map { it.text }
            if (distractors.size < OPTION_COUNT - 1) continue
            val options = (distractors + segment.text).shuffled(random)
            questions += QuizQuestion(
                id = questions.size,
                kind = QuizKind.TRANSLATE,
                prompt = segment.fa,
                hint = "کدام جملهٔ انگلیسی معنی این جمله است؟",
                options = options,
                correctIndex = options.indexOf(segment.text),
                explanation = "«${segment.fa}» meant: ${segment.text}",
                speakable = segment.text,
            )
        }

        return questions
    }

    private fun meaningPool(
        episode: Episode,
        glossary: Map<String, GlossaryEntry>,
        random: Random,
    ): List<Pair<String, String>> {
        val seen = HashSet<String>()
        val pool = ArrayList<Pair<String, String>>()

        episode.vocab.forEach { item ->
            val meaning = item.fa.trim()
            if (meaning.isNotEmpty() && seen.add(item.term.lowercase())) {
                pool += item.term to meaning
            }
        }
        glossary.entries.shuffled(random).forEach { (term, entry) ->
            val meaning = entry.fa.trim()
            if (meaning.isNotEmpty() && term.length > 1 && seen.add(term.lowercase())) {
                pool += term to meaning
            }
        }
        return pool
    }

    private fun findSegment(episode: Episode, term: String): Segment? {
        val needle = term.lowercase()
        return episode.segments.firstOrNull { segment ->
            segment.text.lowercase().contains(needle) && segment.text.length > needle.length + 8
        }
    }

    private fun blankOut(text: String, term: String): String? {
        val index = text.lowercase().indexOf(term.lowercase())
        if (index < 0) return null
        val before = text.substring(0, index)
        val after = text.substring(index + term.length)
        return (before + "____" + after).trim()
    }
}
