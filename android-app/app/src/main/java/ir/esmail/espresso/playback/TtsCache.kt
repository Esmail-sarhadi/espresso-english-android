package ir.esmail.espresso.playback

import android.content.Context
import ir.esmail.espresso.data.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

/**
 * Turns a text-only lesson into a WAV in the app cache. Media3 can then play that file exactly like
 * an MP3, so notifications, audio focus, seek and lock-screen controls keep working unchanged.
 *
 * Each segment is synthesized separately and the PCM is concatenated into one WAV. This keeps
 * every call to the native engine small (avoiding buffer/sentence-limit failures on long
 * lessons) while still producing a single seekable media item for the player.
 */
class TtsCache(
    private val context: Context,
    private val engine: BundledTts,
    private val systemTts: SystemTts,
    private val preferSystemVoice: () -> Boolean = { true },
) {

    private val mutex = Mutex()

    suspend fun audioFile(episode: Episode): File = withContext(Dispatchers.IO) {
        mutex.withLock {
            val dir = File(context.cacheDir, "tts-audio")
            dir.mkdirs()
            val safeId = episode.id.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
            val file = File(dir, "$safeId.wav")
            if (file.exists() && file.length() > 1_024L) return@withLock file

            // A partial/failed file from a previous run must never be treated as a cache hit.
            runCatching { if (file.exists()) file.delete() }

            val texts = episode.segments.map { it.text.trim() }
                .filter { it.isNotEmpty() }
                .ifEmpty { listOf(episode.title.ifBlank { episode.titleFa }) }

            // Prefer the device's natural neural voice: zero APK cost and far less "robotic"
            // than the small bundled model. Fall back per lesson if the device has no voice.
            val systemReady = preferSystemVoice() && systemTts.isAvailable()
            if (systemReady) {
                val tempDir = File(dir, "sys-${safeId}").also { it.mkdirs() }
                val wavFiles = ArrayList<File>(texts.size)
                var ok = true
                for ((index, text) in texts.withIndex()) {
                    val part = File(tempDir, "part-$index.wav")
                    if (!systemTts.synthesize(text, part)) {
                        ok = false
                        break
                    }
                    wavFiles.add(part)
                }
                if (ok && wavFiles.isNotEmpty()) {
                    val combined = concatenateWavs(wavFiles, file)
                    tempDir.deleteRecursively()
                    if (combined) {
                        android.util.Log.i(
                            "TtsCache",
                            "built ${file.name} with SYSTEM voice: parts=${wavFiles.size} bytes=${file.length()}",
                        )
                        prune(dir)
                        return@withLock file
                    }
                }
                runCatching { tempDir.deleteRecursively() }
                android.util.Log.w("TtsCache", "system voice failed; falling back to bundled Piper")
            }

            var totalSamples = 0
            val parts = ArrayList<FloatArray>(texts.size)
            for (text in texts) {
                val generated = engine.synthesize(text)
                if (generated.samples.isEmpty()) {
                    error("صوت این درس ساخته نشد (متن: ${text.take(40)}).")
                }
                totalSamples += generated.samples.size
                parts.add(generated.samples)
            }
            if (totalSamples <= 0) error("صوت این درس ساخته نشد (هیچ نمونهای ساخته نشد).")

            val sampleRate = engine.sampleRate
            val pcm = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (part in parts) {
                for (sample in part) {
                    val clamped = max(-1f, minOf(1f, sample))
                    pcm.putShort((clamped * Short.MAX_VALUE).toInt().toShort())
                }
            }
            writeWav(file, pcm.array(), sampleRate)
            android.util.Log.i(
                "TtsCache",
                "built ${file.name}: parts=${parts.size} samples=$totalSamples rate=$sampleRate bytes=${file.length()}",
            )
            prune(dir)
            file
        }
    }

    /**
     * Joins the per-segment WAVs produced by the system engine into one file. Resamples nothing:
     * Android's TTS output is normally 16-bit PCM mono at 16/22/24 kHz, and we keep the first
     * file's rate for the combined header.
     */
    private fun concatenateWavs(parts: List<File>, output: File): Boolean {
        val chunks = ArrayList<ByteArray>(parts.size)
        var rate = 0
        for (part in parts) {
            val raw = part.readBytes()
            if (raw.size < 44 || String(raw.copyOfRange(0, 4), Charsets.US_ASCII) != "RIFF") return false
            val channels = ByteBuffer.wrap(raw, 22, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
            val bits = ByteBuffer.wrap(raw, 34, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
            val partRate = ByteBuffer.wrap(raw, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (channels != 1 || bits != 16) return false
            if (rate == 0) rate = partRate
            if (partRate != rate) return false
            val dataLength = ByteBuffer.wrap(raw, 40, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (dataLength <= 0 || 44 + dataLength > raw.size) return false
            chunks.add(raw.copyOfRange(44, 44 + dataLength))
        }
        if (rate <= 0) return false
        val pcm = ByteArray(chunks.sumOf { it.size })
        var offset = 0
        chunks.forEach { chunk ->
            System.arraycopy(chunk, 0, pcm, offset, chunk.size)
            offset += chunk.size
        }
        writeWav(output, pcm, rate)
        return output.length() > 1_024L
    }

    /** Writes a 16-bit mono PCM RIFF/WAVE file with a correct header. */
    private fun writeWav(file: File, pcm: ByteArray, sampleRate: Int) {
        val dataSize = pcm.size
        RandomAccessFile(file, "rw").use { out ->
            out.setLength(0)
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(36 + dataSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16)             // PCM chunk size
            header.putShort(1.toShort())  // PCM format
            header.putShort(1.toShort())  // mono
            header.putInt(sampleRate)
            header.putInt(sampleRate * 2) // byte rate
            header.putShort(2.toShort())  // block align
            header.putShort(16.toShort()) // bits per sample
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(dataSize)
            out.write(header.array())
            out.write(pcm)
        }
    }

    private fun prune(dir: File) {
        val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: return
        if (files.size <= 12) return
        files.take(files.size - 12).forEach { runCatching { it.delete() } }
    }
}
