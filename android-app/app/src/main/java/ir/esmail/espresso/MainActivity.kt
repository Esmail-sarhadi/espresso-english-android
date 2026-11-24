package ir.esmail.espresso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.esmail.espresso.data.ThemeMode
import ir.esmail.espresso.ui.AppRoot
import ir.esmail.espresso.ui.AppViewModel
import ir.esmail.espresso.ui.AppViewModelFactory
import ir.esmail.espresso.ui.theme.EspressoTheme
import ir.esmail.espresso.ui.theme.espresso

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels {
        AppViewModelFactory((application as EspressoApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val dark = when (settings.theme) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            val view = LocalView.current
            LaunchedEffect(dark) {
                val window = (view.context as? ComponentActivity)?.window ?: return@LaunchedEffect
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }

            EspressoTheme(mode = settings.theme, fontScale = settings.fontScale) {
                Surface(color = espresso.background) {
                    AppRoot(viewModel)
                }
            }
        }
    }
}
