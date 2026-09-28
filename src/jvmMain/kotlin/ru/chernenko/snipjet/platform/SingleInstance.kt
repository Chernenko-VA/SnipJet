package ru.chernenko.snipjet.platform

import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Unix-domain socket so a second `--capture` process can ask the running instance
 * to open a new tab instead of starting another JVM window.
 */
object SingleInstance {
    const val COMMAND_CAPTURE: String = "CAPTURE"

    fun socketPath(): Path {
        val runtimeDir = System.getenv("XDG_RUNTIME_DIR")
        return if (!runtimeDir.isNullOrBlank()) {
            Path.of(runtimeDir, "snipjet.sock")
        } else {
            val cache = Path.of(System.getProperty("user.home"), ".cache", "snipjet")
            Files.createDirectories(cache)
            cache.resolve("snipjet.sock")
        }
    }

    /**
     * Opens a client connection to a live primary, or returns null.
     * Deletes a stale socket file when connect fails.
     */
    private fun tryConnectToPrimary(): SocketChannel? {
        val path = socketPath()
        if (!Files.exists(path)) return null
        return try {
            val channel = SocketChannel.open(StandardProtocolFamily.UNIX)
            channel.connect(UnixDomainSocketAddress.of(path))
            channel
        } catch (_: Exception) {
            try {
                Files.deleteIfExists(path)
            } catch (_: Exception) {
                // ignore stale cleanup failures
            }
            null
        }
    }

    /**
     * Delivers [command] to an already-running primary instance.
     * @return true if the command was sent successfully
     */
    fun tryNotifyPrimary(command: String): Boolean {
        val channel = tryConnectToPrimary() ?: return false
        return try {
            channel.use {
                val bytes = "$command\n".toByteArray(StandardCharsets.UTF_8)
                it.write(ByteBuffer.wrap(bytes))
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    class Server(
        private val onCommand: (String) -> Unit,
    ) : AutoCloseable {
        private val running = AtomicBoolean(true)
        private var serverChannel: ServerSocketChannel? = null
        private var acceptThread: Thread? = null

        /**
         * Binds the IPC socket as primary.
         * @return false if another live primary already owns the socket (do not steal it)
         */
        fun start(): Boolean {
            tryConnectToPrimary()?.use {
                // Live primary — leave its socket alone.
                return false
            }

            val path = socketPath()
            path.parent?.let { Files.createDirectories(it) }
            Files.deleteIfExists(path)

            val server = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
            server.bind(UnixDomainSocketAddress.of(path))
            serverChannel = server

            acceptThread = Thread(
                {
                    while (running.get()) {
                        try {
                            server.accept().use { client ->
                                val buf = ByteBuffer.allocate(256)
                                val n = client.read(buf)
                                if (n > 0) {
                                    buf.flip()
                                    val text = StandardCharsets.UTF_8.decode(buf).toString().trim()
                                    if (text.isNotEmpty()) {
                                        onCommand(text)
                                    }
                                }
                            }
                        } catch (_: Exception) {
                            if (!running.get()) break
                        }
                    }
                },
                "snipjet-single-instance",
            ).apply {
                isDaemon = true
                start()
            }
            return true
        }

        override fun close() {
            running.set(false)
            try {
                serverChannel?.close()
            } catch (_: Exception) {
                // ignore
            }
            try {
                Files.deleteIfExists(socketPath())
            } catch (_: Exception) {
                // ignore
            }
        }
    }
}
