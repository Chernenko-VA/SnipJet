package ru.chernenko.snipjet.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.chernenko.snipjet.SnipJetSession
import ru.chernenko.snipjet.capture.AreaCaptureRunner
import ru.chernenko.snipjet.capture.CaptureHandlers
import ru.chernenko.snipjet.config.MessageKeys
import ru.chernenko.snipjet.config.Messages

/**
 * Editor window content. Status lives in a separate Compose [androidx.compose.ui.window.Window].
 */
@Composable
fun EditorApp(
    session: SnipJetSession,
    captureRunner: AreaCaptureRunner,
    onVisibilityForCapture: (visible: Boolean) -> Unit,
    pendingBackgroundCopyPng: ByteArray? = null,
) {
    val scope = rememberCoroutineScope()
    var captureJob by remember { mutableStateOf<Job?>(null) }
    var captureErrorTitle by remember { mutableStateOf<String?>(null) }
    var captureErrorMessage by remember { mutableStateOf<String?>(null) }

    val tabs = session.tabs
    val canUndoActive = session.canUndoActive
    val canRedoActive = session.canRedoActive
    val activeTab = session.activeTab ?: return

    LaunchedEffect(pendingBackgroundCopyPng) {
        val bytes = pendingBackgroundCopyPng ?: return@LaunchedEffect
        captureRunner.copyPngInBackground(bytes)
    }

    fun dismissCaptureError() {
        captureErrorTitle = null
        captureErrorMessage = null
    }

    fun startCaptureFromEditor() {
        if (captureJob?.isActive == true) return
        captureJob = scope.launch {
            captureRunner.captureAreaAndDispatch(
                onVisibilityForCapture = onVisibilityForCapture,
                handlers = CaptureHandlers(
                    onSuccess = { outcome -> session.openTab(outcome.image) },
                    onNeedInstall = {
                        captureErrorTitle = Messages.get(MessageKeys.STATUS_NEED_INSTALL_TITLE)
                        captureErrorMessage = Messages.get(MessageKeys.STATUS_NEED_INSTALL_HINT)
                    },
                    onNeedClipboard = {
                        captureErrorTitle = Messages.get(MessageKeys.STATUS_NEED_CLIPBOARD_TITLE)
                        captureErrorMessage = Messages.get(MessageKeys.STATUS_NEED_CLIPBOARD_HINT)
                    },
                    onFailed = { message ->
                        captureErrorTitle = Messages.get(MessageKeys.ERROR_CAPTURE_FAILED)
                        captureErrorMessage = message
                    },
                ),
            )
        }
    }

    if (captureErrorTitle != null && captureErrorMessage != null) {
        AlertDialog(
            onDismissRequest = ::dismissCaptureError,
            title = { Text(captureErrorTitle.orEmpty()) },
            text = { Text(captureErrorMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = ::dismissCaptureError) {
                    Text(Messages.get(MessageKeys.DIALOG_OK))
                }
            },
        )
    }

    EditorScreen(
        tab = activeTab,
        tabs = tabs,
        onSelectTab = session::selectTab,
        onCloseTab = session::closeTab,
        onAnnotationsChange = session::updateAnnotations,
        onUndo = { session.undo(activeTab.id) },
        onRedo = { session.redo(activeTab.id) },
        undoEnabled = canUndoActive,
        redoEnabled = canRedoActive,
        onNewCapture = ::startCaptureFromEditor,
        onBeforeAnnotatedCopy = captureRunner::cancelBackgroundCopy,
        clipboard = captureRunner.clipboard(),
    )
}
