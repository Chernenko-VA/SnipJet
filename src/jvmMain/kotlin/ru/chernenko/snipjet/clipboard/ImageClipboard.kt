package ru.chernenko.snipjet.clipboard

interface ImageClipboard {
    fun isAvailable(): Boolean
    fun copyPngBytes(pngBytes: ByteArray)
}
