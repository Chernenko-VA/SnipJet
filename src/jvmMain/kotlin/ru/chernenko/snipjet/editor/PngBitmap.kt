package ru.chernenko.snipjet.editor

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import java.nio.file.Files
import java.nio.file.Path

fun loadPngImageBitmap(path: Path): ImageBitmap =
    Files.newInputStream(path).buffered().use { loadImageBitmap(it) }
