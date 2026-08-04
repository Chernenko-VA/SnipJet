package ru.chernenko.snipjet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import ru.chernenko.snipjet.config.MessageKeys
import ru.chernenko.snipjet.config.Messages
import ru.chernenko.snipjet.editor.EditorTab
import ru.chernenko.snipjet.editor.StrokeAnnotation

/**
 * Editor session kept outside Compose so tabs survive window/composition recreation
 * (e.g. after long idle / WM suspend on Linux).
 */
class SnipJetSession {
    var tabs by mutableStateOf<List<EditorTab>>(emptyList())
        private set
    var activeTabId by mutableStateOf<Long?>(null)
        private set

    private var nextTabId by mutableLongStateOf(1L)
    private var nextTabNumber by mutableIntStateOf(1)

    val editorOpen: Boolean get() = tabs.isNotEmpty()

    val activeTab: EditorTab?
        get() = tabs.firstOrNull { it.id == activeTabId } ?: tabs.lastOrNull()

    fun openTab(image: ImageBitmap) {
        val id = nextTabId
        nextTabId += 1
        val number = nextTabNumber
        nextTabNumber += 1
        tabs = tabs + EditorTab(
            id = id,
            title = Messages.get(MessageKeys.EDITOR_TAB_TITLE, number),
            image = image,
        )
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

    fun selectTab(id: Long) {
        if (tabs.any { it.id == id }) {
            activeTabId = id
        }
    }

    fun updateStrokes(tabId: Long, strokes: List<StrokeAnnotation>) {
        tabs = tabs.map { tab ->
            if (tab.id == tabId) tab.copy(strokes = strokes) else tab
        }
    }
}
