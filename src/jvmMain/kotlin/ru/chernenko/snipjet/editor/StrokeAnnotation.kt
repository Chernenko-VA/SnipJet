package ru.chernenko.snipjet.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class StrokeAnnotation(
    val points: List<Offset>,
    val color: Color,
    val widthPx: Float,
)
