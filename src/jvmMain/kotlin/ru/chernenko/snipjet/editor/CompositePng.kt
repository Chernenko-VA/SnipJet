package ru.chernenko.snipjet.editor

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin
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
                    canvas.drawAnnotation(annotation, strokePaint, fillPaint)
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
