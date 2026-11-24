package ir.esmail.espresso.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.esmail.espresso.ui.screens.OnboardingTour
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.esmail.espresso.R
import ir.esmail.espresso.data.ContentBundle
import ir.esmail.espresso.ui.components.CoverArt
import ir.esmail.espresso.ui.components.MiniPlayer
import ir.esmail.espresso.ui.screens.HomeScreen
import ir.esmail.espresso.ui.screens.LibraryScreen
import ir.esmail.espresso.ui.screens.NotebookScreen
import ir.esmail.espresso.ui.screens.PlayerScreen
import ir.esmail.espresso.ui.screens.PodcastsScreen
import ir.esmail.espresso.ui.screens.QuizScreen
import ir.esmail.espresso.ui.screens.ReviewScreen
import ir.esmail.espresso.ui.screens.ScreenScanScreen
import ir.esmail.espresso.ui.screens.SearchScreen
import ir.esmail.espresso.ui.screens.SpeakScreen
import ir.esmail.espresso.ui.screens.StatsScreen
import ir.esmail.espresso.ui.screens.SongLyricsScreen
import ir.esmail.espresso.ui.screens.SettingsScreen
import ir.esmail.espresso.ui.screens.TranslateScreen
import ir.esmail.espresso.ui.theme.espresso

val LocalBottomInset = staticCompositionLocalOf { 0.dp }

private const val ROUTE_HOME = "home"
private const val ROUTE_LIBRARY = "library"
private const val ROUTE_REVIEW = "review"
private const val ROUTE_NOTEBOOK = "notebook"
private const val ROUTE_PODCASTS = "podcasts"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_PLAYER = "player"
private const val ROUTE_QUIZ = "quiz"
private const val ROUTE_TRANSLATE = "translate"
private const val ROUTE_SONG = "song"
private const val ROUTE_SCAN = "scan"
private const val ROUTE_SEARCH = "search"
private const val ROUTE_SPEAK = "speak"
private const val ROUTE_STATS = "stats"

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(ROUTE_HOME, "خانه", Icons.Rounded.Home),
    Tab(ROUTE_LIBRARY, "کتابخانه", Icons.Rounded.LibraryBooks),
    Tab(ROUTE_REVIEW, "مرور", Icons.Rounded.Style),
    Tab(ROUTE_NOTEBOOK, "دفترچه", Icons.Rounded.Bookmarks),
    Tab(ROUTE_PODCASTS, "پادکست", Icons.Rounded.Podcasts),
)

@Composable
fun AppRoot(vm: AppViewModel) {
    val content by vm.content.collectAsStateWithLifecycle()
    when (val state = content) {
        is ContentState.Loading -> SplashScreen()
        is ContentState.Failed -> ErrorScreen(message = state.message, onRetry = { vm.loadContent() })
        is ContentState.Ready -> {
            val context = LocalContext.current
            var showTour by remember { mutableStateOf(!TourPrefs.seen(context)) }
            Box(Modifier.fillMaxSize()) {
                MainNavigation(vm = vm, bundle = state.bundle)
                AnimatedVisibility(
                    visible = showTour,
                    enter = fadeIn(tween(260)),
                    exit = fadeOut(tween(320)),
                ) {
                    OnboardingTour(
                        onFinish = {
                            TourPrefs.markSeen(context)
                            showTour = false
                        },
                    )
                }
            }
        }
    }
}

/** Remembers whether the first-run walkthrough has already been shown. */
object TourPrefs {
    private const val FILE = "espresso_onboarding"
    private const val KEY_SEEN = "seen"

    fun seen(context: android.content.Context): Boolean =
        context.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
            .getBoolean(KEY_SEEN, false)

    fun markSeen(context: android.content.Context) {
        context.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SEEN, true).apply()
    }

    fun reset(context: android.content.Context) {
        context.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SEEN, false).apply()
    }
}

@Composable
private fun SplashScreen() {
    val palette = espresso
    Box(
        modifier = Modifier.fillMaxSize().background(palette.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.brand_mark),
                contentDescription = null,
                modifier = Modifier.size(88.dp).clip(RoundedCornerShape(24.dp)),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Espresso English",
                style = MaterialTheme.typography.headlineSmall,
                color = palette.textPrimary,
            )
            Spacer(Modifier.height(20.dp))
            // A plain ring avoids the heavyweight Material progress animation on cold start.
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .border(2.dp, palette.accent, CircleShape),
            )
        }
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    val palette = espresso
    Box(
        modifier = Modifier.fillMaxSize().background(palette.background).statusBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "محتوای درس‌ها بارگذاری نشد",
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textTertiary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.accent)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "تلاش دوباره",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.accentInk,
                )
            }
        }
    }
}

