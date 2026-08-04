package ru.chernenko.snipjet.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

sealed interface EditorAnnotation

data class StrokeAnnotation(
    val points: List<Offset>,
    val color: Color,
    val widthPx: Float,
) : EditorAnnotation

data class TextAnnotation(
    val position: Offset,
    val text: String,
    val color: Color,
    val sizePx: Float,
    val fontFamily: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
) : EditorAnnotation

const val DefaultTextFontFamily = "Courier New"
