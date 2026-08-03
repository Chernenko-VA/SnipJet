package ru.chernenko.snipjet.capture

import ru.chernenko.snipjet.config.AppConfig
import ru.chernenko.snipjet.config.MessageKeys
import ru.chernenko.snipjet.config.Messages
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class GnomeScreenshotCapture(
    private val command: String = AppConfig.captureCommand,
    private val timeoutSeconds: Long = AppConfig.captureTimeoutSeconds,
    private val pathCandidates: List<String> = AppConfig.capturePathCandidates,
    private val tempPrefix: String = AppConfig.captureTempPrefix,
) : ScreenCapture {

    override fun isAvailable(): Boolean = resolveCommandPath() != null

    override fun captureArea(): Path {
        val output = Files.createTempFile(tempPrefix, ".png")
        output.toFile().deleteOnExit()
        val executable = resolveCommandPath() ?: command
        try {
            val process = ProcessBuilder(executable, "-a", "-f", output.toAbsolutePath().toString())
                .redirectErrorStream(true)
                .start()

            val stdout = StringBuilder()
            val drain = drainAsync(process.inputStream, stdout)

            val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                drain.join(2_000)
                Files.deleteIfExists(output)
                throw ScreenCaptureException(Messages.get(MessageKeys.ERROR_CAPTURE_TIMEOUT, timeoutSeconds))
            }

            drain.join(5_000)
            val code = process.exitValue()
            val stderr = stdout.toString().trim()
            if (code != 0) {
                Files.deleteIfExists(output)
                val suffix = if (stderr.isNotEmpty()) ": $stderr" else ""
                throw ScreenCaptureException(Messages.get(MessageKeys.ERROR_CAPTURE_EXIT, command, code, suffix))
            }

            if (!Files.exists(output) || Files.size(output) == 0L) {
                Files.deleteIfExists(output)
                throw ScreenCaptureException(Messages.get(MessageKeys.ERROR_CAPTURE_EMPTY))
            }

            return output
        } catch (e: IOException) {
            Files.deleteIfExists(output)
            throw ScreenCaptureException(Messages.get(MessageKeys.ERROR_CAPTURE_MISSING, command), e)
        }
    }

    private fun resolveCommandPath(): String? {
        val fromWhich = which(command)
        if (fromWhich.isNotEmpty()) return fromWhich
        return pathCandidates.firstOrNull { Files.isExecutable(Path.of(it)) }
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
                    if (sink.length < 8_000) {
                        sink.appendLine(line)
                    }
                }
            }
        }.also {
            it.isDaemon = true
            it.start()
        }
    }
}

class ScreenCaptureException(message: String, cause: Throwable? = null) : Exception(message, cause)
