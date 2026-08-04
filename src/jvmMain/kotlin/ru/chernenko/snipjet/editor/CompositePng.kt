package ru.chernenko.snipjet.editor

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Font
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Surface

/**
 * Renders [image] with [annotations] at image pixel size and returns PNG bytes.
 */
fun composeAnnotatedPng(
    image: ImageBitmap,
    annotations: List<EditorAnnotation>,
): ByteArray {
    val width = image.width
    val height = image.height
    require(width > 0 && height > 0) { "Image must be non-empty" }

    Surface.makeRasterN32Premul(width, height).use { surface ->
        val canvas = surface.canvas
        Image.makeFromBitmap(image.asSkiaBitmap()).use { skiaImage ->
            canvas.drawImage(skiaImage, 0f, 0f)
        }

        val strokePaint = Paint().apply {
            mode = PaintMode.STROKE
            strokeCap = PaintStrokeCap.ROUND
            strokeJoin = PaintStrokeJoin.ROUND
            isAntiAlias = true
        }
        val fillPaint = Paint().apply {
            mode = PaintMode.FILL
            isAntiAlias = true
        }

        strokePaint.use {
            fillPaint.use {
                for (annotation in annotations) {
                    when (annotation) {
                        is StrokeAnnotation -> drawStroke(canvas, annotation, strokePaint)
                        is TextAnnotation -> drawText(canvas, annotation, fillPaint, strokePaint)
                    }
                }
            }
        }

        val snapshot = surface.makeImageSnapshot()
            ?: error("Failed to snapshot composed image")
        snapshot.use { encoded ->
            val data = encoded.encodeToData(EncodedImageFormat.PNG)
                ?: error("Failed to encode composed PNG")
            return data.bytes
        }
    }
}

private fun drawStroke(
    canvas: org.jetbrains.skia.Canvas,
    stroke: StrokeAnnotation,
    paint: Paint,
) {
    if (stroke.points.size < 2) return
    paint.color = stroke.color.toArgb()
    paint.strokeWidth = stroke.widthPx
    val builder = PathBuilder()
    val first = stroke.points.first()
    builder.moveTo(first.x, first.y)
    for (i in 1 until stroke.points.size) {
        val p = stroke.points[i]
        builder.lineTo(p.x, p.y)
    }
    builder.detach().use { path ->
        canvas.drawPath(path, paint)
    }
}

private fun drawText(
    canvas: org.jetbrains.skia.Canvas,
    text: TextAnnotation,
    fillPaint: Paint,
    underlinePaint: Paint,
) {
    if (text.text.isEmpty()) return
    val typeface = matchTypeface(text.fontFamily, text.bold, text.italic)
    val font = Font(typeface, text.sizePx)
    val lineHeight = text.sizePx * 1.2f
    try {
        fillPaint.color = text.color.toArgb()
        val lines = text.text.split('\n')
        lines.forEachIndexed { index, line ->
            val y = text.position.y + index * lineHeight
            canvas.drawString(line, text.position.x, y, font, fillPaint)
            if (text.underline && line.isNotEmpty()) {
                val width = font.measureTextWidth(line)
                underlinePaint.mode = PaintMode.STROKE
                underlinePaint.strokeWidth = (text.sizePx * 0.08f).coerceAtLeast(1f)
                underlinePaint.color = text.color.toArgb()
                val underlineY = y + text.sizePx * 0.12f
                canvas.drawLine(
                    text.position.x,
                    underlineY,
                    text.position.x + width,
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
