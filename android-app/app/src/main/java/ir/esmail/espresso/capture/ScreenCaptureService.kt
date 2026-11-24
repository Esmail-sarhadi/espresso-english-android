package ir.esmail.espresso.capture

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.core.app.ServiceCompat
import ir.esmail.espresso.R

/**
 * One-shot foreground screen capture. Android 14+ requires MediaProjection to run from a
 * foreground service of type mediaProjection; this service grabs a single usable frame,
 * publishes it through [ScreenCaptureBus] and stops immediately.
 *
 * Every entry point is guarded: a device that refuses MediaProjection must show a readable
 * message in the UI, never crash the app. MIUI/HyperOS in particular can hand back a
 * projection that only ever produces uniform (black) frames, so the reader keeps polling
 * until a frame with real content arrives or the deadline passes.
 */
class ScreenCaptureService : Service() {

    private companion object {
        const val TAG = "ScreenCapture"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"
        const val CHANNEL_ID = "espresso_screen_scan"
        const val NOTIFICATION_ID = 2207
        const val DEADLINE_MS = 9_000L
        const val MAX_BLANK_FRAMES = 40
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var finished = false
    private var blankFrames = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1) Satisfy the foreground obligation FIRST. Android kills the whole process with
        //    ForegroundServiceDidNotStartInTimeException if startForeground() is not reached
        //    within ~5 s of startForegroundService(), no matter what else goes wrong later.
        if (!promoteToForeground()) {
            failSafe("اجازهٔ سرویس ضبط صفحه گرفته نشد.")
            return START_NOT_STICKY
        }

        if (intent == null) {
            failSafe("ضبط صفحه آغاز نشد.")
            return START_NOT_STICKY
        }

        // Activity.RESULT_OK is -1, so it can never be used as the "missing extra" default.
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE)
        val data = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_DATA)
            }
        }.getOrNull()

        if (resultCode == Activity.RESULT_OK && data != null) {
            ScreenCaptureBus.start()
            capture(resultCode, data)
        } else {
            failSafe("اجازهٔ ضبط صفحه داده نشد.")
        }
        return START_NOT_STICKY
    }

    private fun promoteToForeground(): Boolean = runCatching {
        ensureChannel()
        val notification = buildNotification()
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
    }.onFailure { error ->
        Log.e(TAG, "startForeground failed", error)
    }.isSuccess

    private fun screenSize(): IntArray {
        val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val width: Int
        val height: Int
        val density: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = manager.currentWindowMetrics.bounds
            width = bounds.width()
            height = bounds.height()
            density = resources.configuration.densityDpi
        } else {
            val metrics = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            manager.defaultDisplay.getRealMetrics(metrics)
            width = metrics.widthPixels
            height = metrics.heightPixels
            density = metrics.densityDpi
        }
        // Even dimensions keep every device's mirror buffer happy.
        return intArrayOf(width and 0x7FFFFFFE, height and 0x7FFFFFFE, density)
    }

    private fun capture(resultCode: Int, data: Intent) {
        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
            val active = projection ?: run {
                failSafe("دسترسی ضبط صفحه باز نشد. یک‌بار دیگر تلاش کن.")
                return
            }
            active.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    handler.post { if (!finished) failSafe("ضبط صفحه متوقف شد.") }
                }
            }, handler)

            val dims = screenSize()
            val width = dims[0]
            val height = dims[1]
            val density = dims[2]
            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
            imageReader = reader
            reader.setOnImageAvailableListener({ r -> onFrame(r, width, height) }, handler)

            virtualDisplay = active.createVirtualDisplay(
                "EspressoScan",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler,
            )
            if (virtualDisplay == null) failSafe("نمایش مجازی ساخته نشد.")

            handler.postDelayed({
                if (!finished) {
                    Log.w(TAG, "capture timeout after $blankFrames blank frames")
                    failSafe(
                        if (blankFrames > 0) {
                            "گوشی تصویر صفحه را برنگرداند (صفحهٔ سیاه). از دکمهٔ «انتخاب تصویر» استفاده کن."
                        } else {
                            "زمان ضبط صفحه تمام شد. دوباره تلاش کن."
                        },
                    )
                }
            }, DEADLINE_MS)
        } catch (error: Throwable) {
            Log.e(TAG, "capture failed", error)
            failSafe(error.message ?: "ضبط صفحه ممکن نشد.")
        }
    }

    private fun onFrame(reader: ImageReader, width: Int, height: Int) {
        val image: Image = runCatching { reader.acquireLatestImage() }.getOrNull() ?: return
        var delivered = false
        try {
            if (finished) return
            val bitmap = runCatching { image.toBitmap(width, height) }.getOrNull() ?: return
            if (isUniform(bitmap)) {
                // MIUI often emits a few blank frames before the mirror warms up.
                blankFrames++
                if (blankFrames < MAX_BLANK_FRAMES) return
                failSafe("تصویر صفحه خالی بود. از دکمهٔ «انتخاب تصویر» استفاده کن.")
                delivered = true
                return
            }
            finished = true
            ScreenCaptureBus.success(bitmap)
            delivered = true
        } catch (error: Throwable) {
            Log.e(TAG, "frame conversion failed", error)
            failSafe("تصویر صفحه خوانده نشد.")
        } finally {
            runCatching { image.close() }
            if (delivered || finished) cleanup()
        }
    }

    private fun Image.toBitmap(width: Int, height: Int): Bitmap {
        val plane = planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        // Respect rowStride: screen buffers pad each row to an alignment boundary. Reading
        // width*4 blindly skews the image and destroys OCR accuracy.
        val pixels = IntArray(width * height)
        buffer.rewind()
        for (y in 0 until height) {
            val rowStart = y * rowStride
            var index = rowStart
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

    /** A frame where every sampled pixel is identical is a dead mirror, not a dark screen. */
    private fun isUniform(bitmap: Bitmap): Boolean {
        val stepX = (bitmap.width / 24).coerceAtLeast(1)
        val stepY = (bitmap.height / 24).coerceAtLeast(1)
        var first = Int.MIN_VALUE
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                if (first == Int.MIN_VALUE) first = pixel else if (pixel != first) return false
                x += stepX
            }
            y += stepY
        }
        return true
    }

    private fun failSafe(message: String) {
        if (finished) return
        finished = true
        ScreenCaptureBus.fail(message)
        cleanup()
    }

    private fun cleanup() {
        runCatching { virtualDisplay?.release() }
        runCatching { imageReader?.close() }
        runCatching { projection?.stop() }
        virtualDisplay = null
        imageReader = null
        projection = null
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("اسکن صفحه")
            .setContentText("در حال خواندن متن صفحه…")
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        runCatching { virtualDisplay?.release() }
        runCatching { imageReader?.close() }
        runCatching { projection?.stop() }
        super.onDestroy()
    }
}
