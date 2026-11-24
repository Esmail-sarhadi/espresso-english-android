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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.esmail.espresso.data.ContentBundle
import ir.esmail.espresso.data.Episode
import ir.esmail.espresso.data.ProgressEntity
import ir.esmail.espresso.ui.AppViewModel
import ir.esmail.espresso.ui.Fa
import ir.esmail.espresso.ui.components.EpisodeRow
import ir.esmail.espresso.ui.components.FilterPill
import ir.esmail.espresso.ui.theme.espresso

@Composable
fun LibraryScreen(
    vm: AppViewModel,
    bundle: ContentBundle,
    bottomInset: Dp,
    onOpenEpisode: (Episode) -> Unit,
) {
    val palette = espresso
    val progress by vm.progress.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable { mutableStateOf("all") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    val favoriteIds by vm.favorites.collectAsStateWithLifecycle()
    val favoriteSet = remember(favoriteIds) { favoriteIds.map { it.episodeId }.toSet() }

    val filtered = remember(bundle, query, level, favoritesOnly, favoriteSet) {
        bundle.episodes.filter { episode ->
            val matchesLevel = level == "all" || episode.level == level
            val matchesFavorite = !favoritesOnly || episode.id in favoriteSet
            val matchesQuery = query.isBlank() || listOf(
                episode.title,
                episode.titleFa,
                episode.subtitle,
                episode.category,
                episode.tags.joinToString(" "),
            ).any { it.contains(query, ignoreCase = true) }
            matchesLevel && matchesQuery && matchesFavorite
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(top = 18.dp, bottom = bottomInset + 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                Text(
                    text = "کتابخانه",
                    style = MaterialTheme.typography.headlineLarge,
                    color = palette.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${Fa.num(bundle.episodes.size)} درس با متن همگام، ترجمه و آزمون",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textTertiary,
                )
                Spacer(Modifier.height(16.dp))
                SearchField(value = query, onValueChange = { query = it })
                Spacer(Modifier.height(12.dp))
            }
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { FilterPill("همه", level == "all", onClick = { level = "all" }) }
                item { FilterPill("مقدماتی", level == "beginner", onClick = { level = "beginner" }) }
                item { FilterPill("متوسط", level == "intermediate", onClick = { level = "intermediate" }) }
                item { FilterPill("پیشرفته", level == "advanced", onClick = { level = "advanced" }) }
                item { FilterPill("علاقهمندیها", favoritesOnly, onClick = { favoritesOnly = !favoritesOnly }) }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (filtered.isEmpty()) {
            item {
                Text(
                    text = "درسی با این مشخصات پیدا نشد.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textTertiary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 32.dp),
                )
            }
        }

        itemsIndexed(filtered, key = { _, episode -> episode.id }) { _, episode ->
            EpisodeRow(
                episode = episode,
                index = bundle.episodes.indexOf(episode),
                progress = progress[episode.id],
                onClick = { onOpenEpisode(episode) },
                modifier = Modifier.padding(horizontal = 14.dp),
            )
        }
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "جست‌وجوی درس، واژه یا موضوع…",
) {
    val palette = espresso
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = palette.textTertiary,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(palette.surfaceHigh)
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "پاک کردن",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

@Suppress("unused")
private fun progressOf(map: Map<String, ProgressEntity>, id: String) = map[id]
