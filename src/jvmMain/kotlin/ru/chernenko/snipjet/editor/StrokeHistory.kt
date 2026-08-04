package ru.chernenko.snipjet.editor

/**
 * Per-tab undo/redo for stroke lists. Keeps at most [maxUndo] past states.
 */
class StrokeHistory(
    private val maxUndo: Int = MaxUndoEntries,
) {
    private val undoStack = ArrayDeque<List<StrokeAnnotation>>()
    private val redoStack = ArrayDeque<List<StrokeAnnotation>>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /**
     * Records [previous] before applying a new strokes list.
     * Clears redo. Drops oldest undo entries beyond [maxUndo].
     */
    fun recordChange(previous: List<StrokeAnnotation>) {
        undoStack.addLast(previous)
        while (undoStack.size > maxUndo) {
            undoStack.removeFirst()
        }
        redoStack.clear()
    }

    /**
     * Pops undo: returns the strokes to restore, after pushing [current] onto redo.
     */
    fun undo(current: List<StrokeAnnotation>): List<StrokeAnnotation>? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        return previous
    }

    /**
     * Pops redo: returns the strokes to restore, after pushing [current] onto undo.
     */
    fun redo(current: List<StrokeAnnotation>): List<StrokeAnnotation>? {
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
