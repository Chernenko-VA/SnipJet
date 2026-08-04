package ru.chernenko.snipjet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import ru.chernenko.snipjet.config.MessageKeys
import ru.chernenko.snipjet.config.Messages
import ru.chernenko.snipjet.editor.AnnotationHistory
import ru.chernenko.snipjet.editor.EditorAnnotation
import ru.chernenko.snipjet.editor.EditorTab

/**
 * Editor session kept outside Compose so tabs survive window/composition recreation
 * (e.g. after long idle / WM suspend on Linux).
 */
class SnipJetSession {
    var tabs by mutableStateOf<List<EditorTab>>(emptyList())
        private set
    var activeTabId by mutableStateOf<Long?>(null)
        private set

    /** Bumped on undo/redo/push so UI can refresh enabled flags. */
    var historyRevision by mutableIntStateOf(0)
        private set

    private var nextTabId by mutableLongStateOf(1L)
    private var nextTabNumber by mutableIntStateOf(1)
    private val histories = mutableMapOf<Long, AnnotationHistory>()

    val editorOpen: Boolean get() = tabs.isNotEmpty()

    val activeTab: EditorTab?
        get() = tabs.firstOrNull { it.id == activeTabId } ?: tabs.lastOrNull()

    fun openTab(image: ImageBitmap) {
        val id = nextTabId
        nextTabId += 1
        val number = nextTabNumber
        nextTabNumber += 1
        histories[id] = AnnotationHistory()
        tabs = tabs + EditorTab(
            id = id,
            title = Messages.get(MessageKeys.EDITOR_TAB_TITLE, number),
            image = image,
        )
        activeTabId = id
        touchHistory()
    }

    fun closeTab(id: Long) {
        val remaining = tabs.filter { it.id != id }
        tabs = remaining
        histories.remove(id)
        activeTabId = when {
            remaining.isEmpty() -> null
            activeTabId == id -> remaining.last().id
            else -> activeTabId
        }
        touchHistory()
    }

    fun selectTab(id: Long) {
        if (tabs.any { it.id == id }) {
            activeTabId = id
            touchHistory()
        }
    }

    fun updateAnnotations(tabId: Long, annotations: List<EditorAnnotation>) {
        val current = tabs.firstOrNull { it.id == tabId } ?: return
        if (current.annotations == annotations) return
        historyFor(tabId).recordChange(current.annotations)
        tabs = tabs.map { tab ->
            if (tab.id == tabId) tab.copy(annotations = annotations) else tab
        }
        touchHistory()
    }

    fun canUndo(tabId: Long): Boolean = histories[tabId]?.canUndo == true

    fun canRedo(tabId: Long): Boolean = histories[tabId]?.canRedo == true

    fun undo(tabId: Long): Boolean {
        val current = tabs.firstOrNull { it.id == tabId } ?: return false
        val previous = historyFor(tabId).undo(current.annotations) ?: return false
        tabs = tabs.map { tab ->
            if (tab.id == tabId) tab.copy(annotations = previous) else tab
        }
        touchHistory()
        return true
    }

    fun redo(tabId: Long): Boolean {
        val current = tabs.firstOrNull { it.id == tabId } ?: return false
        val next = historyFor(tabId).redo(current.annotations) ?: return false
        tabs = tabs.map { tab ->
            if (tab.id == tabId) tab.copy(annotations = next) else tab
        }
        touchHistory()
        return true
    }

    private fun historyFor(tabId: Long): AnnotationHistory =
        histories.getOrPut(tabId) { AnnotationHistory() }

    private fun touchHistory() {
        historyRevision += 1
    }
}
