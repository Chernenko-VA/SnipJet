package ru.chernenko.snipjet.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import ru.chernenko.snipjet.loadClasspathBitmapPainter

@Composable
internal fun rememberClasspathBitmapPainter(resourcePath: String): Painter =
    remember(resourcePath) { loadClasspathBitmapPainter(resourcePath) }
