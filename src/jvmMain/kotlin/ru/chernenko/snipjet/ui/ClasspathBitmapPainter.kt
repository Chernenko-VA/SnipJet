package ru.chernenko.snipjet.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

internal fun loadClasspathBitmapPainter(resourcePath: String): Painter {
    val bytes = Thread.currentThread().contextClassLoader
        .getResourceAsStream(resourcePath)
        ?.readBytes()
        ?: error("Missing resource $resourcePath")
    return BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
}

@Composable
internal fun rememberClasspathBitmapPainter(resourcePath: String): Painter =
    remember(resourcePath) { loadClasspathBitmapPainter(resourcePath) }
