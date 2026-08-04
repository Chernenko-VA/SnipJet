package ru.chernenko.snipjet.editor

import androidx.compose.ui.geometry.Offset

/**
 * Returns true if [eraserPoint] (with [eraserRadiusPx]) hits the stroke polyline,
 * accounting for half of the stroke width.
 */
fun strokeHitByEraser(
    stroke: StrokeAnnotation,
    eraserPoint: Offset,
    eraserRadiusPx: Float,
): Boolean {
    val points = stroke.points
    if (points.isEmpty()) return false
    val hitRadius = eraserRadiusPx + stroke.widthPx / 2f
    val hitRadiusSq = hitRadius * hitRadius

    if (points.size == 1) {
        return distanceSq(points[0], eraserPoint) <= hitRadiusSq
    }

    for (i in 0 until points.lastIndex) {
        if (distanceSqPointToSegment(eraserPoint, points[i], points[i + 1]) <= hitRadiusSq) {
            return true
        }
    }
    return false
}

/**
 * Removes strokes under the eraser path using paint order: at each sample only the
 * topmost (last drawn) hit counts. Overlapping strokes below are not erased until
 * the upper one is gone — so a click on a stack removes the visible top stroke.
 */
fun eraseStrokesAlongPath(
    strokes: List<StrokeAnnotation>,
    eraserPath: List<Offset>,
    eraserRadiusPx: Float,
): List<StrokeAnnotation> {
    if (eraserPath.isEmpty() || strokes.isEmpty()) return strokes
    val removeIndices = linkedSetOf<Int>()
    for (point in eraserPath) {
        val hitIndex = strokes.indexOfLast { stroke ->
            strokeHitByEraser(stroke, point, eraserRadiusPx)
        }
        if (hitIndex >= 0) {
            removeIndices.add(hitIndex)
        }
    }
    if (removeIndices.isEmpty()) return strokes
    return strokes.filterIndexed { index, _ -> index !in removeIndices }
}

private fun distanceSq(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return dx * dx + dy * dy
}

private fun distanceSqPointToSegment(point: Offset, a: Offset, b: Offset): Float {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val lengthSq = abx * abx + aby * aby
    if (lengthSq < 1e-6f) return distanceSq(point, a)

    val t = ((point.x - a.x) * abx + (point.y - a.y) * aby) / lengthSq
    val clamped = t.coerceIn(0f, 1f)
    val closest = Offset(a.x + abx * clamped, a.y + aby * clamped)
    return distanceSq(point, closest)
}
