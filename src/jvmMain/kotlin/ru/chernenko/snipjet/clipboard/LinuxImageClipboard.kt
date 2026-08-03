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
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

class LinuxImageClipboard : ImageClipboard {

    override fun copyPngFile(path: Path) {
        copyPngBytes(Files.readAllBytes(path))
    }

    override fun copyPngBytes(pngBytes: ByteArray) {
        ImageIO.read(ByteArrayInputStream(pngBytes))
            ?: throw ClipboardException(Messages.get(MessageKeys.ERROR_CLIPBOARD_DECODE))

        // AWT setContents often "succeeds" on Wayland without actually publishing the image.
        if (isWayland()) {
            copyViaWlCopy(pngBytes)
            return
        }

        val image = ImageIO.read(ByteArrayInputStream(pngBytes))!!
        try {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(ImageTransferable(image), null)
        } catch (_: Exception) {
            copyViaWlCopy(pngBytes)
        }
    }

    private fun isWayland(): Boolean =
        !System.getenv("WAYLAND_DISPLAY").isNullOrBlank()

    private fun copyViaWlCopy(pngBytes: ByteArray) {
        val process = try {
            ProcessBuilder(
                AppConfig.clipboardWlCopyCommand,
                AppConfig.clipboardWlCopyTypeFlag,
                AppConfig.clipboardWlCopyMime,
            )
                .redirectErrorStream(true)
                .start()
        } catch (e: Exception) {
            throw ClipboardException(Messages.get(MessageKeys.ERROR_CLIPBOARD_UNAVAILABLE), e)
        }

        val stdout = StringBuilder()
        val reader = Thread {
            process.inputStream.bufferedReader().use { br ->
                br.forEachLine { stdout.appendLine(it) }
            }
        }.also { it.isDaemon = true; it.start() }

        process.outputStream.use { it.write(pngBytes) }
        val code = process.waitFor()
        reader.join(5_000)
        if (code != 0) {
            throw ClipboardException(
                Messages.get(MessageKeys.ERROR_CLIPBOARD_WL_COPY_FAILED, code, stdout.toString().trim())
            )
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
