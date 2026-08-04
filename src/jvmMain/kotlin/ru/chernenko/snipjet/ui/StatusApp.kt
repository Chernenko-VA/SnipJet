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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.chernenko.snipjet.capture.AreaCaptureRunner
import ru.chernenko.snipjet.capture.CaptureOutcome
import ru.chernenko.snipjet.config.MessageKeys
import ru.chernenko.snipjet.config.Messages

private sealed interface StatusPhase {
    data object Checking : StatusPhase
    data object Ready : StatusPhase
    data object NeedInstall : StatusPhase
    data object NeedClipboard : StatusPhase
    data object Capturing : StatusPhase
    data object Copied : StatusPhase
    data object Cancelled : StatusPhase
    data class Failed(val message: String) : StatusPhase
}

private data class StatusContent(
    val showProgress: Boolean = false,
    val title: String? = null,
    val hint: String? = null,
    val command: String? = null,
    val errorMessage: String? = null,
)

private data class ActionBarState(
    val captureLabel: String,
    val captureEnabled: Boolean,
    val showRetry: Boolean,
    val exitEnabled: Boolean,
)

@Composable
fun StatusApp(
    onVisibilityForCapture: (visible: Boolean) -> Unit,
    onCaptureReady: (ImageBitmap) -> Unit,
    onExit: () -> Unit,
    captureRunner: AreaCaptureRunner = remember { AreaCaptureRunner() },
) {
    var phase by remember { mutableStateOf<StatusPhase>(StatusPhase.Checking) }
    val scope = rememberCoroutineScope()
    var captureJob by remember { mutableStateOf<Job?>(null) }

    fun runCapture() {
        if (captureJob?.isActive == true) return
        if (!captureRunner.isCaptureAvailable() || !captureRunner.isClipboardAvailable()) return
        phase = StatusPhase.Capturing
        captureJob = scope.launch {
            when (val outcome = captureRunner.captureArea(onVisibilityForCapture)) {
                is CaptureOutcome.Success -> {
                    onCaptureReady(outcome.image)
                    phase = StatusPhase.Ready
                    captureRunner.copyPngInBackground(scope, outcome.pngBytes)
                }
                is CaptureOutcome.Cancelled -> phase = StatusPhase.Cancelled
                is CaptureOutcome.NeedInstall -> phase = StatusPhase.NeedInstall
                is CaptureOutcome.NeedClipboard -> phase = StatusPhase.NeedClipboard
                is CaptureOutcome.Failed -> phase = StatusPhase.Failed(outcome.message)
            }
        }
    }

    fun checkDependency() {
        scope.launch {
            val captureOk = withContext(Dispatchers.IO) { captureRunner.isCaptureAvailable() }
            if (!captureOk) {
                phase = StatusPhase.NeedInstall
                return@launch
            }
            val clipboardOk = withContext(Dispatchers.IO) { captureRunner.isClipboardAvailable() }
            phase = if (clipboardOk) StatusPhase.Ready else StatusPhase.NeedClipboard
        }
    }

    LaunchedEffect(Unit) {
        checkDependency()
    }

    val busy = phase is StatusPhase.Capturing || captureJob?.isActive == true
    val content = phase.toContent()
    val actions = phase.toActionBarState(
        busy = busy,
        captureAvailable = captureRunner.isCaptureAvailable(),
        clipboardAvailable = captureRunner.isClipboardAvailable(),
    )

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
                onClick = ::runCapture,
                enabled = actions.captureEnabled,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Text(actions.captureLabel)
            }
            if (actions.showRetry) {
                Button(
                    onClick = ::checkDependency,
                    enabled = !busy,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Text(Messages.get(MessageKeys.STATUS_NEED_INSTALL_RETRY))
                }
            }
            ExitButton(onClick = onExit, enabled = actions.exitEnabled)
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

@Composable
private fun ExitButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
    ) {
        Text(
            Messages.get(MessageKeys.STATUS_EXIT),
            maxLines = 1,
            softWrap = false,
        )
    }
}

private fun StatusPhase.toContent(): StatusContent = when (this) {
    is StatusPhase.Checking -> StatusContent(
        showProgress = true,
        title = Messages.get(MessageKeys.STATUS_CHECKING),
    )
    is StatusPhase.Capturing -> StatusContent(
        showProgress = true,
        title = Messages.get(MessageKeys.STATUS_CAPTURING),
    )
    is StatusPhase.Ready -> StatusContent(
        title = Messages.get(MessageKeys.STATUS_READY_TITLE),
        hint = Messages.get(MessageKeys.STATUS_READY_HINT),
    )
    is StatusPhase.Copied -> StatusContent(
        title = Messages.get(MessageKeys.STATUS_COPIED_TITLE),
        hint = Messages.get(MessageKeys.STATUS_COPIED_HINT),
    )
    is StatusPhase.Cancelled -> StatusContent(
        title = Messages.get(MessageKeys.STATUS_CANCELLED_TITLE),
        hint = Messages.get(MessageKeys.STATUS_CANCELLED_HINT),
    )
    is StatusPhase.NeedInstall -> StatusContent(
        title = Messages.get(MessageKeys.STATUS_NEED_INSTALL_TITLE),
        hint = Messages.get(MessageKeys.STATUS_NEED_INSTALL_HINT),
        command = Messages.get(MessageKeys.STATUS_NEED_INSTALL_COMMAND),
    )
    is StatusPhase.NeedClipboard -> StatusContent(
        title = Messages.get(MessageKeys.STATUS_NEED_CLIPBOARD_TITLE),
        hint = Messages.get(MessageKeys.STATUS_NEED_CLIPBOARD_HINT),
        command = Messages.get(MessageKeys.STATUS_NEED_CLIPBOARD_COMMAND),
    )
    is StatusPhase.Failed -> StatusContent(errorMessage = message)
}

private fun StatusPhase.toActionBarState(
    busy: Boolean,
    captureAvailable: Boolean,
    clipboardAvailable: Boolean,
): ActionBarState {
    val needsDependency = this is StatusPhase.NeedInstall || this is StatusPhase.NeedClipboard
    val captureLabel = when (this) {
        is StatusPhase.Ready, is StatusPhase.NeedInstall, is StatusPhase.NeedClipboard,
        is StatusPhase.Checking, is StatusPhase.Capturing,
        -> Messages.get(MessageKeys.STATUS_READY_CAPTURE)
        else -> Messages.get(MessageKeys.STATUS_NEW_CAPTURE)
    }
    val captureEnabled = !busy &&
        !needsDependency &&
        this !is StatusPhase.Checking &&
        this !is StatusPhase.Capturing &&
        captureAvailable &&
        clipboardAvailable
    return ActionBarState(
        captureLabel = captureLabel,
        captureEnabled = captureEnabled,
        showRetry = needsDependency,
        exitEnabled = this !is StatusPhase.Capturing,
    )
}
