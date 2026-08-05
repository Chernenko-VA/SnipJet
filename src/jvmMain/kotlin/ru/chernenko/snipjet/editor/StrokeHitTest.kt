package ru.chernenko.snipjet.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

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

fun textHitByEraser(
    text: TextAnnotation,
    eraserPoint: Offset,
    eraserRadiusPx: Float,
): Boolean {
    val bounds = approximateTextBounds(text).inflate(eraserRadiusPx)
    return bounds.contains(eraserPoint)
}

fun annotationHitByEraser(
    annotation: EditorAnnotation,
    eraserPoint: Offset,
    eraserRadiusPx: Float,
): Boolean = when (annotation) {
    is StrokeAnnotation -> strokeHitByEraser(annotation, eraserPoint, eraserRadiusPx)
    is TextAnnotation -> textHitByEraser(annotation, eraserPoint, eraserRadiusPx)
}

/**
 * Removes annotations under the eraser path using paint order: at each sample only the
 * topmost (last drawn) hit counts.
 */
fun eraseAnnotationsAlongPath(
    annotations: List<EditorAnnotation>,
    eraserPath: List<Offset>,
    eraserRadiusPx: Float,
): List<EditorAnnotation> {
    if (eraserPath.isEmpty() || annotations.isEmpty()) return annotations
    val removeIndices = linkedSetOf<Int>()
    for (point in eraserPath) {
        val hitIndex = annotations.indexOfLast { annotation ->
            annotationHitByEraser(annotation, point, eraserRadiusPx)
        }
        if (hitIndex >= 0) {
            removeIndices.add(hitIndex)
        }
    }
    if (removeIndices.isEmpty()) return annotations
    return annotations.filterIndexed { index, _ -> index !in removeIndices }
}

fun approximateTextBounds(text: TextAnnotation): Rect {
    val lines = text.text.split('\n')
    val lineHeight = text.sizePx * TextLineHeightFactor
    val maxChars = lines.maxOfOrNull { it.length } ?: 0
    val width = (maxChars * text.sizePx * 0.55f).coerceAtLeast(text.sizePx)
    val height = lineHeight * lines.size.coerceAtLeast(1)
    return Rect(
        left = text.position.x,
        top = text.position.y - text.sizePx,
        right = text.position.x + width,
        bottom = text.position.y - text.sizePx + height,
    )
}

private fun Rect.inflate(amount: Float): Rect = Rect(
    left = left - amount,
    top = top - amount,
    right = right + amount,
    bottom = bottom + amount,
)

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
