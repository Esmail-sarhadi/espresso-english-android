package ir.esmail.espresso.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.esmail.espresso.data.ContentBundle
import ir.esmail.espresso.data.SavedWordEntity
import ir.esmail.espresso.data.Srs
import ir.esmail.espresso.data.WordMeaning
import ir.esmail.espresso.ui.AppViewModel
import ir.esmail.espresso.ui.Fa
import ir.esmail.espresso.ui.components.EmptyState
import ir.esmail.espresso.ui.components.FilterPill
import ir.esmail.espresso.ui.components.WordSheet
import ir.esmail.espresso.ui.theme.Fraunces
import ir.esmail.espresso.ui.theme.espresso

@Composable
fun NotebookScreen(
    vm: AppViewModel,
    bundle: ContentBundle,
    bottomInset: Dp,
    onReview: () -> Unit,
) {
    val palette = espresso
    val words by vm.words.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    var selected by remember { mutableStateOf<SavedWordEntity?>(null) }

    val now = System.currentTimeMillis()
    val filtered = remember(words, query, filter, now) {
        words.filter { word ->
            val matchesQuery = query.isBlank() ||
                word.term.contains(query, ignoreCase = true) ||
                word.meaning.contains(query) ||
                (word.episodeTitle?.contains(query, ignoreCase = true) == true)
            val matchesFilter = when (filter) {
                "due" -> !word.mastered && word.dueAt <= now
                "mastered" -> word.mastered
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(top = 18.dp, bottom = bottomInset + 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "دفترچهٔ واژه‌ها",
                            style = MaterialTheme.typography.headlineLarge,
                            color = palette.textPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${Fa.num(words.size)} واژه ذخیره‌شده، ${Fa.num(words.count { it.mastered })} واژه مسلط",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textTertiary,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.mint.copy(alpha = 0.14f))
                            .clickable(onClick = onReview)
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Rounded.Style,
                                contentDescription = null,
                                tint = palette.mint,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "مرور",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.mint,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                SearchField(value = query, onValueChange = { query = it }, placeholder = "جست‌وجو در واژه‌ها…")
                Spacer(Modifier.height(12.dp))
            }
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { FilterPill("همه", filter == "all", onClick = { filter = "all" }) }
                item { FilterPill("آمادهٔ مرور", filter == "due", onClick = { filter = "due" }) }
                item { FilterPill("مسلط", filter == "mastered", onClick = { filter = "mastered" }) }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Bookmarks,
                    title = if (words.isEmpty()) "هنوز واژه‌ای ذخیره نکرده‌ای" else "چیزی پیدا نشد",
                    message = if (words.isEmpty()) {
                        "در متن درس روی هر واژه بزن تا معنی‌اش را ببینی و به دفترچه اضافه کنی."
                    } else {
                        "فیلتر یا عبارت جست‌وجو را تغییر بده."
                    },
                )
            }
        }

        items(filtered, key = { it.id }) { word ->
            WordRow(word = word, onClick = { selected = word })
        }
    }

    selected?.let { word ->
        WordSheet(
            token = word.term,
            meaning = WordMeaning(
                term = word.term,
                fa = word.meaning,
                pos = word.partOfSpeech,
                ipa = word.ipa,
                en = word.definition,
                example = word.example,
                source = word.episodeTitle ?: "",
            ),
            saved = true,
            onDismiss = { selected = null },
            onToggleSave = {
                vm.deleteWord(word)
                selected = null
            },
        )
    }
}

@Composable
private fun WordRow(word: SavedWordEntity, onClick: () -> Unit) {
    val palette = espresso
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = word.term,
                    fontFamily = Fraunces,
                    fontSize = 18.sp,
                    color = palette.textPrimary,
                    style = TextStyle(textDirection = TextDirection.Ltr),
                )
                if (word.ipa.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = word.ipa,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textTertiary,
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = word.meaning,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = palette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!word.episodeTitle.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = word.episodeTitle ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        BoxDots(box = word.box, mastered = word.mastered)
    }
}

@Composable
private fun BoxDots(box: Int, mastered: Boolean) {
    val palette = espresso
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(Srs.intervalsDays.size) { index ->
            val filled = index <= box
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            mastered && filled -> palette.mint
                            filled -> palette.accent
                            else -> palette.outline
                        },
                    ),
            )
        }
    }
}
