package ir.esmail.espresso.capture

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CaptureState(
    val running: Boolean = false,
    val bitmap: Bitmap? = null,
    val error: String? = null,
    val sequence: Long = 0L,
)

/** Same-process bridge between the foreground capture service and the Compose screen. */
object ScreenCaptureBus {
    private val _state = MutableStateFlow(CaptureState())
    val state: StateFlow<CaptureState> = _state.asStateFlow()

    private var counter = 0L

    fun start() {
        _state.value = CaptureState(running = true, sequence = ++counter)
    }

    fun success(bitmap: Bitmap) {
        _state.value = CaptureState(bitmap = bitmap, sequence = ++counter)
    }

    fun fail(message: String) {
        _state.value = CaptureState(error = message, sequence = ++counter)
    }

    fun clear() {
        _state.value = CaptureState()
    }
}
