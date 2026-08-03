package ru.chernenko.snipjet.clipboard

import java.nio.file.Path

interface ImageClipboard {
    fun copyPngFile(path: Path)
    fun copyPngBytes(pngBytes: ByteArray)
}
