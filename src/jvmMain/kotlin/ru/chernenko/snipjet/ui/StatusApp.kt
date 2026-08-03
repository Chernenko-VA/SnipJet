package ru.chernenko.snipjet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.chernenko.snipjet.capture.GnomeScreenshotCapture
import ru.chernenko.snipjet.capture.ScreenCaptureException
import ru.chernenko.snipjet.clipboard.ClipboardException
import ru.chernenko.snipjet.clipboard.ImageClipboard
import ru.chernenko.snipjet.clipboard.LinuxImageClipboard
import ru.chernenko.snipjet.config.AppConfig
import ru.chernenko.snipjet.config.MessageKeys
import ru.chernenko.snipjet.config.Messages
import java.nio.file.Files

private sealed interface StatusPhase {
    data object Checking : StatusPhase
    data object Ready : StatusPhase
    data object NeedInstall : StatusPhase
    data object Capturing : StatusPhase
    data object Copied : StatusPhase
    data object Cancelled : StatusPhase
    data class Failed(val message: String) : StatusPhase
}

@Composable
fun StatusApp(
    onVisibilityForCapture: (visible: Boolean) -> Unit,
    onExit: () -> Unit,
    capture: GnomeScreenshotCapture = remember { GnomeScreenshotCapture() },
    clipboard: ImageClipboard = remember { LinuxImageClipboard() },
) {
    var phase by remember { mutableStateOf<StatusPhase>(StatusPhase.Checking) }
    val scope = rememberCoroutineScope()
    var captureJob by remember { mutableStateOf<Job?>(null) }

    fun runCapture() {
        if (captureJob?.isActive == true) return
        phase = StatusPhase.Capturing
        captureJob = scope.launch {
            onVisibilityForCapture(false)
            delay(AppConfig.windowHideDelayMs)
            var tempFile: java.nio.file.Path? = null
            try {
                tempFile = withContext(Dispatchers.IO) { capture.captureArea() }
                withContext(Dispatchers.IO) { clipboard.copyPngFile(tempFile) }
                phase = StatusPhase.Copied
            } catch (e: ScreenCaptureException) {
                when {
                    !capture.isAvailable() -> phase = StatusPhase.NeedInstall
                    e.isCancellationLike() -> phase = StatusPhase.Cancelled
                    else -> phase = StatusPhase.Failed(
                        e.message ?: Messages.get(
                            MessageKeys.ERROR_GENERIC,
                            Messages.get(MessageKeys.ERROR_CAPTURE_FAILED),
                        )
                    )
                }
            } catch (e: ClipboardException) {
                phase = StatusPhase.Failed(
                    e.message ?: Messages.get(
                        MessageKeys.ERROR_GENERIC,
                        Messages.get(MessageKeys.ERROR_CLIPBOARD_UNAVAILABLE),
                    )
                )
            } catch (e: Exception) {
                phase = StatusPhase.Failed(
                    Messages.get(MessageKeys.ERROR_GENERIC, e.message ?: e.toString())
                )
            } finally {
                tempFile?.let { Files.deleteIfExists(it) }
                onVisibilityForCapture(true)
            }
        }
    }

    fun checkDependency() {
        scope.launch {
            val available = withContext(Dispatchers.IO) { capture.isAvailable() }
            phase = if (available) StatusPhase.Ready else StatusPhase.NeedInstall
        }
    }

    LaunchedEffect(Unit) {
        checkDependency()
    }

    val busy = phase is StatusPhase.Capturing || captureJob?.isActive == true

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val current = phase) {
            is StatusPhase.Checking -> {
                CircularProgressIndicator()
                Text(
                    Messages.get(MessageKeys.STATUS_CHECKING),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            is StatusPhase.Ready -> {
                Text(
                    Messages.get(MessageKeys.STATUS_READY_TITLE),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    Messages.get(MessageKeys.STATUS_READY_HINT),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier.padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(onClick = ::runCapture, enabled = !busy) {
                        Text(Messages.get(MessageKeys.STATUS_READY_CAPTURE))
                    }
                    OutlinedButton(onClick = onExit) {
                        Text(Messages.get(MessageKeys.STATUS_EXIT))
                    }
                }
            }

            is StatusPhase.NeedInstall -> {
                Text(
                    Messages.get(MessageKeys.STATUS_NEED_INSTALL_TITLE),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    Messages.get(MessageKeys.STATUS_NEED_INSTALL_HINT),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    Messages.get(MessageKeys.STATUS_NEED_INSTALL_COMMAND),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(
                    modifier = Modifier.padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(onClick = ::checkDependency) {
                        Text(Messages.get(MessageKeys.STATUS_NEED_INSTALL_RETRY))
                    }
                    OutlinedButton(onClick = onExit) {
                        Text(Messages.get(MessageKeys.STATUS_EXIT))
                    }
                }
            }

            is StatusPhase.Capturing -> {
                CircularProgressIndicator()
                Text(
                    Messages.get(MessageKeys.STATUS_CAPTURING),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            is StatusPhase.Copied -> {
                Text(
                    Messages.get(MessageKeys.STATUS_COPIED_TITLE),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    Messages.get(MessageKeys.STATUS_COPIED_HINT),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier.padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(onClick = ::runCapture, enabled = !busy) {
                        Text(Messages.get(MessageKeys.STATUS_NEW_CAPTURE))
                    }
                    OutlinedButton(onClick = onExit) {
                        Text(Messages.get(MessageKeys.STATUS_EXIT))
                    }
                }
            }

            is StatusPhase.Cancelled -> {
                Text(
                    Messages.get(MessageKeys.STATUS_CANCELLED_TITLE),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    Messages.get(MessageKeys.STATUS_CANCELLED_HINT),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier.padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(onClick = ::runCapture, enabled = !busy) {
                        Text(Messages.get(MessageKeys.STATUS_NEW_CAPTURE))
                    }
                    OutlinedButton(onClick = onExit) {
                        Text(Messages.get(MessageKeys.STATUS_EXIT))
                    }
                }
            }

            is StatusPhase.Failed -> {
                Text(
                    current.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row(
                    modifier = Modifier.padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(onClick = ::runCapture, enabled = !busy) {
                        Text(Messages.get(MessageKeys.STATUS_NEW_CAPTURE))
                    }
                    OutlinedButton(onClick = onExit) {
                        Text(Messages.get(MessageKeys.STATUS_EXIT))
                    }
                }
            }
        }
    }
}

private fun ScreenCaptureException.isCancellationLike(): Boolean {
    val msg = message.orEmpty()
    return msg.contains("cancelled", ignoreCase = true) ||
        msg.contains("отмен", ignoreCase = true)
}
