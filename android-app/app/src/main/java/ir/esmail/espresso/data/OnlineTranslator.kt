package ir.esmail.espresso.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@Serializable
private data class TranslationPayload(val responseData: ResponseData? = null)

@Serializable
private data class ResponseData(val translatedText: String = "", val match: Double = 0.0)

data class TranslationResult(val text: String, val match: Double = 0.0)

/**
 * Online translation.
 *
 * Two backends are used: Google's public `gtx` endpoint carries a whole paragraph (up to ~4k
 * characters) in a single round trip, which is what makes full-song translation both fast and
 * complete; MyMemory stays as a per-line fallback when the first one is unreachable.
 *
 * Failures are returned as readable messages, never as crashes, and all lesson content remains
 * fully usable offline.
 */
class OnlineTranslator {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Single short string – used by the screen translator and word lookups. */
    suspend fun translate(text: String, from: String, to: String): Result<TranslationResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val clean = text.trim()
                if (clean.isBlank()) throw IllegalArgumentException("متنی برای ترجمه وارد نشده است.")
                if (clean.length > 4_000) throw IllegalArgumentException("متن بیش از حد بلند است.")

                val fast = runCatching { google(clean, from, to) }.getOrNull()
                if (!fast.isNullOrBlank()) return@runCatching TranslationResult(fast, 1.0)

                TranslationResult(myMemory(clean, from, to), 0.6)
            }
        }

    /**
     * Translates every line, preserving a one-to-one mapping. Lines are packed into blocks of
     * ~3.5k characters, so a whole song costs one or two requests instead of one per line.
     * [onProgress] reports how many lines are done so the UI can fill in as results arrive.
     */
    suspend fun translateLines(
        lines: List<String>,
        from: String,
        to: String,
        onProgress: suspend (done: Int, total: Int, translated: Map<Int, String>) -> Unit = { _, _, _ -> },
    ): Result<Map<Int, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val targets = lines.indices.filter { lines[it].isNotBlank() }
            if (targets.isEmpty()) return@runCatching emptyMap()

            val result = LinkedHashMap<Int, String>()
            var done = 0

            for (block in packBlocks(lines, targets)) {
                val joined = block.joinToString("\n") { lines[it].trim() }
                val translatedBlock = runCatching { google(joined, from, to) }.getOrNull()

                if (translatedBlock != null) {
                    val parts = translatedBlock.replace("\r\n", "\n").split("\n")
                    if (parts.size == block.size) {
                        block.forEachIndexed { position, index ->
                            val value = parts[position].trim()
                            if (value.isNotEmpty()) result[index] = value
                        }
                        done += block.size
                        onProgress(done, targets.size, result.toMap())
                        continue
                    }
                }

                // Fallback: translate this block line by line, four at a time.
                val semaphore = Semaphore(4)
                val outcomes = coroutineScope {
                    block.map { index ->
                        async {
                            semaphore.withPermit {
                                index to runCatching { translateOne(lines[index], from, to) }.getOrNull()
                            }
                        }
                    }.awaitAll()
                }
                for ((index, value) in outcomes) {
                    if (!value.isNullOrBlank()) result[index] = value
                }
                done += block.size
                onProgress(done, targets.size, result.toMap())
            }

            result
        }
    }

    private fun packBlocks(lines: List<String>, targets: List<Int>, limit: Int = 3_500): List<List<Int>> {
        val blocks = ArrayList<List<Int>>()
        var current = ArrayList<Int>()
        var size = 0
        for (index in targets) {
            val length = lines[index].trim().length + 1
            if (current.isNotEmpty() && size + length > limit) {
                blocks += current
                current = ArrayList()
                size = 0
            }
            current += index
            size += length
        }
        if (current.isNotEmpty()) blocks += current
        return blocks
    }

    private fun translateOne(text: String, from: String, to: String): String =
        runCatching { google(text, from, to) }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: myMemory(text, from, to)

    // ---------------------------------------------------------------- backends

    /** Google's public endpoint: one request can carry a whole paragraph. */
    private fun google(text: String, from: String, to: String): String {
        val query = URLEncoder.encode(text, "UTF-8")
        val url = URL("$GOOGLE?client=gtx&sl=$from&tl=$to&dt=t&q=$query")
        val body = read(url)

        val root = json.parseToJsonElement(body).jsonArray
        val segments = root.firstOrNull() as? JsonArray ?: throw IOException("پاسخ ترجمه نامعتبر بود.")
        val builder = StringBuilder()
        for (segment in segments) {
            val parts = segment as? JsonArray ?: continue
            val piece = (parts.firstOrNull() as? JsonPrimitive)?.content.orEmpty()
            builder.append(piece)
        }
        val out = builder.toString().trim()
        if (out.isBlank()) throw IOException("ترجمه‌ای دریافت نشد.")
        return out
    }

    private fun myMemory(text: String, from: String, to: String): String {
        val query = URLEncoder.encode(text, "UTF-8")
        val pair = URLEncoder.encode("$from|$to", "UTF-8")
        val url = URL("$MYMEMORY?q=$query&langpair=$pair")
        val payload = json.decodeFromString<TranslationPayload>(read(url))
        val translated = payload.responseData?.translatedText.orEmpty()
        if (translated.isBlank()) throw IOException("ترجمه‌ای دریافت نشد. دوباره تلاش کن.")
        return translated
    }

    private fun read(url: URL): String {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android) EspressoEnglish/5.4")
        }
        return try {
            when (val code = connection.responseCode) {
                in 200..299 -> connection.inputStream.bufferedReader().use { it.readText() }
                else -> throw IOException("سرویس ترجمه پاسخ $code داد.")
            }
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    companion object {
        private const val GOOGLE = "https://translate.googleapis.com/translate_a/single"
        private const val MYMEMORY = "https://api.mymemory.translated.net/get"

        const val SOURCE_NAME = "Google Translate · MyMemory"
        const val SOURCE_URL = "https://mymemory.translated.net/doc/spec.php"
    }
}
