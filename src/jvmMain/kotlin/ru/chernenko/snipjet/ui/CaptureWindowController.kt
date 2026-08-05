package ru.chernenko.snipjet.ui

import androidx.compose.ui.window.WindowState
import java.awt.Point
import java.awt.Window

/** Hides/shows the editor window during a new capture from the editor. */
class CaptureWindowController(
    private val window: Window,
    private val windowState: WindowState,
) {
    private var savedLocation: Point? = null

    fun onVisibilityForCapture(visible: Boolean) {
        if (visible) {
            windowState.isMinimized = false
            try {
                window.opacity = 1f
            } catch (_: Exception) {
                // Translucency may be unsupported.
            }
            val restoreAt = savedLocation
            savedLocation = null
            if (restoreAt != null) {
                window.location = restoreAt
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
    }
}
