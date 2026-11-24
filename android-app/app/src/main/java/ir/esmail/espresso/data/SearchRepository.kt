package ir.esmail.espresso.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SearchHit(
    val key: String,
    val kind: Kind,
    val english: String,
    val persian: String,
    val detail: String,
    val episodeId: String,
    val episodeTitle: String,
    val segmentId: String,
    val level: String,
) {
    enum class Kind { WORD, PHRASE }
}

/**
 * Builds an in-memory index over every vocabulary item and phrase in the bundled content so the
 * learner can find any word across all lessons and jump straight to the exact transcript line.
 */
class SearchRepository(private val content: ContentRepository) {

    private data class Indexed(
        val hit: SearchHit,
        val haystack: String,
        val terms: List<String>,
    )

    @Volatile
    private var index: List<Indexed>? = null

    suspend fun ensureIndex() = withContext(Dispatchers.IO) {
        if (index != null) return@withContext
        val bundle = content.load()
        val built = ArrayList<Indexed>(bundle.episodes.size * 12)
        bundle.episodes.forEach { episode ->
            val title = episode.titleFa.ifBlank { episode.title }
            val segmentOf = HashMap<String, String>()
            episode.segments.forEach { segment ->
                WordForm.candidates(segment.text).forEach { candidate ->
                    segmentOf.putIfAbsent(candidate, segment.id)
                }
            }
            episode.vocab.forEach { item ->
                val segmentId = WordForm.candidates(item.term)
                    .firstNotNullOfOrNull { segmentOf[it] } ?: ""
                val detail = listOfNotNull(
                    item.ipa.takeIf { it.isNotBlank() },
                    item.pos.takeIf { it.isNotBlank() }?.let { WordForm.candidates(it).firstOrNull() ?: it },
                    item.en.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                val hit = SearchHit(
                    key = "w:${episode.id}:${WordForm.normalize(item.term)}",
                    kind = SearchHit.Kind.WORD,
                    english = item.term,
                    persian = item.fa,
                    detail = detail,
                    episodeId = episode.id,
                    episodeTitle = title,
                    segmentId = segmentId,
                    level = episode.level,
                )
                built += Indexed(
                    hit,
                    "${item.term} ${item.fa} ${item.en} ${item.example} ${item.ipa}".lowercase(),
                    WordForm.candidates(item.term) + WordForm.normalize(item.fa),
                )
            }
            episode.phrases.forEach { item ->
                val segmentId = WordForm.candidates(item.en)
                    .firstNotNullOfOrNull { segmentOf[it] } ?: ""
                val hit = SearchHit(
                    key = "p:${episode.id}:${WordForm.normalize(item.en)}",
                    kind = SearchHit.Kind.PHRASE,
                    english = item.en,
                    persian = item.fa,
                    detail = item.note,
                    episodeId = episode.id,
                    episodeTitle = title,
                    segmentId = segmentId,
                    level = episode.level,
                )
                built += Indexed(
                    hit,
                    "${item.en} ${item.fa} ${item.note}".lowercase(),
                    WordForm.candidates(item.en) + WordForm.normalize(item.fa),
                )
            }
        }
        index = built
    }

    fun query(text: String, filter: SearchHit.Kind? = null, limit: Int = 80): List<SearchHit> {
        val current = index ?: return emptyList()
        val needle = WordForm.normalize(text.trim())
        if (needle.length < 2) return emptyList()
        val tokens = needle.split(Regex("\\s+")).filter { it.length >= 2 }
        return current.asSequence()
            .filter { filter == null || it.hit.kind == filter }
            .mapNotNull { entry ->
                val hay = entry.haystack
                // Accept a full substring (Persian or English) or all query tokens.
                val score = when {
                    hay.contains(needle) -> 3
                    entry.terms.any { it.startsWith(needle) } -> 2
                    tokens.isNotEmpty() && tokens.all { hay.contains(it) } -> 1
                    else -> 0
                }
                if (score == 0) null else score to entry.hit
            }
            .sortedWith(compareByDescending<Pair<Int, SearchHit>> { it.first }
                .thenBy { it.second.english.length })
            .take(limit)
            .map { it.second }
            .toList()
    }

    fun countByKind(): Pair<Int, Int> {
        val current = index ?: return 0 to 0
        return current.count { it.hit.kind == SearchHit.Kind.WORD } to
            current.count { it.hit.kind == SearchHit.Kind.PHRASE }
    }
}
