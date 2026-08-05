package ru.chernenko.snipjet.platform

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import ru.chernenko.snipjet.config.WindowSettings
import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Toolkit

data class CenterPlacement(
    val composePosition: WindowPosition,
    val pixelLocation: Point,
)

fun primaryScreenConfiguration(): GraphicsConfiguration =
    GraphicsEnvironment.getLocalGraphicsEnvironment()
        .defaultScreenDevice
        .defaultConfiguration

fun centeredPixelLocation(windowWidthPx: Int, windowHeightPx: Int): Point {
    val config = primaryScreenConfiguration()
    val screen = config.bounds
    val insets = Toolkit.getDefaultToolkit().getScreenInsets(config)
    val availW = screen.width - insets.left - insets.right
    val availH = screen.height - insets.top - insets.bottom
    val x = screen.x + insets.left + (availW - windowWidthPx).coerceAtLeast(0) / 2
    val y = screen.y + insets.top + (availH - windowHeightPx).coerceAtLeast(0) / 2
    return Point(x, y)
}

fun centeredDpPosition(dpSize: DpSize): CenterPlacement {
    val config = primaryScreenConfiguration()
    val screen = config.bounds
    val insets = Toolkit.getDefaultToolkit().getScreenInsets(config)
    val transform = config.defaultTransform
    val scaleX = transform.scaleX.coerceAtLeast(1e-6)
    val scaleY = transform.scaleY.coerceAtLeast(1e-6)
    val widthPx = (dpSize.width.value * scaleX).toInt().coerceAtLeast(1)
    val heightPx = (dpSize.height.value * scaleY).toInt().coerceAtLeast(1)
    val availW = screen.width - insets.left - insets.right
    val availH = screen.height - insets.top - insets.bottom
    val x = screen.x + insets.left + (availW - widthPx).coerceAtLeast(0) / 2
    val y = screen.y + insets.top + (availH - heightPx).coerceAtLeast(0) / 2
    val point = Point(x, y)
    return CenterPlacement(
        composePosition = pixelToWindowPosition(point),
        pixelLocation = point,
    )
}

fun pixelToWindowPosition(point: Point): WindowPosition {
    val transform = primaryScreenConfiguration().defaultTransform
    val scaleX = transform.scaleX.coerceAtLeast(1e-6)
    val scaleY = transform.scaleY.coerceAtLeast(1e-6)
    return WindowPosition((point.x / scaleX).dp, (point.y / scaleY).dp)
}

fun editorScreenDpSize(settings: WindowSettings): DpSize {
    if (settings.editorWidthDp > 0 && settings.editorHeightDp > 0) {
        return DpSize(settings.editorWidthDp.dp, settings.editorHeightDp.dp)
    }
    val config = primaryScreenConfiguration()
    val bounds = config.bounds
    val transform = config.defaultTransform
    val scaleX = transform.scaleX.coerceAtLeast(1e-6)
    val scaleY = transform.scaleY.coerceAtLeast(1e-6)
    val widthDp = (bounds.width / scaleX * 2.0 / 3.0).toInt().coerceAtLeast(400)
    val heightDp = (bounds.height / scaleY * 7.0 / 8.0).toInt().coerceAtLeast(300)
    return DpSize(widthDp.dp, heightDp.dp)
}
