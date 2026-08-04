package ru.chernenko.snipjet.ui

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
import ru.chernenko.snipjet.capture.CaptureOutcome

@Composable
fun SnipJetApp(
    session: SnipJetSession,
    onVisibilityForCapture: (visible: Boolean) -> Unit,
    onEditorOpen: (open: Boolean) -> Unit,
    onExit: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val captureRunner = remember { AreaCaptureRunner() }
    var captureJob by remember { mutableStateOf<Job?>(null) }

    val tabs = session.tabs
    val editorOpen = session.editorOpen
    // Observe history so undo/redo button state refreshes.
    @Suppress("UNUSED_VARIABLE")
    val historyRevision = session.historyRevision

    LaunchedEffect(editorOpen) {
        onEditorOpen(editorOpen)
    }

    fun startCaptureFromEditor() {
        if (captureJob?.isActive == true) return
        captureJob = scope.launch {
            when (val outcome = captureRunner.captureArea(onVisibilityForCapture)) {
                is CaptureOutcome.Success -> {
                    session.openTab(outcome.image)
                    captureRunner.copyPngInBackground(scope, outcome.pngBytes)
                }
                else -> Unit
            }
        }
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
            undoEnabled = session.canUndo(activeTab.id),
            redoEnabled = session.canRedo(activeTab.id),
            onNewCapture = ::startCaptureFromEditor,
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
