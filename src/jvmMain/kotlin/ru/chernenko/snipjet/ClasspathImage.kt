package ru.chernenko.snipjet

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

fun loadClasspathBitmapPainter(resourcePath: String): Painter {
    val bytes = Thread.currentThread().contextClassLoader
        .getResourceAsStream(resourcePath)
        ?.readBytes()
        ?: error("Missing resource $resourcePath")
    return BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
}
