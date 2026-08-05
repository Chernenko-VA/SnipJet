package ru.chernenko.snipjet.editor

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.nio.file.Files
import java.nio.file.Path

fun loadPngImageBitmap(bytes: ByteArray): ImageBitmap =
    Image.makeFromEncoded(bytes).toComposeImageBitmap()

fun loadPngImageBitmap(path: Path): ImageBitmap =
    loadPngImageBitmap(Files.readAllBytes(path))
