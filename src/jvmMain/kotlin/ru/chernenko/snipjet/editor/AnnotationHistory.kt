package ru.chernenko.snipjet.editor

/**
 * Per-tab undo/redo for annotation lists. Keeps at most [maxUndo] past states.
 */
class AnnotationHistory(
    private val maxUndo: Int = MaxUndoEntries,
) {
    private val undoStack = ArrayDeque<List<EditorAnnotation>>()
    private val redoStack = ArrayDeque<List<EditorAnnotation>>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun recordChange(previous: List<EditorAnnotation>) {
        undoStack.addLast(previous)
        while (undoStack.size > maxUndo) {
            undoStack.removeFirst()
        }
        redoStack.clear()
    }

    fun undo(current: List<EditorAnnotation>): List<EditorAnnotation>? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        return previous
    }

    fun redo(current: List<EditorAnnotation>): List<EditorAnnotation>? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        while (undoStack.size > maxUndo) {
            undoStack.removeFirst()
        }
        return next
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    companion object {
        const val MaxUndoEntries = 10
    }
}
