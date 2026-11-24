package ir.esmail.espresso.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.esmail.espresso.ui.theme.espresso
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private data class TourPage(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val chips: List<String>,
)

private val PAGES = listOf(
    TourPage(
        icon = Icons.Rounded.Headphones,
        title = "به Espresso English خوش آمدی",
        body = "۲۰۸ درس واقعی از پادکست Espresso English — با متن هم‌زمان، ترجمهٔ فارسی، واژه‌نامه و آزمون.",
        chips = listOf("۲۰۸ درس", "متن همگام", "ترجمهٔ فارسی"),
    ),
    TourPage(
        icon = Icons.Rounded.GraphicEq,
        title = "گوش بده، چشم هم دنبال کن",
        body = "متن درس با صدا جلو می‌رود و خطِ در حال پخش روشن می‌شود. هر خط ترجمهٔ فارسی دارد و سرعت پخش را خودت تعیین می‌کنی.",
        chips = listOf("متن زنده", "سرعت دلخواه", "حالت شب"),
    ),
    TourPage(
        icon = Icons.Rounded.Translate,
        title = "ترانه و صفحه، هر جا که باشی",
        body = "آهنگ در حال پخش را تشخیص می‌دهد و ترانه را همراه با موسیقی جلو می‌برد. با «حباب شناور» هر متن انگلیسی روی صفحهٔ گوشی را بدون بیرون آمدن از برنامه ترجمه کن.",
        chips = listOf("تشخیص آهنگ", "حباب شناور", "ترجمهٔ صفحه"),
    ),
    TourPage(
        icon = Icons.Rounded.Insights,
        title = "مرور هوشمند، پیشرفت واقعی",
        body = "واژه‌ها با تکرار فاصله‌دار سر جای خودشان برمی‌گردند، تلفظت نمره می‌گیرد و آمار دقیق می‌گوید کجا ایستاده‌ای.",
        chips = listOf("مرور فاصله‌دار", "تمرین تلفظ", "آمار روزانه"),
    ),
)

/**
 * First-run walkthrough. Four pages, swipeable, with a slow aurora behind the artwork so the
 * entry screen feels like part of the product rather than a legal notice.
 */
@Composable
fun OnboardingTour(onFinish: () -> Unit) {
    val palette = espresso
    val pager = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == PAGES.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.background, palette.backgroundAlt))),
    ) {
        AuroraBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = "رد کردن",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textTertiary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onFinish)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f),
            ) { page ->
                TourPageView(PAGES[page])
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                PAGES.indices.forEach { index ->
                    val active = index == pager.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(if (active) 22.dp else 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (active) palette.accent else palette.outline),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pager.currentPage > 0) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(palette.surfaceHigh)
                            .clickable { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "قبلی", tint = palette.textSecondary)
                    }
                    Spacer(Modifier.width(12.dp))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.horizontalGradient(palette.warmGradient))
                        .clickable {
                            if (last) onFinish()
                            else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (last) "شروع یادگیری" else "بعدی",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.accentInk,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TourPageView(page: TourPage) {
    val palette = espresso
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(palette.accentSoft, Color.Transparent),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(palette.auroraGradient)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = palette.background,
                    modifier = Modifier.size(46.dp),
                )
            }
        }

        Spacer(Modifier.height(38.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            color = palette.textPrimary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            page.chips.forEach { chip ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.surface)
                        .border(1.dp, palette.outline, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(chip, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                }
            }
        }
    }
}

/** Slow, low-contrast colour wash so the background is never a flat rectangle. */
@Composable
private fun AuroraBackdrop() {
    val palette = espresso
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Canvas(Modifier.fillMaxSize()) {
        val drift = sin(phase * 2 * PI).toFloat()
        drawCircle(
            brush = Brush.radialGradient(
                listOf(palette.lilac.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(size.width * (0.2f + 0.05f * drift), size.height * 0.18f),
                radius = size.width * 0.7f,
            ),
            radius = size.width * 0.7f,
            center = Offset(size.width * (0.2f + 0.05f * drift), size.height * 0.18f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(palette.mint.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(size.width * (0.85f - 0.05f * drift), size.height * 0.55f),
                radius = size.width * 0.6f,
            ),
            radius = size.width * 0.6f,
            center = Offset(size.width * (0.85f - 0.05f * drift), size.height * 0.55f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(palette.accent.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.92f),
                radius = size.width * 0.8f,
            ),
            radius = size.width * 0.8f,
            center = Offset(size.width * 0.5f, size.height * 0.92f),
        )
    }
}
