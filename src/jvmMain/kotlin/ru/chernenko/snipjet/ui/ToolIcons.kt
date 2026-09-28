package ru.chernenko.snipjet.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import ru.chernenko.snipjet.editor.ShapeKind

enum class EditorTool {
    Pen,
    Marker,
    Eraser,
    Text,
    Shapes,
}

private val toolIconResource: Map<EditorTool, String> = mapOf(
    EditorTool.Pen to "icon/pen.png",
    EditorTool.Marker to "icon/marker.png",
    EditorTool.Eraser to "icon/eraser.png",
    EditorTool.Text to "icon/text.png",
    EditorTool.Shapes to "icon/shapes.png",
)

private val shapeKindIconResource: Map<ShapeKind, String> = mapOf(
    ShapeKind.Line to "icon/line.png",
    ShapeKind.Arrow to "icon/arrow.png",
    ShapeKind.Rectangle to "icon/rectangle.png",
    ShapeKind.Ellipse to "icon/circle.png",
    ShapeKind.Triangle to "icon/triangle.png",
)

@Composable
fun rememberToolIcon(tool: EditorTool): Painter =
    rememberClasspathBitmapPainter(toolIconResource.getValue(tool))

@Composable
fun rememberShapeKindIcon(kind: ShapeKind): Painter =
    rememberClasspathBitmapPainter(shapeKindIconResource.getValue(kind))
