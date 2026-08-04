package ru.chernenko.snipjet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ru.chernenko.snipjet.config.AppConfig
import ru.chernenko.snipjet.ui.SnipJetApp
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Toolkit
import javax.swing.SwingUtilities

fun main() = application {
    val statusAlignment = windowAlignment(AppConfig.windowPosition)
    val windowState = rememberWindowState(
        size = DpSize(AppConfig.windowWidthDp.dp, AppConfig.windowHeightDp.dp),
        position = WindowPosition.Aligned(statusAlignment),
    )
    var hiddenForCapture by remember { mutableStateOf(false) }
    var savedLocation by remember { mutableStateOf<Point?>(null) }
    val icon = remember { loadAppIcon() }

    Window(
        onCloseRequest = ::exitApplication,
        title = AppConfig.appTitle,
        state = windowState,
        alwaysOnTop = AppConfig.windowAlwaysOnTop,
        icon = icon,
        resizable = true,
    ) {
        SnipJetApp(
            onVisibilityForCapture = { visible ->
                hiddenForCapture = !visible
                if (visible) {
                    windowState.isMinimized = false
                    try {
                        window.opacity = 1f
                    } catch (_: Exception) {
                        // Translucency may be unsupported.
                    }
                    // Do not restore pre-capture location — editor/status set position themselves.
                    savedLocation = null
                } else {
                    savedLocation = window.location
                    windowState.isMinimized = true
                    try {
                        window.opacity = 0f
                    } catch (_: Exception) {
                        window.location = Point(-32_000, -32_000)
                    }
                }
            },
            onEditorOpen = { open ->
                if (hiddenForCapture) return@SnipJetApp
                if (open) {
                    windowState.placement = WindowPlacement.Floating
                    windowState.size = editorScreenDpSize()
                    windowState.position = WindowPosition.Aligned(Alignment.Center)
                    SwingUtilities.invokeLater {
                        centerOnScreen(window)
                    }
                } else {
                    windowState.placement = WindowPlacement.Floating
                    windowState.size = DpSize(AppConfig.windowWidthDp.dp, AppConfig.windowHeightDp.dp)
                    windowState.position = WindowPosition.Aligned(statusAlignment)
                }
            },
            onExit = ::exitApplication,
        )
    }
}

private fun centerOnScreen(window: java.awt.Window) {
    val gc = window.graphicsConfiguration ?: return
    val screen = gc.bounds
    val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
    val availW = screen.width - insets.left - insets.right
    val availH = screen.height - insets.top - insets.bottom
    val x = screen.x + insets.left + (availW - window.width).coerceAtLeast(0) / 2
    val y = screen.y + insets.top + (availH - window.height).coerceAtLeast(0) / 2
    window.setLocation(x, y)
}

private fun editorScreenDpSize(): DpSize {
    val config = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .defaultScreenDevice
        .defaultConfiguration
    val bounds = config.bounds
    val transform = config.defaultTransform
    val scaleX = transform.scaleX.coerceAtLeast(1e-6)
    val scaleY = transform.scaleY.coerceAtLeast(1e-6)
    val widthDp = (bounds.width / scaleX * 2.0 / 3.0).toInt().coerceAtLeast(400)
    val heightDp = (bounds.height / scaleY * 2.0 / 3.0).toInt().coerceAtLeast(300)
    return DpSize(widthDp.dp, heightDp.dp)
}

private fun windowAlignment(name: String): Alignment = when (name) {
    "BottomEnd" -> Alignment.BottomEnd
    "TopStart" -> Alignment.TopStart
    "BottomStart" -> Alignment.BottomStart
    "Center" -> Alignment.Center
    else -> Alignment.TopEnd
}
