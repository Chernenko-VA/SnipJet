package ru.chernenko.snipjet.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.chernenko.snipjet.capture.AreaCaptureRunner
import ru.chernenko.snipjet.capture.CaptureOutcome
import ru.chernenko.snipjet.config.MessageKeys
import ru.chernenko.snipjet.config.Messages
import ru.chernenko.snipjet.editor.EditorTab
import ru.chernenko.snipjet.editor.StrokeAnnotation

@Composable
fun SnipJetApp(
    onVisibilityForCapture: (visible: Boolean) -> Unit,
    onEditorOpen: (open: Boolean) -> Unit,
    onExit: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val captureRunner = remember { AreaCaptureRunner() }
    var tabs by remember { mutableStateOf<List<EditorTab>>(emptyList()) }
    var activeTabId by remember { mutableStateOf<Long?>(null) }
    var nextTabId by remember { mutableLongStateOf(1L) }
    var nextTabNumber by remember { mutableIntStateOf(1) }
    var captureJob by remember { mutableStateOf<Job?>(null) }

    val editorOpen = tabs.isNotEmpty()
    LaunchedEffect(editorOpen) {
        onEditorOpen(editorOpen)
    }

    fun openTab(image: ImageBitmap) {
        val id = nextTabId
        nextTabId += 1
        val number = nextTabNumber
        nextTabNumber += 1
        val tab = EditorTab(
            id = id,
            title = Messages.get(MessageKeys.EDITOR_TAB_TITLE, number),
            image = image,
        )
        tabs = tabs + tab
        activeTabId = id
    }

    fun closeTab(id: Long) {
        val remaining = tabs.filter { it.id != id }
        tabs = remaining
        activeTabId = when {
            remaining.isEmpty() -> null
            activeTabId == id -> remaining.last().id
            else -> activeTabId
        }
    }

    fun updateStrokes(tabId: Long, strokes: List<StrokeAnnotation>) {
        tabs = tabs.map { tab ->
            if (tab.id == tabId) tab.copy(strokes = strokes) else tab
        }
    }

    fun startCaptureFromEditor() {
        if (captureJob?.isActive == true) return
        captureJob = scope.launch {
            when (val outcome = captureRunner.captureArea(onVisibilityForCapture)) {
                is CaptureOutcome.Success -> {
                    openTab(outcome.image)
                    captureRunner.copyPngInBackground(scope, outcome.pngBytes)
                }
                else -> Unit
            }
        }
    }

    val activeTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.lastOrNull()
    if (activeTab != null && tabs.isNotEmpty()) {
        EditorScreen(
            tab = activeTab,
            tabs = tabs,
            onSelectTab = { activeTabId = it },
            onCloseTab = ::closeTab,
            onStrokesChange = { tabId, strokes -> updateStrokes(tabId, strokes) },
            onNewCapture = ::startCaptureFromEditor,
        )
    } else {
        StatusApp(
            onVisibilityForCapture = onVisibilityForCapture,
            onCaptureReady = ::openTab,
            onExit = onExit,
            captureRunner = captureRunner,
        )
    }
}
