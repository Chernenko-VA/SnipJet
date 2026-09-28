package ru.chernenko.snipjet.editor

import androidx.compose.ui.graphics.toArgb
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PathBuilder

const val TextLineHeightFactor = 1.2f

fun Canvas.drawStrokeAnnotation(
    stroke: StrokeAnnotation,
    paint: Paint,
    scaleX: Float = 1f,
    scaleY: Float = 1f,
) {
    if (stroke.points.size < 2) return
    paint.color = stroke.color.toArgb()
    paint.strokeWidth = stroke.widthPx * ((scaleX + scaleY) / 2f)
    val builder = PathBuilder()
    val first = stroke.points.first()
    builder.moveTo(first.x * scaleX, first.y * scaleY)
    for (i in 1 until stroke.points.size) {
        val point = stroke.points[i]
        builder.lineTo(point.x * scaleX, point.y * scaleY)
    }
    builder.detach().use { path ->
        drawPath(path, paint)
    }
}

fun Canvas.drawTextAnnotation(
    text: TextAnnotation,
    fillPaint: Paint,
    underlinePaint: Paint,
    scaleX: Float = 1f,
    scaleY: Float = 1f,
) {
    if (text.text.isEmpty()) return
    val scale = (scaleX + scaleY) / 2f
    val sizePx = text.sizePx * scale
    val lineHeight = sizePx * TextLineHeightFactor
    val typeface = matchTypeface(text.fontFamily, text.bold, text.italic)
    val font = Font(typeface, sizePx)
    val originX = text.position.x * scaleX
    val originY = text.position.y * scaleY
    try {
        fillPaint.color = text.color.toArgb()
        val lines = text.text.split('\n')
        lines.forEachIndexed { index, line ->
            val y = originY + index * lineHeight
            drawString(line, originX, y, font, fillPaint)
            if (text.underline && line.isNotEmpty()) {
                val width = font.measureTextWidth(line)
                underlinePaint.mode = PaintMode.STROKE
                underlinePaint.strokeWidth = (sizePx * 0.08f).coerceAtLeast(1f)
                underlinePaint.color = text.color.toArgb()
                val underlineY = y + sizePx * 0.12f
                drawLine(
                    originX,
                    underlineY,
                    originX + width,
                    underlineY,
                    underlinePaint,
                )
            }
        }
    } finally {
        font.close()
        typeface.close()
    }
}

fun Canvas.drawShapeAnnotation(
    shape: ShapeAnnotation,
    strokePaint: Paint,
    fillPaint: Paint,
    scaleX: Float = 1f,
    scaleY: Float = 1f,
) {
    val startX = shape.start.x * scaleX
    val startY = shape.start.y * scaleY
    val endX = shape.end.x * scaleX
    val endY = shape.end.y * scaleY
    val colorArgb = shape.color.toArgb()
    val strokeWidth = shape.widthPx * ((scaleX + scaleY) / 2f)

    strokePaint.color = colorArgb
    strokePaint.strokeWidth = strokeWidth
    fillPaint.color = colorArgb

    when (shape.kind) {
        ShapeKind.Line -> {
            drawLine(startX, startY, endX, endY, strokePaint)
        }
        ShapeKind.Arrow -> {
            val arrow = shapeArrowGeometry(shape.start, shape.end, shape.widthPx)
            drawLine(
                startX,
                startY,
                arrow.shaftEnd.x * scaleX,
                arrow.shaftEnd.y * scaleY,
                strokePaint,
            )
            val builder = PathBuilder()
            builder.moveTo(arrow.tip.x * scaleX, arrow.tip.y * scaleY)
            builder.lineTo(arrow.headLeft.x * scaleX, arrow.headLeft.y * scaleY)
            builder.lineTo(arrow.headRight.x * scaleX, arrow.headRight.y * scaleY)
            builder.closePath()
            builder.detach().use { path ->
                if (shape.filled) {
                    drawPath(path, fillPaint)
                }
                drawPath(path, strokePaint)
            }
        }
        ShapeKind.Rectangle -> {
            val left = minOf(startX, endX)
            val top = minOf(startY, endY)
            val right = maxOf(startX, endX)
            val bottom = maxOf(startY, endY)
            if (shape.filled) {
                drawRect(org.jetbrains.skia.Rect.makeLTRB(left, top, right, bottom), fillPaint)
            }
            drawRect(org.jetbrains.skia.Rect.makeLTRB(left, top, right, bottom), strokePaint)
        }
        ShapeKind.Ellipse -> {
            val left = minOf(startX, endX)
            val top = minOf(startY, endY)
            val right = maxOf(startX, endX)
            val bottom = maxOf(startY, endY)
            val oval = org.jetbrains.skia.Rect.makeLTRB(left, top, right, bottom)
            if (shape.filled) {
                drawOval(oval, fillPaint)
            }
            drawOval(oval, strokePaint)
        }
        ShapeKind.Triangle -> {
            val points = shapeTrianglePoints(shape.start, shape.end)
            if (points.size < 3) return
            val builder = PathBuilder()
            builder.moveTo(points[0].x * scaleX, points[0].y * scaleY)
            builder.lineTo(points[1].x * scaleX, points[1].y * scaleY)
            builder.lineTo(points[2].x * scaleX, points[2].y * scaleY)
            // closePath() closes the contour; close() would free the native builder (SIGSEGV).
            builder.closePath()
            builder.detach().use { path ->
                if (shape.filled) {
                    drawPath(path, fillPaint)
                }
                drawPath(path, strokePaint)
            }
        }
    }
}

fun Canvas.drawAnnotation(
    annotation: EditorAnnotation,
    strokePaint: Paint,
    fillPaint: Paint,
    scaleX: Float = 1f,
    scaleY: Float = 1f,
) {
    when (annotation) {
        is StrokeAnnotation -> drawStrokeAnnotation(annotation, strokePaint, scaleX, scaleY)
        is TextAnnotation -> drawTextAnnotation(annotation, fillPaint, strokePaint, scaleX, scaleY)
        is ShapeAnnotation -> drawShapeAnnotation(annotation, strokePaint, fillPaint, scaleX, scaleY)
    }
}
