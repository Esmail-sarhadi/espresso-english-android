package ir.esmail.espresso.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.esmail.espresso.data.ContentBundle
import ir.esmail.espresso.data.PodcastRef
import ir.esmail.espresso.ui.Fa
import ir.esmail.espresso.ui.components.LevelBadge
import ir.esmail.espresso.ui.components.ltrStart
import ir.esmail.espresso.ui.theme.CoverPalette
import ir.esmail.espresso.ui.theme.espresso

@Composable
fun PodcastsScreen(
    bundle: ContentBundle,
    bottomInset: Dp,
) {
    val palette = espresso
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(top = 18.dp, bottom = bottomInset + 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                Text(
                    text = "پادکست‌ها",
                    style = MaterialTheme.typography.headlineLarge,
                    color = palette.textPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "این درس‌ها با متن همگام و آزمون داخل برنامه هستند. بقیهٔ این پادکست‌ها را " +
                        "می‌توانی از سایت رسمی‌شان گوش کنی؛ همه معتبر و مناسب یادگیری‌اند.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textTertiary,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(palette.accentSoft, palette.surface)))
                        .border(1.dp, palette.accent.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = Fa.num(bundle.episodes.size),
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.accent,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "درس آمادهٔ پخش در همین برنامه، با زیرنویس واژه‌به‌واژه و ترجمهٔ فارسی",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                }
            }
        }

        items(bundle.podcasts, key = { it.name }) { podcast ->
            PodcastCard(podcast = podcast, onOpen = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(podcast.url)))
                }
            })
        }
    }
}

@Composable
private fun PodcastCard(podcast: PodcastRef, onOpen: () -> Unit) {
    val palette = espresso
    val accent = CoverPalette[podcast.name.hashCode().let { if (it < 0) -it else it } % CoverPalette.size]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(24.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = podcast.name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = accent,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = podcast.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold).ltrStart(),
                    color = palette.textPrimary,
                )
                Text(
                    text = podcast.host,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textTertiary,
                )
            }
            if (podcast.badge.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.mint.copy(alpha = 0.16f))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = podcast.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.mint,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = podcast.descriptionFa,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
        )

        if (podcast.whyFa.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surfaceHigh)
                    .padding(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(34.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = podcast.whyFa,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (podcast.level.isNotBlank()) LevelBadge(podcast.level)
            Spacer(Modifier.width(6.dp))
            if (podcast.accent.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.surfaceHigh)
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = podcast.accent,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = 0.14f))
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "سایت پادکست",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.accent,
                )
                Spacer(Modifier.width(5.dp))
                Icon(
                    imageVector = Icons.Rounded.OpenInNew,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
