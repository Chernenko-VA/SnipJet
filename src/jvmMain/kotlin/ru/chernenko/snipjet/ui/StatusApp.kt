package ru.chernenko.snipjet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.chernenko.snipjet.capture.AreaCaptureRunner
import ru.chernenko.snipjet.capture.CaptureOutcome
import ru.chernenko.snipjet.config.MessageKeys
import ru.chernenko.snipjet.config.Messages

private sealed interface StatusPhase {
    data object Checking : StatusPhase
    data object Ready : StatusPhase
    data class Outcome(val value: CaptureOutcome) : StatusPhase
}

private data class StatusContent(
    val showProgress: Boolean = false,
    val title: String? = null,
    val hint: String? = null,
    val command: String? = null,
    val errorMessage: String? = null,
)

/**
 * Status UI only. Capture is started by [onStartCapture] (headless, same as --capture).
 */
@Composable
fun StatusApp(
    onStartCapture: () -> Unit,
    onExit: () -> Unit,
    captureRunner: AreaCaptureRunner,
    initialOutcome: CaptureOutcome? = null,
) {
    var phase by remember(initialOutcome) {
        mutableStateOf(
            if (initialOutcome != null) {
                StatusPhase.Outcome(initialOutcome)
            } else {
                StatusPhase.Checking
            },
        )
    }
    val scope = rememberCoroutineScope()

    fun checkDependency() {
        scope.launch {
            val captureOk = withContext(Dispatchers.IO) { captureRunner.isCaptureAvailable() }
            if (!captureOk) {
                phase = StatusPhase.Outcome(CaptureOutcome.NeedInstall)
                return@launch
            }
            val clipboardOk = withContext(Dispatchers.IO) { captureRunner.isClipboardAvailable() }
            phase = if (clipboardOk) {
                StatusPhase.Ready
            } else {
                StatusPhase.Outcome(CaptureOutcome.NeedClipboard)
            }
        }
    }

    LaunchedEffect(initialOutcome) {
        if (initialOutcome == null) {
            checkDependency()
        } else {
            phase = StatusPhase.Outcome(initialOutcome)
        }
    }

    val content = phase.toContent()
    val outcome = (phase as? StatusPhase.Outcome)?.value
    val needsDependency = outcome is CaptureOutcome.NeedInstall ||
        outcome is CaptureOutcome.NeedClipboard
    val captureLabel = when (outcome) {
        is CaptureOutcome.Cancelled, is CaptureOutcome.Failed -> {
            Messages.get(MessageKeys.STATUS_NEW_CAPTURE)
        }
        else -> Messages.get(MessageKeys.STATUS_READY_CAPTURE)
    }
    val captureEnabled = phase !is StatusPhase.Checking &&
        !needsDependency &&
        captureRunner.isCaptureAvailable() &&
        captureRunner.isClipboardAvailable()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            StatusContentView(content)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            Button(
                onClick = onStartCapture,
                enabled = captureEnabled,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Text(captureLabel)
            }
            if (needsDependency) {
                Button(
                    onClick = ::checkDependency,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Text(Messages.get(MessageKeys.STATUS_NEED_INSTALL_RETRY))
                }
            }
            OutlinedButton(onClick = onExit) {
                Text(
                    Messages.get(MessageKeys.STATUS_EXIT),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun StatusContentView(content: StatusContent) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (content.showProgress) {
            CircularProgressIndicator()
        }
        content.errorMessage?.let { message ->
            Text(
                message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
        content.title?.let { title ->
            Text(
                title,
                modifier = if (content.showProgress) Modifier.padding(top = 16.dp) else Modifier,
                style = if (content.showProgress) {
                    MaterialTheme.typography.bodyLarge
                } else {
                    MaterialTheme.typography.headlineSmall
                },
                textAlign = TextAlign.Center,
            )
        }
        content.hint?.let { hint ->
            Text(
                hint,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
        content.command?.let { command ->
            Text(
                command,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun StatusPhase.toContent(): StatusContent = when (this) {
    is StatusPhase.Checking -> StatusContent(
        showProgress = true,
        title = Messages.get(MessageKeys.STATUS_CHECKING),
    )
    is StatusPhase.Ready -> StatusContent(
        title = Messages.get(MessageKeys.STATUS_READY_TITLE),
        hint = Messages.get(MessageKeys.STATUS_READY_HINT),
    )
    is StatusPhase.Outcome -> value.toStatusContent()
}

private fun CaptureOutcome.toStatusContent(): StatusContent = when (this) {
    is CaptureOutcome.Success -> StatusContent(
        title = Messages.get(MessageKeys.STATUS_READY_TITLE),
        hint = Messages.get(MessageKeys.STATUS_READY_HINT),
    )
    CaptureOutcome.Cancelled -> StatusContent(
        title = Messages.get(MessageKeys.STATUS_CANCELLED_TITLE),
        hint = Messages.get(MessageKeys.STATUS_CANCELLED_HINT),
    )
    CaptureOutcome.NeedInstall -> StatusContent(
        title = Messages.get(MessageKeys.STATUS_NEED_INSTALL_TITLE),
        hint = Messages.get(MessageKeys.STATUS_NEED_INSTALL_HINT),
        command = Messages.get(MessageKeys.STATUS_NEED_INSTALL_COMMAND),
    )
    CaptureOutcome.NeedClipboard -> StatusContent(
        title = Messages.get(MessageKeys.STATUS_NEED_CLIPBOARD_TITLE),
        hint = Messages.get(MessageKeys.STATUS_NEED_CLIPBOARD_HINT),
        command = Messages.get(MessageKeys.STATUS_NEED_CLIPBOARD_COMMAND),
    )
    is CaptureOutcome.Failed -> StatusContent(errorMessage = message)
}
