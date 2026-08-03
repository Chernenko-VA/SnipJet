package ru.chernenko.snipjet.clipboard

import java.nio.file.Path

interface ImageClipboard {
    fun isAvailable(): Boolean
    fun copyPngFile(path: Path)
    fun copyPngBytes(pngBytes: ByteArray)
}
