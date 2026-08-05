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
    }
}
