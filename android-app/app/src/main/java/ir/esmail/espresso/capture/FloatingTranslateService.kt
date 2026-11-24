package ir.esmail.espresso.capture

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ServiceCompat
import ir.esmail.espresso.MainActivity
import ir.esmail.espresso.R
import ir.esmail.espresso.data.OnlineTranslator
import ir.esmail.espresso.data.ScreenTextRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * A floating bubble that lives on top of every app. Tapping it grabs one frame from the live
 * MediaProjection mirror, reads its text on-device and shows the translation in an overlay panel
 * – without ever leaving whatever the user was reading.
 *
 * The projection is created once and kept alive, so the user is not asked for consent on every
 * tap (Android 14 allows a single virtual display per projection).
 */
class FloatingTranslateService : Service() {

    private companion object {
        const val TAG = "FloatingTranslate"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"
        const val ACTION_STOP = "ir.esmail.espresso.STOP_BUBBLE"
        const val CHANNEL_ID = "espresso_floating_translate"
        const val NOTIFICATION_ID = 2211

        const val INK = 0xFFF6EFE7.toInt()
        const val INK_SOFT = 0xFFB4A79C.toInt()
        const val SURFACE = 0xFF171312.toInt()
        const val SURFACE_HIGH = 0xFF221C1A.toInt()
        const val OUTLINE = 0xFF2E2622.toInt()
        const val ACCENT = 0xFFF2B04B.toInt()
        const val ACCENT_DEEP = 0xFFD98A28.toInt()
        const val ACCENT_INK = 0xFF231605.toInt()
        const val DANGER = 0xFFF07A6A.toInt()
    }

