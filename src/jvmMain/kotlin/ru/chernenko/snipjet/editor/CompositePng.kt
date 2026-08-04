package ru.chernenko.snipjet.editor

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Surface

/**
 * Renders [image] with [strokes] at image pixel size and returns PNG bytes.
 */
fun composeAnnotatedPng(
    image: ImageBitmap,
    strokes: List<StrokeAnnotation>,
): ByteArray {
    val width = image.width
    val height = image.height
    require(width > 0 && height > 0) { "Image must be non-empty" }

    Surface.makeRasterN32Premul(width, height).use { surface ->
        val canvas = surface.canvas
        Image.makeFromBitmap(image.asSkiaBitmap()).use { skiaImage ->
            canvas.drawImage(skiaImage, 0f, 0f)
        }

        val paint = Paint().apply {
            mode = PaintMode.STROKE
            strokeCap = PaintStrokeCap.ROUND
            strokeJoin = PaintStrokeJoin.ROUND
            isAntiAlias = true
        }
        paint.use {
            for (stroke in strokes) {
                if (stroke.points.size < 2) continue
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
