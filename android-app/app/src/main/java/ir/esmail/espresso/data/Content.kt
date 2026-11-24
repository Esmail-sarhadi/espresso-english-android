package ir.esmail.espresso.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/* ------------------------------------------------------------------------------------------
 * Bundled content model. The JSON files in assets/content are produced by the authoring
 * pipeline (Whisper alignment + hand written Persian translations) and are immutable at
 * runtime, so they are parsed once and kept in memory for the lifetime of the process.
 * ------------------------------------------------------------------------------------------ */

@Serializable
data class WordToken(
    val w: String,
    val s: Double = 0.0,
    val e: Double = 0.0,
)

@Serializable
data class Segment(
    val id: String = "",
    val start: Double = 0.0,
    val end: Double = 0.0,
    val text: String = "",
    val words: List<WordToken> = emptyList(),
    val fa: String = "",
) {
    val startMs: Long get() = (start * 1000).toLong()
    val endMs: Long get() = (end * 1000).toLong()
}

@Serializable
data class VocabItem(
    val term: String,
    val fa: String = "",
    val pos: String = "",
    val en: String = "",
    val example: String = "",
    val ipa: String = "",
)

@Serializable
data class PhraseItem(
    val en: String,
    val fa: String = "",
    val note: String = "",
)

@Serializable
data class Episode(
    val id: String,
    val title: String,
    val titleFa: String = "",
    val subtitle: String = "",
    val category: String = "",
    val level: String = "beginner",
    val duration: Double = 0.0,
    val audio: String = "",
    val peaks: List<Float> = emptyList(),
    val descriptionFa: String = "",
    val tags: List<String> = emptyList(),
    val source: String = "",
    val sourceUrl: String = "",
    val ttsOnly: Boolean = false,
    val segments: List<Segment> = emptyList(),
    val vocab: List<VocabItem> = emptyList(),
    val phrases: List<PhraseItem> = emptyList(),
) {
    val durationMs: Long get() = (duration * 1000).toLong().coerceAtLeast(1L)
    val assetPath: String get() = "audio/$id.mp3"
    val coverAsset: String get() = "covers/$id.png"
    val vocabCount: Int get() = vocab.size
    val segmentCount: Int get() = segments.size
}

@Serializable
data class GlossaryEntry(
    val fa: String = "",
    val pos: String = "",
    val en: String = "",
    val ipa: String = "",
)

@Serializable
data class PodcastRef(
    val name: String,
    val host: String = "",
    val level: String = "",
    val accent: String = "",
    val url: String = "",
    val descriptionFa: String = "",
    val whyFa: String = "",
    val tags: List<String> = emptyList(),
    val badge: String = "",
)

/** Everything the UI needs, resolved once. */
class ContentBundle(
    val episodes: List<Episode>,
    val glossary: Map<String, GlossaryEntry>,
    val podcasts: List<PodcastRef>,
) {
    val byId: Map<String, Episode> = episodes.associateBy { it.id }

    private val glossaryByForm: Map<String, GlossaryEntry> =
        glossary.entries.associate { (term, entry) -> WordForm.normalize(term) to entry }

    fun episode(id: String?): Episode? = id?.let { byId[it] }

    fun nextOf(episodeId: String): Episode? {
        val index = episodes.indexOfFirst { it.id == episodeId }
        return if (index >= 0 && index + 1 < episodes.size) episodes[index + 1] else null
    }

    fun previousOf(episodeId: String): Episode? {
        val index = episodes.indexOfFirst { it.id == episodeId }
        return if (index > 0) episodes[index - 1] else null
    }

    /** Resolves the meaning of a tapped token, preferring the vocabulary of the current lesson. */
    fun meaning(raw: String, episode: Episode?): WordMeaning? {
        val candidates = WordForm.candidates(raw)
        if (candidates.isEmpty()) return null

        episode?.vocab?.forEach { item ->
            val forms = WordForm.candidates(item.term) + WordForm.normalize(item.term)
            if (forms.any { it in candidates }) {
                return WordMeaning(
                    term = item.term,
                    fa = item.fa,
                    pos = item.pos,
                    ipa = item.ipa,
                    en = item.en,
                    example = item.example,
                    source = "درس",
                )
            }
        }

        for (candidate in candidates) {
            glossaryByForm[candidate]?.let { entry ->
                return WordMeaning(
                    term = candidate,
                    fa = entry.fa,
                    pos = entry.pos,
                    ipa = entry.ipa,
                    en = entry.en,
                    example = "",
                    source = "واژه‌نامه",
                )
            }
        }
        return null
    }

    companion object {
        val Empty = ContentBundle(emptyList(), emptyMap(), emptyList())
    }
}

data class WordMeaning(
    val term: String,
    val fa: String,
    val pos: String = "",
    val ipa: String = "",
    val en: String = "",
    val example: String = "",
    val source: String = "",
)

class ContentRepository(private val context: Context) {

    private companion object {
        const val TAG = "ContentRepository"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val mutex = Mutex()

    @Volatile
    private var cached: ContentBundle? = null

    suspend fun load(): ContentBundle {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: withContext(Dispatchers.IO) { readBundle() }.also { cached = it }
        }
    }

    private fun readBundle(): ContentBundle {
        val episodes = runCatching {
            json.decodeFromString<List<Episode>>(read("content/episodes.json"))
        }.onFailure { Log.e(TAG, "episodes.json could not be parsed", it) }.getOrElse { emptyList() }

        val glossary = runCatching {
            json.decodeFromString<Map<String, GlossaryEntry>>(read("content/glossary.json"))
        }.onFailure { Log.e(TAG, "glossary.json could not be parsed", it) }.getOrElse { emptyMap() }

        val podcasts = runCatching {
            json.decodeFromString<List<PodcastRef>>(read("content/podcasts.json"))
        }.onFailure { Log.e(TAG, "podcasts.json could not be parsed", it) }.getOrElse { emptyList() }

        // Beginners first, then by length: the library reads as a course, and the lesson
        // numbers printed on the cover art stay in sync with the order shown in the app.
        val levelRank = mapOf("beginner" to 0, "intermediate" to 1, "advanced" to 2)
        return ContentBundle(
            episodes = episodes.sortedWith(
                compareBy({ levelRank[it.level.lowercase()] ?: 1 }, { it.duration }),
            ),
            glossary = glossary,
            podcasts = podcasts,
        )
    }

    private fun read(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }
}
