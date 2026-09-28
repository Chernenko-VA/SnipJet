package ru.chernenko.snipjet.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

sealed interface EditorAnnotation

data class StrokeAnnotation(
    val points: List<Offset>,
    val color: Color,
    val widthPx: Float,
) : EditorAnnotation

data class TextAnnotation(
    val position: Offset,
    val text: String,
    val color: Color,
    val sizePx: Float,
    val fontFamily: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
) : EditorAnnotation

enum class ShapeKind {
    Line,
    Arrow,
    Rectangle,
    Ellipse,
    Triangle,
}

data class ShapeAnnotation(
    val kind: ShapeKind,
    val start: Offset,
    val end: Offset,
    val color: Color,
    val widthPx: Float,
    val filled: Boolean,
) : EditorAnnotation

const val DefaultTextFontFamily = "Courier New"

/** Constrains [end] relative to [start] when Shift is held (square / circle / isosceles AABB). */
fun constrainShapeEnd(
    kind: ShapeKind,
    start: Offset,
    end: Offset,
    shiftPressed: Boolean,
): Offset {
    if (!shiftPressed || kind == ShapeKind.Line || kind == ShapeKind.Arrow) return end
    val dx = end.x - start.x
    val dy = end.y - start.y
    val side = max(abs(dx), abs(dy))
    val sx = if (dx >= 0f) side else -side
    val sy = if (dy >= 0f) side else -side
    return Offset(start.x + sx, start.y + sy)
}

fun shapeBounds(start: Offset, end: Offset): Pair<Offset, Offset> {
    val left = min(start.x, end.x)
    val top = min(start.y, end.y)
    val right = max(start.x, end.x)
    val bottom = max(start.y, end.y)
    return Offset(left, top) to Offset(right, bottom)
}

/** Apex at top-center, base along bottom edge of the AABB. */
fun shapeTrianglePoints(start: Offset, end: Offset): List<Offset> {
    val (topLeft, bottomRight) = shapeBounds(start, end)
    val midX = (topLeft.x + bottomRight.x) / 2f
    return listOf(
        Offset(midX, topLeft.y),
        Offset(bottomRight.x, bottomRight.y),
        Offset(topLeft.x, bottomRight.y),
    )
}

/**
 * Arrow from [start] to [end]: shaft ends at the head base; tip is [end].
 * Head size scales with [widthPx], capped so a short drag still looks like an arrow.
 */
data class ArrowGeometry(
    val shaftEnd: Offset,
    val tip: Offset,
    val headLeft: Offset,
    val headRight: Offset,
)

fun shapeArrowGeometry(start: Offset, end: Offset, widthPx: Float): ArrowGeometry {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val ux = dx / length
    val uy = dy / length
    val px = -uy
    val py = ux
    val maxHead = (length * 0.45f).coerceAtLeast(1f)
    val headLength = (widthPx * 4f).coerceIn(1f, maxHead)
    val headHalf = headLength * 0.45f
    val shaftEnd = Offset(end.x - ux * headLength, end.y - uy * headLength)
    return ArrowGeometry(
        shaftEnd = shaftEnd,
        tip = end,
        headLeft = Offset(shaftEnd.x + px * headHalf, shaftEnd.y + py * headHalf),
        headRight = Offset(shaftEnd.x - px * headHalf, shaftEnd.y - py * headHalf),
    )
}
