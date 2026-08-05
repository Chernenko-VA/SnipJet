package ru.chernenko.snipjet.capture

import java.nio.file.Path

interface ScreenCapture {
    fun isAvailable(): Boolean

    /**
     * Interactive area selection via the desktop environment (Wayland-safe).
     * Returns path to a PNG file; caller owns cleanup.
     */
    fun captureArea(): Path
}
