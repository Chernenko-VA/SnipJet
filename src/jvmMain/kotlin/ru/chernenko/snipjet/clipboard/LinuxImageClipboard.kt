package ru.chernenko.snipjet.clipboard

import ru.chernenko.snipjet.config.AppConfig
import ru.chernenko.snipjet.config.MessageKeys
import ru.chernenko.snipjet.config.Messages
import ru.chernenko.snipjet.platform.drainProcessOutput
import ru.chernenko.snipjet.platform.resolveExecutable
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

class LinuxImageClipboard(
    private val wlCopyCommand: String = AppConfig.settings.clipboard.wlCopyCommand,
    private val wlCopyTypeFlag: String = AppConfig.settings.clipboard.wlCopyTypeFlag,
    private val wlCopyMime: String = AppConfig.settings.clipboard.wlCopyMime,
    private val wlCopyTimeoutSeconds: Long = AppConfig.settings.clipboard.wlCopyTimeoutSeconds,
    private val pathCandidates: List<String> = AppConfig.settings.clipboard.pathCandidates,
) : ImageClipboard {

    override fun isAvailable(): Boolean {
        if (!isWayland()) return true
        return resolveWlCopyPath() != null
    }

    override fun copyPngBytes(pngBytes: ByteArray) {
        if (pngBytes.isEmpty()) {
            throw ClipboardException(Messages.get(MessageKeys.ERROR_CLIPBOARD_DECODE))
        }

        if (isWayland()) {
            copyViaWlCopy(pngBytes)
            return
        }

        val image = ImageIO.read(ByteArrayInputStream(pngBytes))
            ?: throw ClipboardException(Messages.get(MessageKeys.ERROR_CLIPBOARD_DECODE))
        try {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(ImageTransferable(image), null)
        } catch (_: Exception) {
            copyViaWlCopy(pngBytes)
        }
    }

    private fun isWayland(): Boolean =
        !System.getenv("WAYLAND_DISPLAY").isNullOrBlank()

    private fun resolveWlCopyPath(): String? =
        resolveExecutable(wlCopyCommand, pathCandidates)

    private fun copyViaWlCopy(pngBytes: ByteArray) {
        val executable = resolveWlCopyPath()
            ?: throw ClipboardException(Messages.get(MessageKeys.ERROR_CLIPBOARD_UNAVAILABLE))

        val process = try {
            ProcessBuilder(
                executable,
                wlCopyTypeFlag,
                wlCopyMime,
            )
                .redirectErrorStream(true)
                .start()
        } catch (e: Exception) {
            throw ClipboardException(Messages.get(MessageKeys.ERROR_CLIPBOARD_UNAVAILABLE), e)
        }

        val stdout = StringBuilder()
        val reader = drainProcessOutput(process.inputStream, stdout)

        try {
            process.outputStream.use { it.write(pngBytes) }
        } catch (e: Exception) {
            process.destroyForcibly()
            reader.join(2_000)
            throw ClipboardException(Messages.get(MessageKeys.ERROR_CLIPBOARD_UNAVAILABLE), e)
        }

        // Clipboard is ready once stdin is closed. wl-copy often stays alive on Wayland
        // to serve paste clients — do not block the UI waiting for process exit.
        val exitedQuickly = process.waitFor(EarlyExitPollMs, TimeUnit.MILLISECONDS)
        if (exitedQuickly) {
            reader.join(2_000)
            val code = process.exitValue()
            if (code != 0) {
                throw ClipboardException(
                    Messages.get(MessageKeys.ERROR_CLIPBOARD_WL_COPY_FAILED, code, stdout.toString().trim()),
                )
            }
            return
        }

        reapWlCopyInBackground(process, reader)
    }

    private fun reapWlCopyInBackground(process: Process, reader: Thread) {
        Thread {
            try {
                if (!process.waitFor(wlCopyTimeoutSeconds, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                } else {
                    val code = process.exitValue()
                    if (code != 0) {
                        reader.join(2_000)
                        System.err.println(
                            "wl-copy exited with code $code in background",
                        )
                    }
                }
            } catch (e: Exception) {
                process.destroyForcibly()
                System.err.println("wl-copy background reaper error: ${e.message}")
            } finally {
                reader.join(2_000)
            }
        }.apply {
            isDaemon = true
            name = "snipjet-wl-copy-reaper"
            start()
        }
    }

    companion object {
        private const val EarlyExitPollMs = 100L
    }
}

private class ImageTransferable(private val image: BufferedImage) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)
    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.imageFlavor

    override fun getTransferData(flavor: DataFlavor): Any {
        if (!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
        return image as Image
    }
}

class ClipboardException(message: String, cause: Throwable? = null) : Exception(message, cause)
