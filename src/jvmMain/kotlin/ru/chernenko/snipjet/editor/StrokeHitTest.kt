package ru.chernenko.snipjet.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs

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
    is ShapeAnnotation -> shapeHitByEraser(annotation, eraserPoint, eraserRadiusPx)
}

fun shapeHitByEraser(
    shape: ShapeAnnotation,
    eraserPoint: Offset,
    eraserRadiusPx: Float,
): Boolean {
    val hitRadius = eraserRadiusPx + shape.widthPx / 2f
    val hitRadiusSq = hitRadius * hitRadius
    val end = shape.end

    return when (shape.kind) {
        ShapeKind.Line -> {
            distanceSqPointToSegment(eraserPoint, shape.start, end) <= hitRadiusSq
        }
        ShapeKind.Arrow -> {
            val arrow = shapeArrowGeometry(shape.start, end, shape.widthPx)
            if (distanceSqPointToSegment(eraserPoint, shape.start, arrow.shaftEnd) <= hitRadiusSq) {
                return true
            }
            if (shape.filled && pointInTriangle(eraserPoint, arrow.tip, arrow.headLeft, arrow.headRight)) {
                return true
            }
            val head = listOf(arrow.tip, arrow.headLeft, arrow.headRight)
            for (i in head.indices) {
                val a = head[i]
                val b = head[(i + 1) % head.size]
                if (distanceSqPointToSegment(eraserPoint, a, b) <= hitRadiusSq) return true
            }
            false
        }
        ShapeKind.Rectangle -> {
            val (topLeft, bottomRight) = shapeBounds(shape.start, end)
            val rect = Rect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
            if (shape.filled && rect.inflate(eraserRadiusPx).contains(eraserPoint)) {
                true
            } else {
                nearRectOutline(eraserPoint, rect, hitRadiusSq)
            }
        }
        ShapeKind.Ellipse -> {
            val (topLeft, bottomRight) = shapeBounds(shape.start, end)
            val cx = (topLeft.x + bottomRight.x) / 2f
            val cy = (topLeft.y + bottomRight.y) / 2f
            val rx = ((bottomRight.x - topLeft.x) / 2f).coerceAtLeast(1f)
            val ry = ((bottomRight.y - topLeft.y) / 2f).coerceAtLeast(1f)
            val nx = (eraserPoint.x - cx) / rx
            val ny = (eraserPoint.y - cy) / ry
            val dist = kotlin.math.sqrt(nx * nx + ny * ny)
            if (shape.filled) {
                dist <= 1f + eraserRadiusPx / minOf(rx, ry)
            } else {
                abs(dist - 1f) * minOf(rx, ry) <= hitRadius
            }
        }
        ShapeKind.Triangle -> {
            val points = shapeTrianglePoints(shape.start, end)
            if (points.size < 3) return false
            if (shape.filled && pointInTriangle(eraserPoint, points[0], points[1], points[2])) {
                return true
            }
            for (i in points.indices) {
                val a = points[i]
                val b = points[(i + 1) % points.size]
                if (distanceSqPointToSegment(eraserPoint, a, b) <= hitRadiusSq) return true
            }
            false
        }
    }
}

private fun nearRectOutline(point: Offset, rect: Rect, hitRadiusSq: Float): Boolean {
    val corners = listOf(
        Offset(rect.left, rect.top),
        Offset(rect.right, rect.top),
        Offset(rect.right, rect.bottom),
        Offset(rect.left, rect.bottom),
    )
    for (i in corners.indices) {
        if (distanceSqPointToSegment(point, corners[i], corners[(i + 1) % corners.size]) <= hitRadiusSq) {
            return true
        }
    }
    return false
}

private fun pointInTriangle(p: Offset, a: Offset, b: Offset, c: Offset): Boolean {
    val v0x = c.x - a.x
    val v0y = c.y - a.y
    val v1x = b.x - a.x
    val v1y = b.y - a.y
    val v2x = p.x - a.x
    val v2y = p.y - a.y
    val dot00 = v0x * v0x + v0y * v0y
    val dot01 = v0x * v1x + v0y * v1y
    val dot02 = v0x * v2x + v0y * v2y
    val dot11 = v1x * v1x + v1y * v1y
    val dot12 = v1x * v2x + v1y * v2y
    val denom = dot00 * dot11 - dot01 * dot01
    if (abs(denom) < 1e-6f) return false
    val u = (dot11 * dot02 - dot01 * dot12) / denom
    val v = (dot00 * dot12 - dot01 * dot02) / denom
    return u >= 0f && v >= 0f && (u + v) <= 1f
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
