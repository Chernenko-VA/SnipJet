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

fun main() {
    val session = SnipJetSession()
    application {
        val statusAlignment = windowAlignment(AppConfig.windowPosition)
        val windowState = rememberWindowState(
            size = DpSize(AppConfig.windowWidthDp.dp, AppConfig.windowHeightDp.dp),
            position = WindowPosition.Aligned(statusAlignment),
        )
        var hiddenForCapture by remember { mutableStateOf(false) }
        var savedLocation by remember { mutableStateOf<Point?>(null) }
        var editorSized by remember { mutableStateOf(false) }
        var centeringEditor by remember { mutableStateOf(false) }
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
                session = session,
                onVisibilityForCapture = { visible ->
                    hiddenForCapture = !visible
                    if (visible) {
                        windowState.isMinimized = false
                        try {
                            window.opacity = 1f
                        } catch (_: Exception) {
                            // Translucency may be unsupported.
                        }
                        val restoreAt = savedLocation
                        savedLocation = null
                        when {
                            restoreAt != null && session.editorOpen -> {
                                // Recapture from editor: keep previous editor placement.
                                SwingUtilities.invokeLater {
                                    window.location = restoreAt
                                    SwingUtilities.invokeLater { window.location = restoreAt }
                                }
                            }
                            restoreAt != null -> {
                                // Status capture finished: restore status only if editor did not open.
                                SwingUtilities.invokeLater {
                                    SwingUtilities.invokeLater {
                                        if (!session.editorOpen && !centeringEditor) {
                                            window.location = restoreAt
                                        }
                                    }
                                }
                            }
                        }
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
                        if (!editorSized) {
                            editorSized = true
                            centeringEditor = true
                            placeEditorCentered(window, windowState) {
                                centeringEditor = false
                            }
                        } else {
                            windowState.placement = WindowPlacement.Floating
                            windowState.size = editorScreenDpSize()
                        }
                    } else {
                        editorSized = false
                        centeringEditor = false
                        windowState.placement = WindowPlacement.Floating
                        windowState.size = DpSize(AppConfig.windowWidthDp.dp, AppConfig.windowHeightDp.dp)
                        windowState.position = WindowPosition.Aligned(statusAlignment)
                    }
                },
                onExit = ::exitApplication,
            )
        }
    }
}

/**
 * Sets editor size and forces a centered Absolute position in both Compose state and AWT.
 * Retries after resize — growing from StatusApp TopEnd otherwise leaves the window on the side.
 */
private fun placeEditorCentered(
    window: java.awt.Window,
    windowState: androidx.compose.ui.window.WindowState,
    onDone: () -> Unit,
) {
    val dpSize = editorScreenDpSize()
    val center = centeredAbsolutePosition(dpSize)
    windowState.placement = WindowPlacement.Floating
    windowState.size = dpSize
    windowState.position = center.composePosition

    fun applyAwtCenter() {
        val w = window.width.coerceAtLeast(1)
        val h = window.height.coerceAtLeast(1)
        val point = centeredPixelLocation(w, h)
        window.location = point
        windowState.position = pixelToWindowPosition(point)
    }

    SwingUtilities.invokeLater {
        applyAwtCenter()
        // Size may apply asynchronously; re-center a few times so WM/XWayland settle.
        intArrayOf(16, 50, 100, 200, 400).forEach { delayMs ->
            javax.swing.Timer(delayMs) {
                applyAwtCenter()
                if (delayMs == 400) onDone()
            }.apply {
                isRepeats = false
                start()
            }
        }
    }
}

private data class CenterPlacement(
    val composePosition: WindowPosition,
    val pixelLocation: Point,
)

private fun centeredAbsolutePosition(dpSize: DpSize): CenterPlacement {
    val config = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .defaultScreenDevice
        .defaultConfiguration
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

private fun centeredPixelLocation(windowWidthPx: Int, windowHeightPx: Int): Point {
    val config = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .defaultScreenDevice
        .defaultConfiguration
    val screen = config.bounds
    val insets = Toolkit.getDefaultToolkit().getScreenInsets(config)
    val availW = screen.width - insets.left - insets.right
    val availH = screen.height - insets.top - insets.bottom
    val x = screen.x + insets.left + (availW - windowWidthPx).coerceAtLeast(0) / 2
    val y = screen.y + insets.top + (availH - windowHeightPx).coerceAtLeast(0) / 2
    return Point(x, y)
}

private fun pixelToWindowPosition(point: Point): WindowPosition {
    val config = GraphicsEnvironment.getLocalGraphicsEnvironment()
        .defaultScreenDevice
        .defaultConfiguration
    val transform = config.defaultTransform
    val scaleX = transform.scaleX.coerceAtLeast(1e-6)
    val scaleY = transform.scaleY.coerceAtLeast(1e-6)
    return WindowPosition((point.x / scaleX).dp, (point.y / scaleY).dp)
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
    val heightDp = (bounds.height / scaleY * 6.0 / 8.0).toInt().coerceAtLeast(300)
    return DpSize(widthDp.dp, heightDp.dp)
}

private fun windowAlignment(name: String): Alignment = when (name) {
    "BottomEnd" -> Alignment.BottomEnd
    "TopStart" -> Alignment.TopStart
    "BottomStart" -> Alignment.BottomStart
    "Center" -> Alignment.Center
    else -> Alignment.TopEnd
}
