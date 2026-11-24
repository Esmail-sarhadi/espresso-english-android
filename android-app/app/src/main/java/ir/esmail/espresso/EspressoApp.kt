package ir.esmail.espresso

import android.app.Application
import android.content.Context
import ir.esmail.espresso.data.AppDatabase
import ir.esmail.espresso.data.ContentRepository
import ir.esmail.espresso.data.LyricsRepository
import ir.esmail.espresso.data.NowPlayingDetector
import ir.esmail.espresso.data.OnlineTranslator
import ir.esmail.espresso.data.ScreenTextRecognizer
import ir.esmail.espresso.data.SearchRepository
import ir.esmail.espresso.data.UserRepository
import ir.esmail.espresso.playback.BundledTts
import ir.esmail.espresso.playback.PlayerConnection
import ir.esmail.espresso.playback.SoundManager
import ir.esmail.espresso.playback.SystemTts
import ir.esmail.espresso.playback.TtsCache

/** Manual dependency container – small app, no need for a DI framework. */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val content: ContentRepository by lazy { ContentRepository(appContext) }

    val users: UserRepository by lazy { UserRepository(database.dao()) }

    private val bundledTts: BundledTts by lazy { BundledTts(appContext) }

    /** Natural device voice; used when the user opts in and the device has an English voice. */
    val systemTts: SystemTts by lazy { SystemTts(appContext) }

    @Volatile
    var useNaturalVoice: Boolean = true

    val ttsCache: TtsCache by lazy {
        TtsCache(appContext, bundledTts, systemTts, preferSystemVoice = { useNaturalVoice })
    }

    val player: PlayerConnection by lazy { PlayerConnection(appContext, ttsCache) }

    val sounds: SoundManager by lazy { SoundManager(appContext) }

    val translator: OnlineTranslator by lazy { OnlineTranslator() }

    val search: SearchRepository by lazy { SearchRepository(content) }

    val lyrics: LyricsRepository by lazy { LyricsRepository() }

    val screenText: ScreenTextRecognizer by lazy { ScreenTextRecognizer() }

    val nowPlaying: NowPlayingDetector by lazy { NowPlayingDetector(appContext) }
}

class EspressoApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
