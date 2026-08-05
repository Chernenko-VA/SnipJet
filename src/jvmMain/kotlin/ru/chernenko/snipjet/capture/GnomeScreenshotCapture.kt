package ru.chernenko.snipjet.capture

import ru.chernenko.snipjet.config.AppConfig
import ru.chernenko.snipjet.config.MessageKeys
import ru.chernenko.snipjet.config.Messages
import ru.chernenko.snipjet.platform.drainProcessOutput
import ru.chernenko.snipjet.platform.resolveExecutable
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class GnomeScreenshotCapture(
    private val command: String = AppConfig.settings.capture.command,
    private val timeoutSeconds: Long = AppConfig.settings.capture.timeoutSeconds,
    private val pathCandidates: List<String> = AppConfig.settings.capture.pathCandidates,
    private val tempPrefix: String = AppConfig.settings.capture.tempPrefix,
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
            val drain = drainProcessOutput(process.inputStream, stdout)

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

    private fun resolveCommandPath(): String? =
        resolveExecutable(command, pathCandidates)
}

class ScreenCaptureException(message: String, cause: Throwable? = null) : Exception(message, cause)
