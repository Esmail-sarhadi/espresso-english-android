package ir.esmail.espresso.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the two pieces of pure logic behind "lyrics advance with the song": reading LRC time
 * tags, and deciding which line is active for a given playback position.
 */
class LyricsSyncTest {

    private fun track(lrc: String) = TrackHit(trackName = "Test", syncedLyrics = lrc)

    @Test
    fun `parses hundredths into milliseconds`() {
        val lines = LyricsRepository.parseLines(track("[00:12.50]Look at the stars"))
        assertEquals(1, lines.size)
        assertEquals(12_500L, lines[0].timeMs)
        assertEquals("Look at the stars", lines[0].english)
        assertTrue(lines[0].isTimed)
    }

    @Test
    fun `parses three digit fractions and minute rollover`() {
        val lines = LyricsRepository.parseLines(track("[01:05.250]Second verse"))
        assertEquals(65_250L, lines[0].timeMs)
    }

    @Test
    fun `parses stamps without a fraction`() {
        val lines = LyricsRepository.parseLines(track("[02:07]No fraction"))
        assertEquals(127_000L, lines[0].timeMs)
    }

    @Test
    fun `expands a line carrying several stamps`() {
        val lines = LyricsRepository.parseLines(track("[00:10.00][01:20.00]Chorus"))
        assertEquals(2, lines.size)
        assertEquals(10_000L, lines[0].timeMs)
        assertEquals(80_000L, lines[1].timeMs)
        assertEquals("Chorus", lines[0].english)
        assertEquals("Chorus", lines[1].english)
    }

    @Test
    fun `plain lyrics stay untimed`() {
        val lines = LyricsRepository.parseLines(track("Just a plain line\nand another"))
        assertEquals(2, lines.size)
        assertTrue(lines.none { it.isTimed })
    }

    @Test
    fun `active line follows the playback position`() {
        val lines = LyricsRepository.parseLines(
            track("[00:05.00]one\n[00:12.00]two\n[00:20.00]three"),
        )
        assertEquals(-1, LyricsRepository.activeLineIndex(lines, 4_000L))
        assertEquals(0, LyricsRepository.activeLineIndex(lines, 5_000L))
        assertEquals(0, LyricsRepository.activeLineIndex(lines, 11_999L))
        assertEquals(1, LyricsRepository.activeLineIndex(lines, 12_000L))
        assertEquals(2, LyricsRepository.activeLineIndex(lines, 95_000L))
    }

    @Test
    fun `untimed lines are never highlighted`() {
        val lines = listOf(LyricLine("", "plain"), LyricLine("", "also plain"))
        assertEquals(-1, LyricsRepository.activeLineIndex(lines, 60_000L))
    }

    @Test
    fun `sync offset shifts the active line`() {
        val lines = LyricsRepository.parseLines(track("[00:05.00]one\n[00:12.00]two"))
        // A -3 s correction keeps line 0 active until 9 s of real playback.
        assertEquals(0, LyricsRepository.activeLineIndex(lines, 8_000L - 3_000L))
        assertEquals(1, LyricsRepository.activeLineIndex(lines, 15_000L - 3_000L))
    }

    @Test
    fun `playback match accepts a title buried in the artist field`() {
        // Our own player puts the Persian title first and "Yellow" in the artist string.
        assertTrue(LyricsRepository.matchesPlayback("زرد", "Yellow · Espresso English", "Yellow"))
    }

    @Test
    fun `playback match tolerates live and remaster suffixes`() {
        assertTrue(LyricsRepository.matchesPlayback("Yellow (Live in Buenos Aires)", "Coldplay", "Yellow"))
        assertTrue(LyricsRepository.matchesPlayback("Yellow", "Coldplay", "Yellow - Remastered 2021"))
    }

    @Test
    fun `playback match rejects an unrelated song`() {
        assertTrue(!LyricsRepository.matchesPlayback("Yesterday", "The Beatles", "Yellow"))
        assertTrue(!LyricsRepository.matchesPlayback("", "", "Yellow"))
    }

    @Test
    fun `playback match handles catalogue titles that embed the artist`() {
        // LRCLIB stores "Coldplay - Yellow"; the media session only knows "Yellow".
        assertTrue(LyricsRepository.matchesPlayback("Yellow", "Coldplay", "Coldplay - Yellow"))
        assertTrue(LyricsRepository.matchesPlayback("زرد", "اسپرسو انگلیش · Yellow", "Coldplay - Yellow"))
    }
}
