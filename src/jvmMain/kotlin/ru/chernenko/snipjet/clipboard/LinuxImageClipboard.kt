package ru.chernenko.snipjet.clipboard

import ru.chernenko.snipjet.config.AppConfig
import ru.chernenko.snipjet.config.MessageKeys
import ru.chernenko.snipjet.config.Messages
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

class LinuxImageClipboard(
    private val wlCopyCommand: String = AppConfig.clipboardWlCopyCommand,
    private val pathCandidates: List<String> = AppConfig.clipboardPathCandidates,
) : ImageClipboard {

    override fun isAvailable(): Boolean {
        if (!isWayland()) return true
        return resolveWlCopyPath() != null
    }

    override fun copyPngFile(path: Path) {
        copyPngBytes(Files.readAllBytes(path))
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

    private fun resolveWlCopyPath(): String? {
        val fromWhich = which(wlCopyCommand)
        if (fromWhich.isNotEmpty()) return fromWhich
        return pathCandidates.firstOrNull { Files.isExecutable(Path.of(it)) }
    }

    private fun copyViaWlCopy(pngBytes: ByteArray) {
        val executable = resolveWlCopyPath()
            ?: throw ClipboardException(Messages.get(MessageKeys.ERROR_CLIPBOARD_UNAVAILABLE))

        val process = try {
            ProcessBuilder(
                executable,
                AppConfig.clipboardWlCopyTypeFlag,
                AppConfig.clipboardWlCopyMime,
            )
                .redirectErrorStream(true)
                .start()
        } catch (e: Exception) {
            throw ClipboardException(Messages.get(MessageKeys.ERROR_CLIPBOARD_UNAVAILABLE), e)
        }

        val stdout = StringBuilder()
        val reader = drainAsync(process.inputStream, stdout)

        process.outputStream.use { it.write(pngBytes) }
        val code = process.waitFor()
        reader.join(5_000)
        if (code != 0) {
            throw ClipboardException(
                Messages.get(MessageKeys.ERROR_CLIPBOARD_WL_COPY_FAILED, code, stdout.toString().trim())
            )
        }
    }

    private fun which(cmd: String): String {
        return try {
            val p = ProcessBuilder("which", cmd).redirectErrorStream(true).start()
            val out = StringBuilder()
            val drain = drainAsync(p.inputStream, out)
            val code = p.waitFor()
            drain.join(2_000)
            if (code == 0 && out.isNotBlank()) out.toString().trim() else ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun drainAsync(stream: InputStream, sink: StringBuilder): Thread {
        return Thread {
            stream.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    if (sink.length < 8_000) sink.appendLine(line)
                }
            }
        }.also {
            it.isDaemon = true
            it.start()
        }
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
