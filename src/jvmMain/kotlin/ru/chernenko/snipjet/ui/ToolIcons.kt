package ru.chernenko.snipjet.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

enum class EditorTool {
    Pen,
    Marker,
    Eraser,
    Text,
}

private val toolIconResource: Map<EditorTool, String> = mapOf(
    EditorTool.Pen to "icon/pen.png",
    EditorTool.Marker to "icon/marker.png",
    EditorTool.Eraser to "icon/eraser.png",
    EditorTool.Text to "icon/text.png",
)

@Composable
fun rememberToolIcon(tool: EditorTool): Painter =
    rememberClasspathBitmapPainter(toolIconResource.getValue(tool))
