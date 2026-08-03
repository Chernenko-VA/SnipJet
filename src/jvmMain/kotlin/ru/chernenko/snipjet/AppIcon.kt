package ru.chernenko.snipjet

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

fun loadAppIcon(): Painter {
    val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("icon.png")
        ?: error("Missing resource icon.png")
    val bytes = stream.use { it.readBytes() }
    return BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
}