    private lateinit var windowManager: WindowManager
    /**
     * Overlay windows must be added through a *visual* context. A Service context is not one, so
     * we derive a display context first and build the window context from it. Falling back to the
     * plain service WindowManager keeps older devices working.
     */
    private val overlayWindowManager: WindowManager by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val displayManager = getSystemService(android.hardware.display.DisplayManager::class.java)
                val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)
                val displayContext = createDisplayContext(display)
                displayContext
                    .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
                    .getSystemService(WindowManager::class.java)
            }.getOrElse { windowManager }
        } else {
            windowManager
        }
    }
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val recognizer = ScreenTextRecognizer()
    private val translator = OnlineTranslator()

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var bubbleSize = 0
    private var dismissView: View? = null
    private var dismissParams: WindowManager.LayoutParams? = null
    private var dismissArmed = false
    private var panelView: View? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var panelBody: LinearLayout? = null
    private var panelStatus: TextView? = null

    private var busy = false
    private var screenWidth = 1080
    private var screenHeight = 2400

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Always satisfy the foreground obligation before anything else can fail.
        if (!promoteToForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            toast("اجازهٔ نمایش روی برنامه‌های دیگر داده نشده است.")
            stopSelf()
            return START_NOT_STICKY
        }

        if (bubbleView == null) {
            if (!startMirroring(intent)) {
                stopSelf()
                return START_NOT_STICKY
            }
            showBubble()
        }
        return START_NOT_STICKY
    }

    private fun promoteToForeground(): Boolean = runCatching {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "حباب ترجمه", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, FloatingTranslateService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        val notification = builder
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("حباب ترجمه فعال است")
            .setContentText("روی حباب بزن تا متن صفحه ترجمه شود")
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "بستن حباب", stop).build())
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }
    }.onFailure { Log.e(TAG, "foreground failed", it) }.isSuccess

    // ------------------------------------------------------------- projection

    private fun startMirroring(intent: Intent?): Boolean {
        if (intent == null) return false
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE)
        val data = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_DATA)
            }
        }.getOrNull()
        if (resultCode != Activity.RESULT_OK || data == null) {
            toast("اجازهٔ ضبط صفحه داده نشد.")
            return false
        }

        return runCatching {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val active = manager.getMediaProjection(resultCode, data)
                ?: throw IllegalStateException("پروجکشن باز نشد")
            projection = active
            active.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    handler.post { stopSelf() }
                }
            }, handler)

            val metrics = resources.displayMetrics
            screenWidth = metrics.widthPixels and 0x7FFFFFFE
            screenHeight = metrics.heightPixels and 0x7FFFFFFE

            val reader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
            imageReader = reader
            virtualDisplay = active.createVirtualDisplay(
                "EspressoBubble",
                screenWidth,
                screenHeight,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler,
            )
            virtualDisplay != null
        }.onFailure {
            Log.e(TAG, "mirroring failed", it)
            toast("آینه‌کردن صفحه ممکن نشد.")
        }.getOrDefault(false)
    }

    // ----------------------------------------------------------------- bubble

    private fun showBubble() {
        if (bubbleView != null) return
        val size = dp(58)
        val bubble = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(ACCENT, ACCENT_DEEP),
            ).apply { shape = GradientDrawable.OVAL }
        }
        val icon = ImageView(this).apply {
            setImageResource(R.drawable.ic_bubble_translate)
            setColorFilter(ACCENT_INK)
            layoutParams = FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER)
        }
        bubble.addView(icon)

        val params = WindowManager.LayoutParams(
            size,
            size,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - size - dp(12)
            y = resources.displayMetrics.heightPixels / 2
        }

        bubble.setOnTouchListener(DragTapListener(params) { onBubbleTapped() })
        runCatching { overlayWindowManager.addView(bubble, params) }
            .onFailure { Log.e(TAG, "bubble add failed", it) }
        bubbleView = bubble
        bubbleParams = params
        bubbleSize = size
    }

    /**
     * Distinguishes a drag from a tap, and turns a drag into a "throw away" gesture: as soon as
     * the bubble starts moving, a delete target fades in at the bottom of the screen; dropping
     * the bubble on it removes the bubble for good.
     */
    private inner class DragTapListener(
        private val params: WindowManager.LayoutParams,
        private val onTap: () -> Unit,
    ) : View.OnTouchListener {
        private var startX = 0
        private var startY = 0
        private var touchX = 0f
        private var touchY = 0f
        private var moved = false

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (!moved && (abs(dx) > dp(6) || abs(dy) > dp(6))) {
                        moved = true
                        showDismissTarget()
                        view.animate().scaleX(1.06f).scaleY(1.06f).setDuration(120).start()
                    }
                    if (moved) {
                        params.x = (startX + dx).coerceIn(0, maxOf(0, screenWidth - bubbleSize))
                        params.y = (startY + dy).coerceIn(0, maxOf(0, screenHeight - bubbleSize))
                        runCatching { overlayWindowManager.updateViewLayout(view, params) }
                        setDismissArmed(isOverDismissTarget(params))
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val wasArmed = dismissArmed
                    hideDismissTarget()
                    view.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    when {
                        moved && wasArmed -> stopSelf()
                        !moved -> {
                            view.animate().scaleX(0.88f).scaleY(0.88f).setDuration(70).withEndAction {
                                view.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                            }.start()
                            onTap()
                        }
                    }
                }
            }
            return true
        }
    }

    // --------------------------------------------------------- delete target

    /** Fades in the round delete target at the bottom centre of the screen. */
    private fun showDismissTarget() {
        if (dismissView != null) return
        val size = dp(64)
        val target = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xCC1A1622.toInt())
                setStroke(dp(2), DANGER)
            }
            alpha = 0f
        }
        val cross = TextView(this).apply {
            text = "✕"
            setTextColor(DANGER)
            textSize = 24f
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }
        target.addView(cross)

        val params = WindowManager.LayoutParams(
            size,
            size,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(56)
        }

        runCatching { overlayWindowManager.addView(target, params) }
            .onFailure { Log.e(TAG, "dismiss target add failed", it); return }
        dismissView = target
        dismissParams = params
        dismissArmed = false
        target.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start()
    }

    private fun hideDismissTarget() {
        val target = dismissView ?: return
        dismissArmed = false
        target.animate().alpha(0f).setDuration(120).withEndAction {
            runCatching { overlayWindowManager.removeView(target) }
        }.start()
        dismissView = null
        dismissParams = null
    }

    /** True when the bubble's centre is inside the delete target's hit area. */
    private fun isOverDismissTarget(params: WindowManager.LayoutParams): Boolean {
        val target = dismissView ?: return false
        val targetSize = target.width.takeIf { it > 0 } ?: dp(64)
        val targetCenterX = screenWidth / 2f
        val targetCenterY = screenHeight - dp(56) - targetSize / 2f
        val bubbleCenterX = params.x + bubbleSize / 2f
        val bubbleCenterY = params.y + bubbleSize / 2f
        val reach = dp(52).toFloat()
        return abs(bubbleCenterX - targetCenterX) < reach && abs(bubbleCenterY - targetCenterY) < reach
    }

    private fun setDismissArmed(armed: Boolean) {
        if (armed == dismissArmed) return
        dismissArmed = armed
        val target = dismissView ?: return
        val params = dismissParams
        target.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (armed) 0xE6FF6B5E.toInt() else 0xCC1A1622.toInt())
            setStroke(dp(2), DANGER)
        }
        target.animate().scaleX(if (armed) 1.18f else 1f).scaleY(if (armed) 1.18f else 1f)
            .setDuration(120).start()
        ((target as? FrameLayout)?.getChildAt(0) as? TextView)?.setTextColor(if (armed) 0xFF2A0E0B.toInt() else DANGER)
        params?.let { runCatching { overlayWindowManager.updateViewLayout(target, it) } }
        if (armed) target.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
    }

    // ------------------------------------------------------------------ scan

    private fun onBubbleTapped() {
        if (busy) return
        if (panelView == null) showPanel()
        setStatus("در حال خواندن صفحه…")
        panelBody?.removeAllViews()

        busy = true
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { grabFrame() }
            if (bitmap == null) {
                setStatus("تصویر صفحه گرفته نشد. دوباره تلاش کن.")
                busy = false
                return@launch
            }
            val lines = withContext(Dispatchers.IO) { recognizer.read(bitmap).getOrNull().orEmpty() }
            if (lines.isEmpty()) {
                setStatus("در این صفحه متنی پیدا نشد.")
                busy = false
                return@launch
            }
            setStatus("در حال ترجمه… ${lines.size} خط")
            val translated = withContext(Dispatchers.IO) {
                translator.translateLines(lines, "en", "fa").getOrNull().orEmpty()
            }
            renderLines(lines, translated)
            busy = false
        }
    }

    private fun grabFrame(): Bitmap? {
        val reader = imageReader ?: return null
        repeat(6) {
            val image = runCatching { reader.acquireLatestImage() }.getOrNull()
            if (image != null) {
                try {
                    return image.toBitmap(screenWidth, screenHeight)
                } catch (error: Throwable) {
                    Log.e(TAG, "frame conversion failed", error)
                } finally {
                    runCatching { image.close() }
                }
            }
            Thread.sleep(120)
        }
        return null
    }

    private fun android.media.Image.toBitmap(width: Int, height: Int): Bitmap {
        val plane = planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val pixels = IntArray(width * height)
        buffer.rewind()
        for (y in 0 until height) {
            var index = y * rowStride
            val rowOffset = y * width
            for (x in 0 until width) {
                if (index + 2 < buffer.capacity()) {
                    val red = buffer.get(index).toInt() and 0xFF
                    val green = buffer.get(index + 1).toInt() and 0xFF
                    val blue = buffer.get(index + 2).toInt() and 0xFF
                    pixels[rowOffset + x] = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
                }
                index += pixelStride
            }
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    // ----------------------------------------------------------------- panel

    private fun showPanel() {
        if (panelView != null) return
        val metrics = resources.displayMetrics
        val width = (metrics.widthPixels - dp(28)).coerceAtLeast(dp(240))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(SURFACE)
                setStroke(dp(1), OUTLINE)
            }
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "ترجمهٔ صفحه"
            setTextColor(INK)
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val rescan = TextView(this).apply {
            text = "دوباره"
            setTextColor(ACCENT)
            textSize = 13f
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { onBubbleTapped() }
        }
        val copy = TextView(this).apply {
            text = "کپی"
            setTextColor(ACCENT)
            textSize = 13f
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { copyToClipboard() }
        }
        val close = TextView(this).apply {
            text = "✕"
            setTextColor(INK_SOFT)
            textSize = 16f
            setPadding(dp(10), dp(6), dp(6), dp(6))
            setOnClickListener { hidePanel() }
        }
        header.addView(title)
        header.addView(rescan)
        header.addView(copy)
        header.addView(close)

        val status = TextView(this).apply {
            text = "آماده"
            setTextColor(INK_SOFT)
            textSize = 12f
            setPadding(0, dp(6), 0, dp(8))
        }

        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply {
            addView(body)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (metrics.heightPixels * 0.52f).toInt(),
            )
        }

        card.addView(header)
        card.addView(status)
        card.addView(scroll)

        val params = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(90)
        }

        runCatching { overlayWindowManager.addView(card, params) }
            .onFailure { Log.e(TAG, "panel add failed", it) }
        panelView = card
        panelParams = params
        panelBody = body
        panelStatus = status
    }

    private fun hidePanel() {
        panelView?.let { runCatching { overlayWindowManager.removeView(it) } }
        panelView = null
        panelBody = null
        panelStatus = null
    }

    private fun setStatus(text: String) {
        panelStatus?.text = text
    }

    private fun renderLines(lines: List<String>, translated: Map<Int, String>) {
        val body = panelBody ?: return
        body.removeAllViews()
        setStatus("${lines.size} خط ترجمه شد")
        lines.forEachIndexed { index, english ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(7), 0, dp(7))
            }
            val en = TextView(this).apply {
                text = english
                setTextColor(INK)
                textSize = 14f
                textDirection = View.TEXT_DIRECTION_LTR
                gravity = Gravity.START
            }
            row.addView(en)
            translated[index]?.let { fa ->
                val faView = TextView(this).apply {
                    text = fa
                    setTextColor(ACCENT)
                    textSize = 13f
                    textDirection = View.TEXT_DIRECTION_RTL
                    gravity = Gravity.END
                    setPadding(0, dp(3), 0, 0)
                }
                row.addView(faView)
            }
            body.addView(row)
            if (index != lines.lastIndex) {
                body.addView(View(this).apply {
                    setBackgroundColor(OUTLINE)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                })
            }
        }
        lastResult = lines.mapIndexed { index, en -> en to translated[index].orEmpty() }
    }

    private var lastResult: List<Pair<String, String>> = emptyList()

    private fun copyToClipboard() {
        if (lastResult.isEmpty()) return
        val text = lastResult.joinToString("\n") { (en, fa) ->
            if (fa.isBlank()) en else "$en\n$fa"
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ترجمه", text))
        toast("کپی شد")
    }

    // ----------------------------------------------------------------- utils

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        scope.cancel()
        hideDismissTarget()
        hidePanel()
        bubbleView?.let { runCatching { overlayWindowManager.removeView(it) } }
        bubbleView = null
        runCatching { virtualDisplay?.release() }
        runCatching { imageReader?.close() }
        runCatching { projection?.stop() }
        runCatching { recognizer.close() }
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        super.onDestroy()
    }
}