@Composable
private fun MainNavigation(vm: AppViewModel, bundle: ContentBundle) {
    val palette = espresso
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route.orEmpty()
    val showChrome = TABS.any { it.route == route }

    val playerState by vm.player.state.collectAsStateWithLifecycle()
    val currentEpisode = bundle.episode(playerState.episodeId)

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        containerColor = palette.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showChrome) {
                Column(Modifier.background(palette.background)) {
                    if (currentEpisode != null) {
                        MiniPlayer(
                            state = playerState,
                            title = currentEpisode.titleFa.ifBlank { currentEpisode.title },
                            subtitle = currentEpisode.title,
                            cover = {
                                CoverArt(
                                    episode = currentEpisode,
                                    index = bundle.episodes.indexOf(currentEpisode).coerceAtLeast(0),
                                    modifier = Modifier.size(42.dp),
                                    corner = 12.dp,
                                    numberFraction = 0.34f,
                                    showArcs = false,
                                )
                            },
                            onOpen = { navController.navigate("$ROUTE_PLAYER/${currentEpisode.id}") },
                            onToggle = { vm.player.toggle() },
                            onClose = { vm.player.stopAndClear() },
                        )
                    }
                    BottomBar(
                        current = route,
                        onNavigate = { target -> navController.navigateTopLevel(target) },
                    )
                }
            }
        },
    ) { innerPadding ->
        CompositionLocalProvider(LocalBottomInset provides innerPadding.calculateBottomPadding()) {
            NavHost(
                navController = navController,
                startDestination = ROUTE_HOME,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(ROUTE_HOME, enterTransition = { fadeIn(tween(180)) }, exitTransition = { fadeOut(tween(120)) }) {
                    HomeScreen(
                        vm = vm,
                        bundle = bundle,
                        bottomInset = LocalBottomInset.current,
                        onOpenEpisode = { navController.navigate("$ROUTE_PLAYER/${it.id}") },
                        onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                        onOpenReview = { navController.navigateTopLevel(ROUTE_REVIEW) },
                        onOpenPodcasts = { navController.navigateTopLevel(ROUTE_PODCASTS) },
                        onOpenLibrary = { navController.navigateTopLevel(ROUTE_LIBRARY) },
                        onOpenTranslate = { navController.navigate(ROUTE_TRANSLATE) },
                        onOpenSongs = { navController.navigate(ROUTE_SONG) },
                        onOpenScan = { navController.navigate(ROUTE_SCAN) },
                        onOpenSearch = { navController.navigate(ROUTE_SEARCH) },
                        onOpenSpeak = { navController.navigate(ROUTE_SPEAK) },
                        onOpenStats = { navController.navigate(ROUTE_STATS) },
                    )
                }

                composable(ROUTE_LIBRARY, enterTransition = { fadeIn(tween(180)) }, exitTransition = { fadeOut(tween(120)) }) {
                    LibraryScreen(
                        vm = vm,
                        bundle = bundle,
                        bottomInset = LocalBottomInset.current,
                        onOpenEpisode = { navController.navigate("$ROUTE_PLAYER/${it.id}") },
                    )
                }

                composable(ROUTE_REVIEW, enterTransition = { fadeIn(tween(180)) }, exitTransition = { fadeOut(tween(120)) }) {
                    ReviewScreen(
                        vm = vm,
                        bottomInset = LocalBottomInset.current,
                        onGoToNotebook = { navController.navigateTopLevel(ROUTE_NOTEBOOK) },
                    )
                }

                composable(ROUTE_NOTEBOOK, enterTransition = { fadeIn(tween(180)) }, exitTransition = { fadeOut(tween(120)) }) {
                    NotebookScreen(
                        vm = vm,
                        bundle = bundle,
                        bottomInset = LocalBottomInset.current,
                        onReview = { navController.navigateTopLevel(ROUTE_REVIEW) },
                    )
                }

                composable(ROUTE_PODCASTS, enterTransition = { fadeIn(tween(180)) }, exitTransition = { fadeOut(tween(120)) }) {
                    PodcastsScreen(bundle = bundle, bottomInset = LocalBottomInset.current)
                }

                composable(ROUTE_SETTINGS, enterTransition = { fadeIn(tween(180)) }, exitTransition = { fadeOut(tween(120)) }) {
                    SettingsScreen(vm = vm, bottomInset = LocalBottomInset.current)
                }

                composable(
                    route = ROUTE_TRANSLATE,
                    enterTransition = { slideInVertically(tween(260)) { it } },
                    exitTransition = { fadeOut(tween(140)) },
                    popEnterTransition = { fadeIn(tween(140)) },
                    popExitTransition = { slideOutVertically(tween(220)) { it } },
                ) {
                    TranslateScreen(
                        vm = vm,
                        bottomInset = LocalBottomInset.current,
                        onClose = { navController.popBackStack() },
                    )
                }

                composable(
                    route = ROUTE_SEARCH,
                    enterTransition = { slideInVertically(tween(260)) { it } },
                    exitTransition = { fadeOut(tween(140)) },
                    popEnterTransition = { fadeIn(tween(140)) },
                    popExitTransition = { slideOutVertically(tween(220)) { it } },
                ) {
                    SearchScreen(
                        vm = vm,
                        bottomInset = LocalBottomInset.current,
                        onClose = { navController.popBackStack() },
                        onOpen = { episodeId, segmentId ->
                            navController.navigate("$ROUTE_PLAYER/$episodeId?segment=${java.net.URLEncoder.encode(segmentId, "UTF-8")}")
                        },
                    )
                }

                composable(
                    route = ROUTE_SPEAK,
                    enterTransition = { slideInVertically(tween(260)) { it } },
                    exitTransition = { fadeOut(tween(140)) },
                    popEnterTransition = { fadeIn(tween(140)) },
                    popExitTransition = { slideOutVertically(tween(220)) { it } },
                ) {
                    SpeakScreen(
                        vm = vm,
                        bundle = bundle,
                        bottomInset = LocalBottomInset.current,
                        onClose = { navController.popBackStack() },
                    )
                }

                composable(
                    route = ROUTE_STATS,
                    enterTransition = { slideInVertically(tween(260)) { it } },
                    exitTransition = { fadeOut(tween(140)) },
                    popEnterTransition = { fadeIn(tween(140)) },
                    popExitTransition = { slideOutVertically(tween(220)) { it } },
                ) {
                    StatsScreen(
                        vm = vm,
                        bundle = bundle,
                        bottomInset = LocalBottomInset.current,
                        onClose = { navController.popBackStack() },
                    )
                }

                composable(
                    route = ROUTE_SONG,
                    enterTransition = { slideInVertically(tween(260)) { it } },
                    exitTransition = { fadeOut(tween(140)) },
                    popEnterTransition = { fadeIn(tween(140)) },
                    popExitTransition = { slideOutVertically(tween(220)) { it } },
                ) {
                    SongLyricsScreen(
                        vm = vm,
                        bottomInset = LocalBottomInset.current,
                        onClose = { navController.popBackStack() },
                    )
                }

                composable(
                    route = ROUTE_SCAN,
                    enterTransition = { slideInVertically(tween(260)) { it } },
                    exitTransition = { fadeOut(tween(140)) },
                    popEnterTransition = { fadeIn(tween(140)) },
                    popExitTransition = { slideOutVertically(tween(220)) { it } },
                ) {
                    ScreenScanScreen(
                        vm = vm,
                        bottomInset = LocalBottomInset.current,
                        onClose = { navController.popBackStack() },
                    )
                }

                composable(
                    route = "$ROUTE_PLAYER/{id}?segment={segment}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.StringType },
                        navArgument("segment") { type = NavType.StringType; nullable = true; defaultValue = null },
                    ),
                    enterTransition = { slideInVertically(tween(280)) { it } },
                    exitTransition = { slideOutVertically(tween(240)) { it } },
                    popEnterTransition = { slideInVertically(tween(240)) { it / 4 } },
                    popExitTransition = { slideOutVertically(tween(240)) { it } },
                ) { entry ->
                    val episode = bundle.episode(entry.arguments?.getString("id"))
                    if (episode == null) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        PlayerScreen(
                            vm = vm,
                            episode = episode,
                            bundle = bundle,
                            targetSegmentId = entry.arguments?.getString("segment"),
                            onClose = { navController.popBackStack() },
                            onOpenQuiz = { navController.navigate("$ROUTE_QUIZ/${episode.id}") },
                        )
                    }
                }

                composable(
                    route = "$ROUTE_QUIZ/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                    enterTransition = { slideInVertically(tween(260)) { it } },
                    exitTransition = { fadeOut(tween(160)) },
                    popEnterTransition = { fadeIn(tween(160)) },
                    popExitTransition = { slideOutVertically(tween(240)) { it } },
                ) { entry ->
                    val episode = bundle.episode(entry.arguments?.getString("id"))
                    if (episode == null) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        QuizScreen(
                            vm = vm,
                            episode = episode,
                            bundle = bundle,
                            onClose = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(current: String, onNavigate: (String) -> Unit) {
    val palette = espresso
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface)
            .navigationBarsPadding()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TABS.forEach { tab ->
            val active = current == tab.route
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onNavigate(tab.route) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) palette.accentSoft else androidx.compose.ui.graphics.Color.Transparent)
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (active) palette.accent else palette.textTertiary,
                        modifier = Modifier.size(21.dp),
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) palette.accent else palette.textTertiary,
                    maxLines = 1,
                )
            }
        }
    }
}
