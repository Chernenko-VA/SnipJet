package ru.chernenko.snipjet.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter

enum class EditorTool {
    Pen,
    Marker,
    Eraser,
    Text,
}

private val toolIconResource: Map<EditorTool, String> = mapOf(
    EditorTool.Pen to "icon/pen.svg",
    EditorTool.Marker to "icon/marker.svg",
    EditorTool.Eraser to "icon/eraser.svg",
    EditorTool.Text to "icon/text.svg",
)

@Composable
fun rememberToolIcon(tool: EditorTool): Painter {
    val density = LocalDensity.current
    return remember(tool, density) {
        val path = toolIconResource.getValue(tool)
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(path)
            ?: error("Missing resource $path")
        stream.use { loadSvgPainter(it, density) }
    }
}
