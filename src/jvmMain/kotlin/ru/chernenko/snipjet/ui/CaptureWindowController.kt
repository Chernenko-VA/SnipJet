package ru.chernenko.snipjet.ui

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import ru.chernenko.snipjet.SnipJetSession
import ru.chernenko.snipjet.config.WindowSettings
import ru.chernenko.snipjet.platform.centeredDpPosition
import ru.chernenko.snipjet.platform.centeredPixelLocation
import ru.chernenko.snipjet.platform.editorScreenDpSize
import ru.chernenko.snipjet.platform.pixelToWindowPosition
import java.awt.Point
import java.awt.Window
import javax.swing.SwingUtilities

class CaptureWindowController(
    private val window: Window,
    private val windowState: WindowState,
    private val session: SnipJetSession,
    private val statusAlignment: Alignment,
    private val windowSettings: WindowSettings,
) {
    private var hiddenForCapture = false
    private var savedLocation: Point? = null
    private var editorSized = session.editorOpen
    private var centeringEditor = false

    fun onVisibilityForCapture(visible: Boolean) {
        hiddenForCapture = !visible
        if (visible) {
            showWindow()
            val restoreAt = savedLocation
            savedLocation = null
            when {
                restoreAt != null && session.editorOpen -> {
                    SwingUtilities.invokeLater {
                        window.location = restoreAt
                        SwingUtilities.invokeLater { window.location = restoreAt }
                    }
                }
                restoreAt != null -> {
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
                // Translucency not supported — off-screen only for editor re-capture,
                // not visible to user because window is already minimized.
                window.location = Point(-32_000, -32_000)
            }
        }
    }

    fun onEditorOpen(open: Boolean) {
        if (open) {
            if (!editorSized) {
                editorSized = true
                centeringEditor = true
                placeEditorCentered { centeringEditor = false }
            } else {
                windowState.placement = WindowPlacement.Floating
                windowState.size = editorScreenDpSize(windowSettings)
            }
        } else {
            editorSized = false
            centeringEditor = false
            windowState.placement = WindowPlacement.Floating
            windowState.size = DpSize(windowSettings.widthDp.dp, windowSettings.heightDp.dp)
            windowState.position = WindowPosition.Aligned(statusAlignment)
        }
    }

    private fun placeEditorCentered(onDone: () -> Unit) {
        val dpSize = editorScreenDpSize(windowSettings)
        val center = centeredDpPosition(dpSize)
        windowState.placement = WindowPlacement.Floating
        windowState.size = dpSize
        windowState.position = center.composePosition

        fun applyAwtCenter() {
            val w = window.width.coerceAtLeast(1)
            val h = window.height.coerceAtLeast(1)
            val point = centeredPixelLocation(w, h)
            window.location = point
            windowState.position = pixelToWindowPosition(point)
            showWindow()
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

    private fun showWindow() {
        windowState.isMinimized = false
        try {
            window.opacity = 1f
        } catch (_: Exception) {
            // Translucency may be unsupported.
        }
    }
}
