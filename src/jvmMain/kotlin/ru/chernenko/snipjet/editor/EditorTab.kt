package ru.chernenko.snipjet.editor

import androidx.compose.ui.graphics.ImageBitmap

data class EditorTab(
    val id: Long,
    val title: String,
    val image: ImageBitmap,
    val annotations: List<EditorAnnotation> = emptyList(),
)
