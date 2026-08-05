package ru.chernenko.snipjet.capture

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.chernenko.snipjet.clipboard.ImageClipboard
import ru.chernenko.snipjet.clipboard.LinuxImageClipboard
import ru.chernenko.snipjet.config.AppConfig
import ru.chernenko.snipjet.config.MessageKeys
import ru.chernenko.snipjet.config.Messages
import ru.chernenko.snipjet.editor.loadPngImageBitmap
import java.nio.file.Files

sealed interface CaptureOutcome {
    data class Success(
        val image: ImageBitmap,
        val pngBytes: ByteArray,
    ) : CaptureOutcome

    data object Cancelled : CaptureOutcome
    data object NeedInstall : CaptureOutcome
    data object NeedClipboard : CaptureOutcome
    data class Failed(val message: String) : CaptureOutcome
}

data class CaptureHandlers(
    val onSuccess: (CaptureOutcome.Success) -> Unit,
    val onCancelled: () -> Unit = {},
    val onNeedInstall: () -> Unit = {},
    val onNeedClipboard: () -> Unit = {},
    val onFailed: (String) -> Unit = {},
)

/**
 * Area capture: optional hide window → gnome-screenshot → load PNG.
 * Null [onVisibilityForCapture] = headless (same path as --capture).
 */
class AreaCaptureRunner(
    private val capture: GnomeScreenshotCapture = GnomeScreenshotCapture(),
    private val clipboard: ImageClipboard = LinuxImageClipboard(),
) {
    @Volatile
    private var backgroundCopyJob: Job? = null

    private val copyScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun isCaptureAvailable(): Boolean = capture.isAvailable()

    fun isClipboardAvailable(): Boolean = clipboard.isAvailable()

    fun clipboard(): ImageClipboard = clipboard

    suspend fun captureArea(
        onVisibilityForCapture: ((visible: Boolean) -> Unit)? = null,
    ): CaptureOutcome {
        if (!capture.isAvailable()) return CaptureOutcome.NeedInstall
        if (!clipboard.isAvailable()) return CaptureOutcome.NeedClipboard

        if (onVisibilityForCapture != null) {
            onVisibilityForCapture(false)
            delay(AppConfig.settings.capture.hideDelayMs)
        }

        var tempFile: java.nio.file.Path? = null
        return try {
            tempFile = withContext(Dispatchers.IO) { capture.captureArea() }
            val pngBytes = withContext(Dispatchers.IO) { Files.readAllBytes(tempFile) }
            val bitmap = withContext(Dispatchers.IO) { loadPngImageBitmap(pngBytes) }
            CaptureOutcome.Success(bitmap, pngBytes)
        } catch (e: ScreenCaptureException) {
            when {
                e.isCancellationLike() -> CaptureOutcome.Cancelled
                else -> CaptureOutcome.Failed(
                    e.message ?: Messages.get(
                        MessageKeys.ERROR_GENERIC,
                        Messages.get(MessageKeys.ERROR_CAPTURE_FAILED),
                    ),
                )
            }
        } catch (e: Exception) {
            CaptureOutcome.Failed(
                Messages.get(MessageKeys.ERROR_GENERIC, e.message ?: e.toString()),
            )
        } finally {
            onVisibilityForCapture?.invoke(true)
            tempFile?.let { Files.deleteIfExists(it) }
        }
    }

    suspend fun captureAreaAndDispatch(
        onVisibilityForCapture: ((visible: Boolean) -> Unit)?,
        handlers: CaptureHandlers,
    ) {
        when (val outcome = captureArea(onVisibilityForCapture)) {
            is CaptureOutcome.Success -> {
                handlers.onSuccess(outcome)
                copyPngInBackground(outcome.pngBytes)
            }
            CaptureOutcome.Cancelled -> handlers.onCancelled()
            CaptureOutcome.NeedInstall -> handlers.onNeedInstall()
            CaptureOutcome.NeedClipboard -> handlers.onNeedClipboard()
            is CaptureOutcome.Failed -> handlers.onFailed(outcome.message)
        }
    }

    fun copyPngInBackground(pngBytes: ByteArray) {
        cancelBackgroundCopy()
        backgroundCopyJob = copyScope.launch {
            try {
                delay(AppConfig.settings.capture.backgroundCopyDelayMs)
                clipboard.copyPngBytes(pngBytes)
            } catch (_: Exception) {
                // Background copy must not block the editor.
            }
        }
    }

    fun cancelBackgroundCopy() {
        backgroundCopyJob?.cancel()
        backgroundCopyJob = null
    }
}

fun ScreenCaptureException.isCancellationLike(): Boolean {
    val msg = message.orEmpty()
    return msg.contains("cancelled", ignoreCase = true) ||
        msg.contains("отмен", ignoreCase = true)
}
