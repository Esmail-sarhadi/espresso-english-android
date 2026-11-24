package ir.esmail.espresso.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import ir.esmail.espresso.data.WordToken
import ir.esmail.espresso.ui.theme.espresso

private data class Hit(val index: Int, val range: IntRange, val token: WordToken)

/**
 * English transcript line where every token is independently tappable.
 *
 * The hit map is built once per (text, tokens) pair and the tap position is resolved through
 * the text layout, so a line with 30 words stays a single text node – no per-word composables,
 * no measurable cost while scrolling.
 */
@Composable
fun TappableWords(
    text: String,
    tokens: List<WordToken>,
    style: TextStyle,
    modifier: Modifier = Modifier,
    activeIndex: Int = -1,
    savedWords: Set<String> = emptySet(),
    onWordTap: (WordToken) -> Unit,
) {
    val palette = espresso
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }

    val hits = remember(text, tokens) { buildHits(text, tokens) }

    val annotated = remember(text, tokens, activeIndex, savedWords, hits) {
        buildAnnotatedString {
            append(text)
            hits.forEach { hit ->
                val saved = hit.token.w.lowercase().trim('.', ',', '!', '?', ';', ':', '"', '\'') in savedWords
                when {
                    hit.index == activeIndex -> addStyle(
                        SpanStyle(
                            color = palette.accentInk,
                            background = palette.accent,
                            fontWeight = FontWeight.Bold,
                        ),
                        hit.range.first,
                        hit.range.last + 1,
                    )

                    saved -> addStyle(
                        SpanStyle(
                            color = palette.mint,
                            textDecoration = TextDecoration.Underline,
                        ),
                        hit.range.first,
                        hit.range.last + 1,
                    )
                }
            }
        }
    }

    BasicText(
        text = annotated,
        style = style.copy(color = palette.textPrimary),
        modifier = modifier.pointerInput(hits) {
            detectTapGestures { position ->
                val result = layout ?: return@detectTapGestures
                val offset = result.getOffsetForPosition(position)
                hits.firstOrNull { offset in it.range }?.let { onWordTap(it.token) }
            }
        },
        onTextLayout = { layout = it },
    )
}

private fun buildHits(text: String, tokens: List<WordToken>): List<Hit> {
    if (tokens.isEmpty()) return emptyList()
    val hits = ArrayList<Hit>(tokens.size)
    var cursor = 0
    tokens.forEachIndexed { index, token ->
        val needle = token.w
        if (needle.isBlank()) return@forEachIndexed
        val found = text.indexOf(needle, cursor, ignoreCase = true)
        if (found < 0) return@forEachIndexed
        val range = found until (found + needle.length)
        hits += Hit(index, range, token)
        cursor = found + needle.length
    }
    return hits
}

/** Compact English quote used inside the word sheet. */
@Composable
fun EnglishLine(text: String, style: TextStyle, color: Color? = null) {
    BasicText(
        text = AnnotatedString(text),
        style = style.copy(color = color ?: espresso.textSecondary),
    )
}
