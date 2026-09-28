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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.chernenko.snipjet.SnipJetSession
import ru.chernenko.snipjet.capture.AreaCaptureRunner
import ru.chernenko.snipjet.capture.CaptureHandlers
import ru.chernenko.snipjet.config.MessageKeys
import ru.chernenko.snipjet.config.Messages

@Composable
fun SnipJetApp(
    session: SnipJetSession,
    onVisibilityForCapture: (visible: Boolean) -> Unit,
    onEditorOpen: (open: Boolean) -> Unit,
    onExit: () -> Unit,
    ipcCaptureRequests: Flow<Unit>? = null,
    onBringToFront: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val captureRunner = remember { AreaCaptureRunner() }
    val captureJobHolder = remember { object { var job: Job? = null } }
    var captureErrorTitle by remember { mutableStateOf<String?>(null) }
    var captureErrorMessage by remember { mutableStateOf<String?>(null) }

    val tabs = session.tabs
    val editorOpen = session.editorOpen
    val canUndoActive = session.canUndoActive
    val canRedoActive = session.canRedoActive

    LaunchedEffect(editorOpen) {
        onEditorOpen(editorOpen)
    }

    fun dismissCaptureError() {
        captureErrorTitle = null
        captureErrorMessage = null
    }

    fun startCaptureFromEditor() {
        if (captureJobHolder.job?.isActive == true) return
        captureJobHolder.job = scope.launch {
            captureRunner.captureAreaAndDispatch(
                scope = scope,
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

    if (ipcCaptureRequests != null) {
        LaunchedEffect(ipcCaptureRequests) {
            ipcCaptureRequests.collect {
                // Focus is restored after capture via CaptureWindowController.onVisibilityForCapture(true).
                startCaptureFromEditor()
            }
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

    val activeTab = session.activeTab
    if (activeTab != null && tabs.isNotEmpty()) {
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
    } else {
        StatusApp(
            onVisibilityForCapture = onVisibilityForCapture,
            onCaptureReady = session::openTab,
            onExit = onExit,
            captureRunner = captureRunner,
        )
    }
}
