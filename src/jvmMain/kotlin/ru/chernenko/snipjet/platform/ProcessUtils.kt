package ru.chernenko.snipjet.platform

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

internal fun which(command: String): String {
    return try {
        val process = ProcessBuilder("which", command).redirectErrorStream(true).start()
        val out = StringBuilder()
        val drain = drainProcessOutput(process.inputStream, out)
        val code = process.waitFor()
        drain.join(2_000)
        if (code == 0 && out.isNotBlank()) out.toString().trim() else ""
    } catch (_: Exception) {
        ""
    }
}

internal fun drainProcessOutput(stream: InputStream, sink: StringBuilder): Thread {
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

internal fun resolveExecutable(command: String, candidates: List<String>): String? {
    val fromWhich = which(command)
    if (fromWhich.isNotEmpty()) return fromWhich
    return candidates.firstOrNull { Files.isExecutable(Path.of(it)) }
}
