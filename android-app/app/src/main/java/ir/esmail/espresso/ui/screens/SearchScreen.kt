package ir.esmail.espresso.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.esmail.espresso.data.SearchHit
import ir.esmail.espresso.ui.AppViewModel
import ir.esmail.espresso.ui.Fa
import ir.esmail.espresso.ui.components.FilterPill
import ir.esmail.espresso.ui.components.SoftIconButton
import ir.esmail.espresso.ui.components.rememberSpeaker
import ir.esmail.espresso.ui.theme.espresso

/** Global search over every word and phrase in the course, with deep links to the lesson. */
@Composable
fun SearchScreen(
    vm: AppViewModel,
    bottomInset: Dp,
    onClose: () -> Unit,
    onOpen: (episodeId: String, segmentId: String) -> Unit,
) {
    val palette = espresso
    val speaker = rememberSpeaker()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<SearchHit.Kind?>(null) }
    var ready by remember { mutableStateOf(false) }
    var counts by remember { mutableStateOf(0 to 0) }

    LaunchedEffect(Unit) {
        vm.search.ensureIndex()
        counts = vm.search.countByKind()
        ready = true
    }

    val results = remember(query, filter, ready) {
        if (ready && query.trim().length >= 2) vm.search.query(query, filter) else emptyList()
    }

    Column(
        Modifier.fillMaxSize().background(palette.background).statusBarsPadding().padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SoftIconButton(Icons.Rounded.Close, "بستن", onClose, background = Color.Transparent)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("جستجوی سراسری", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
                Text(
                    "${Fa.num(counts.first)} واژه · ${Fa.num(counts.second)} عبارت",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textTertiary,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        SearchField(value = query, onValueChange = { query = it }, placeholder = "واژه یا معنی را جستجو کن…")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterPill("همه", filter == null, onClick = { filter = null })
            FilterPill("واژهها", filter == SearchHit.Kind.WORD, onClick = { filter = SearchHit.Kind.WORD })
            FilterPill("عبارتها", filter == SearchHit.Kind.PHRASE, onClick = { filter = SearchHit.Kind.PHRASE })
        }
        Spacer(Modifier.height(12.dp))

        when {
            !ready -> {
                Text("در حال ساخت شاخص واژهها…", style = MaterialTheme.typography.bodyMedium, color = palette.textTertiary)
            }
            query.trim().length < 2 -> {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.textTertiary, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "در تمام درسها جستجو کن",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textTertiary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            results.isEmpty() -> {
                Text("نتیجهای پیدا نشد.", style = MaterialTheme.typography.bodyMedium, color = palette.textTertiary)
            }
            else -> {
                Text("${Fa.num(results.size)} نتیجه", style = MaterialTheme.typography.labelMedium, color = palette.textTertiary)
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = bottomInset + 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.key }) { hit ->
                        SearchResultRow(
                            hit = hit,
                            canSpeak = speaker.available,
                            onSpeak = { speaker.say(hit.english) },
                            onClick = { onOpen(hit.episodeId, hit.segmentId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    hit: SearchHit,
    canSpeak: Boolean,
    onSpeak: () -> Unit,
    onClick: () -> Unit,
) {
    val palette = espresso
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(palette.surface).border(1.dp, palette.outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (hit.kind == SearchHit.Kind.WORD) Icons.Rounded.BookmarkBorder else Icons.Rounded.Search,
            contentDescription = null,
            tint = if (hit.kind == SearchHit.Kind.WORD) palette.accent else palette.mint,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(hit.english, style = MaterialTheme.typography.titleSmall.copy(textDirection = TextDirection.Ltr), color = palette.textPrimary)
            Text(hit.persian, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            if (hit.detail.isNotBlank()) {
                Text(hit.detail, style = MaterialTheme.typography.labelSmall, color = palette.textTertiary, maxLines = 1)
            }
            Text(hit.episodeTitle, style = MaterialTheme.typography.labelSmall, color = palette.accent)
        }
        if (canSpeak) {
            SoftIconButton(Icons.Rounded.VolumeUp, "پخش", onSpeak, background = palette.accentSoft, tint = palette.accent)
        }
    }
}
