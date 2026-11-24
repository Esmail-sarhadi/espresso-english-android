package ir.esmail.espresso.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A track returned by one of the free, keyless lyrics catalogues. */
@Serializable
data class TrackHit(
    val id: Long = 0L,
    val trackName: String = "",
    val artistName: String = "",
    val albumName: String = "",
    val duration: Double = 0.0,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
    val instrumental: Boolean = false,
    /** Which catalogue produced this row: lrclib, itunes or deezer. */
    val provider: String = PROVIDER_LRCLIB,
) {
    /** "Coldplay - Yellow" by "Coldplay" reads as "Yellow". */
    val displayTitle: String
        get() {
            val artist = artistName.trim()
            val title = trackName.trim()
            if (artist.isNotEmpty() && title.startsWith("$artist - ", ignoreCase = true)) {
                return title.substring(artist.length + 3).trim().ifBlank { title }
            }
            return title.ifBlank { "—" }
        }
    val displaySubtitle: String
        get() {
            val parts = ArrayList<String>(2)
            if (artistName.isNotBlank()) parts += artistName.trim()
            val album = albumName.trim()
            // LRCLIB sometimes stores "Artist - Title" as the album; repeating it looks broken.
            val redundant = album.isEmpty() ||
                album.equals("$artistName - $trackName", ignoreCase = true) ||
                album.startsWith("$artistName - ", ignoreCase = true)
            if (!redundant) parts += album
            return parts.joinToString(" · ")
        }
    val hasLyrics: Boolean get() = !plainLyrics.isNullOrBlank() || !syncedLyrics.isNullOrBlank()

    /** Stable list key – ids from different catalogues can collide. */
    val uid: String get() = "$provider|$id|$trackName|$artistName"

    companion object {
        const val PROVIDER_LRCLIB = "lrclib"
        const val PROVIDER_ITUNES = "itunes"
        const val PROVIDER_DEEZER = "deezer"
        const val PROVIDER_OVH = "lyrics.ovh"
    }
}

data class LyricLine(val time: String, val english: String, val timeMs: Long = -1L) {
    val isTimed: Boolean get() = timeMs >= 0L
}

enum class Reach { OK, EMPTY, BLOCKED }

/** Per-catalogue health, so the UI can say "blocked" instead of lying with "not found". */
data class ProviderReport(val name: String, val reach: Reach, val detail: String = "")

data class SearchOutcome(
    val hits: List<TrackHit>,
    val reports: List<ProviderReport>,
) {
    val allBlocked: Boolean
        get() = reports.isNotEmpty() && reports.all { it.reach == Reach.BLOCKED }

    val blockedNames: List<String>
        get() = reports.filter { it.reach == Reach.BLOCKED }.map { it.name }
}

/**
 * Song lookup + lyrics over four free, keyless catalogues. Every provider is independent, so one
 * blocked host (a very common situation on Iranian mobile networks) can no longer make the whole
 * feature report "no song found": we merge whatever answers and tell the user which host failed.
 */
class LyricsRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ---------------------------------------------------------------- search

    suspend fun search(query: String, limit: Int = 10): Result<SearchOutcome> =
        withContext(Dispatchers.IO) {
            runCatching {
                val clean = cleanQuery(query)
                if (clean.isBlank()) throw IllegalArgumentException("نام آهنگ یا خواننده را وارد کن.")
                val variants = queryVariants(clean)

                val groups = coroutineScope {
                    val lrclib = async { guard("LRCLIB") { searchLrclib(variants, limit) } }
                    val itunes = async { guard("iTunes") { searchItunes(clean, limit) } }
                    val deezer = async { guard("Deezer") { searchDeezer(clean, limit) } }
                    listOf(lrclib.await(), itunes.await(), deezer.await())
                }

                SearchOutcome(
                    hits = mergeHits(groups.map { it.hits }, clean).take(limit),
                    reports = groups.map { it.report },
                )
            }
        }

    private data class Group(val report: ProviderReport, val hits: List<TrackHit>)

    private inline fun guard(name: String, block: () -> List<TrackHit>): Group = try {
        val hits = block()
        Group(
            report = ProviderReport(name, if (hits.isEmpty()) Reach.EMPTY else Reach.OK),
            hits = hits,
        )
    } catch (error: Throwable) {
        Group(ProviderReport(name, Reach.BLOCKED, error.message.orEmpty()), emptyList())
    }

    private fun searchLrclib(variants: List<String>, limit: Int): List<TrackHit> {
        for (candidate in variants) {
            // LRCLIB's documented free-text parameter is `q`; `query_string` is the legacy name
            // and silently returns nothing on the current API.
            val url = URL("$LRCLIB/api/search?q=${URLEncoder.encode(candidate, "UTF-8")}")
            val body = read(url)
            val all = json.decodeFromString<List<TrackHit>>(body)
            val hits = all.filter { it.trackName.isNotBlank() }.map { it.copy(provider = TrackHit.PROVIDER_LRCLIB) }
            if (hits.isNotEmpty()) return hits.take(limit)
        }
        return emptyList()
    }

    private fun searchItunes(query: String, limit: Int): List<TrackHit> {
        val url = URL("$ITUNES/search?term=${URLEncoder.encode(query, "UTF-8")}&entity=song&limit=$limit")
        val payload = json.decodeFromString<ITunesResponse>(read(url))
        return payload.results
            .filter { it.trackName.isNotBlank() }
            .map {
                TrackHit(
                    id = it.trackId,
                    trackName = it.trackName,
                    artistName = it.artistName,
                    albumName = it.collectionName,
                    duration = it.trackTimeMillis / 1000.0,
                    provider = TrackHit.PROVIDER_ITUNES,
                )
            }
    }

    private fun searchDeezer(query: String, limit: Int): List<TrackHit> {
        val url = URL("$DEEZER/search?q=${URLEncoder.encode(query, "UTF-8")}&limit=$limit")
        val payload = json.decodeFromString<DeezerResponse>(read(url))
        return payload.data
            .filter { it.title.isNotBlank() }
            .map {
                TrackHit(
                    id = it.id,
                    trackName = it.title,
                    artistName = it.artist?.name.orEmpty(),
                    albumName = it.album?.title.orEmpty(),
                    duration = it.duration.toDouble(),
                    provider = TrackHit.PROVIDER_DEEZER,
                )
            }
    }

    // --------------------------------------------------------------- resolve

    /** Fills in lyrics for a search hit that came from a catalogue without them. */
    suspend fun resolve(track: TrackHit): Result<TrackHit> = withContext(Dispatchers.IO) {
        runCatching {
            if (track.hasLyrics) return@runCatching track

            lookupLrclib(track.trackName, track.artistName, track.duration.toInt())
                ?.takeIf { it.hasLyrics }
                ?.let { return@runCatching it.copy(provider = TrackHit.PROVIDER_LRCLIB) }

            fetchOvh(track.trackName, track.artistName)
                ?.let { return@runCatching track.copy(plainLyrics = it, provider = TrackHit.PROVIDER_OVH) }

            throw IOException("ترانه‌ای برای «${track.trackName}» در هیچ منبعی پیدا نشد.")
        }
    }

    private fun lookupLrclib(track: String, artist: String, duration: Int): TrackHit? {
        val url = URL(
            "$LRCLIB/api/get?track_name=${URLEncoder.encode(track.trim(), "UTF-8")}" +
                "&artist_name=${URLEncoder.encode(artist.trim(), "UTF-8")}&duration=$duration",
        )
        val body = runCatching { read(url) }.getOrNull() ?: return null
        return runCatching { json.decodeFromString<TrackHit>(body) }.getOrNull()
    }

    private fun fetchOvh(track: String, artist: String): String? {
        if (artist.isBlank()) return null
        val url = URL("$OVH/v1/${URLEncoder.encode(artist, "UTF-8")}/${URLEncoder.encode(track, "UTF-8")}")
        val body = runCatching { read(url) }.getOrNull() ?: return null
        val payload = runCatching { json.decodeFromString<OvhLyrics>(body) }.getOrNull() ?: return null
        val text = payload.lyrics
            .replace("\r\n", "\n")
            .lines()
            .filterNot { it.startsWith("Paroles de la chanson", ignoreCase = true) }
            .joinToString("\n")
            .trim()
        return text.takeIf { it.isNotBlank() }
    }

    // ----------------------------------------------------------- diagnostics

    /** Pings every catalogue once. Used by the connectivity card in Settings. */
    suspend fun diagnose(): List<ProviderReport> = withContext(Dispatchers.IO) {
        coroutineScope {
            val checks = listOf(
                "LRCLIB" to { read(URL("$LRCLIB/api/search?query_string=coldplay")).isNotBlank() },
                "iTunes" to { read(URL("$ITUNES/search?term=coldplay&entity=song&limit=1")).isNotBlank() },
                "Deezer" to { read(URL("$DEEZER/search?q=coldplay&limit=1")).isNotBlank() },
            )
            checks.map { (name, probe) ->
                async {
                    val started = System.currentTimeMillis()
                    runCatching { probe() }.fold(
                        onSuccess = { ProviderReport(name, Reach.OK, "${System.currentTimeMillis() - started} ms") },
                        onFailure = { ProviderReport(name, Reach.BLOCKED, it.message.orEmpty().take(80)) },
                    )
                }
            }.map { it.await() }
        }
    }

    // ------------------------------------------------------------------ http

    private fun read(url: URL): String {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 10_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "EspressoEnglish/5.2 (Android)")
        }
        return try {
            when (val code = connection.responseCode) {
                in 200..299 -> connection.inputStream.bufferedReader().use { it.readText() }
                404 -> throw HttpFailure(404, "پیدا نشد")
                else -> throw HttpFailure(code, "پاسخ $code")
            }
        } catch (error: HttpFailure) {
            throw error
        } catch (error: IOException) {
            throw IOException("اتصال برقرار نشد (${error.message ?: "شبکه"})")
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    private class HttpFailure(val code: Int, message: String) : IOException(message)

    // ---------------------------------------------------------------- helpers

    private fun mergeHits(groups: List<List<TrackHit>>, query: String): List<TrackHit> {
        val merged = LinkedHashMap<String, TrackHit>()
        for (hit in groups.flatten()) {
            val key = "${fold(hit.trackName)}|${fold(hit.artistName)}"
            val existing = merged[key]
            when {
                existing == null -> merged[key] = hit
                !existing.hasLyrics && hit.hasLyrics -> merged[key] = hit
            }
        }
        val tokens = query.lowercase().split(" ").filter { it.length > 1 }
        return merged.values.sortedByDescending { relevance(it, tokens) }
    }

    /** Rows that match the typed words and already carry lyrics float to the top. */
    private fun relevance(hit: TrackHit, tokens: List<String>): Int {
        val hay = fold("${hit.trackName} ${hit.artistName}")
        var score = tokens.count { hay.contains(fold(it)) } * 3
        if (hit.hasLyrics) score += 2
        if (hit.provider == TrackHit.PROVIDER_LRCLIB) score += 1
        return score
    }

    private fun fold(value: String): String = value.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), "")

    private fun queryVariants(clean: String): List<String> = listOfNotNull(
        clean,
        clean.replace(Regex("(?i)\\s*\\(?(live|remix|official|audio|lyrics|video|hd|hq)\\)?"), " ")
            .replace(Regex("\\s+"), " ").trim()
            .takeIf { it.isNotBlank() && it != clean },
        clean.split(" - ").firstOrNull()?.trim()?.takeIf { it.length > 2 },
    ).distinct()

    companion object {
        private const val LRCLIB = "https://lrclib.net"
        private const val ITUNES = "https://itunes.apple.com"
        private const val DEEZER = "https://api.deezer.com"
        private const val OVH = "https://api.lyrics.ovh"

        const val SOURCE_URL = "https://lrclib.net"

        /** Removes junk that media sessions often append to file names. */
        fun cleanQuery(raw: String): String = raw
            .replace(Regex("(?i)[-–—_\\s]*(download|\\.ir|\\.com|music|mp3|official|lyrics)[-–—_\\s]*"), " ")
            .replace(Regex("(?i)\\s*www\\.\\S+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        /** Parses LRC time tags into simple lines; falls back to plain lyrics. */
        fun parseLines(track: TrackHit): List<LyricLine> {
            val source = track.syncedLyrics?.takeIf { it.isNotBlank() } ?: track.plainLyrics.orEmpty()
            if (source.isBlank()) return emptyList()
            val tag = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?\\]")
            val lines = ArrayList<LyricLine>()
            for (raw in source.lineSequence()) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("[offset:")) continue

                // A single line can carry several stamps, e.g. "[00:12.00][01:20.00]chorus".
                val stamps = ArrayList<Long>(1)
                var cursor = 0
                while (true) {
                    val match = tag.matchAt(line, cursor) ?: break
                    val minutes = match.groupValues[1].toLongOrNull() ?: break
                    val seconds = match.groupValues[2].toLongOrNull() ?: break
                    val fraction = match.groupValues[3]
                    val millis = when (fraction.length) {
                        0 -> 0L
                        1 -> fraction.toLong() * 100
                        2 -> fraction.toLong() * 10
                        else -> fraction.take(3).toLong()
                    }
                    stamps.add(minutes * 60_000L + seconds * 1_000L + millis)
                    cursor = match.range.last + 1
                }

                val text = line.substring(cursor).trim()
                if (text.isEmpty()) continue
                if (stamps.isEmpty()) {
                    lines.add(LyricLine("", text))
                } else {
                    for (stamp in stamps) {
                        lines.add(LyricLine(formatStamp(stamp), text, stamp))
                    }
                }
            }
            return lines.sortedBy { if (it.isTimed) it.timeMs else Long.MAX_VALUE }
        }

        private fun formatStamp(ms: Long): String {
            val totalSeconds = ms / 1000L
            val minutes = totalSeconds / 60L
            val seconds = totalSeconds % 60L
            val hundredths = (ms % 1000L) / 10L
            return "%d:%02d.%02d".format(minutes, seconds, hundredths)
        }

        /** Index of the lyric line that should be highlighted at [positionMs], or -1. */
        fun activeLineIndex(lines: List<LyricLine>, positionMs: Long): Int =
            lines.indexOfLast { it.isTimed && it.timeMs <= positionMs }

        /**
         * Decides whether the lyrics on screen belong to the song that is playing. Players are
         * inconsistent about which metadata field carries the song name – some put it in the
         * title, some in the artist string, some append "(Live)" or "- Remastered" – so the whole
         * blob is searched rather than the title alone. Catalogue rows can also arrive as
         * "Coldplay - Yellow", so every " - " segment is tried as well.
         */
        fun matchesPlayback(snapshotTitle: String, snapshotArtist: String, trackName: String): Boolean {
            val hay = foldText(snapshotTitle + " " + snapshotArtist)
            if (hay.isEmpty()) return false
            val candidates = LinkedHashSet<String>()
            for (part in trackName.split(" - ", " – ", " — ", "-")) {
                val folded = foldText(part)
                if (folded.length >= 3) candidates.add(folded)
            }
            if (candidates.isEmpty()) return false
            return candidates.any { candidate ->
                hay.contains(candidate) || (candidate.length >= 5 && hay.contains(candidate.take(5)))
            }
        }

        private fun foldText(value: String): String =
            value.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")
    }
}

@Serializable
private data class ITunesResponse(val resultCount: Int = 0, val results: List<ITunesTrack> = emptyList())

@Serializable
private data class ITunesTrack(
    val trackId: Long = 0L,
    val trackName: String = "",
    val artistName: String = "",
    val collectionName: String = "",
    val trackTimeMillis: Long = 0L,
)

@Serializable
private data class DeezerResponse(val data: List<DeezerTrack> = emptyList())

@Serializable
private data class DeezerTrack(
    val id: Long = 0L,
    val title: String = "",
    val duration: Int = 0,
    val artist: DeezerArtist? = null,
    val album: DeezerAlbum? = null,
)

@Serializable
private data class DeezerArtist(val name: String = "")

@Serializable
private data class DeezerAlbum(val title: String = "")

@Serializable
private data class OvhLyrics(val lyrics: String = "")
