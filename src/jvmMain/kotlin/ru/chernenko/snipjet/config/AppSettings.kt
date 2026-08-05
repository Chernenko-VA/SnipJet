package ru.chernenko.snipjet.config

import org.yaml.snakeyaml.Yaml
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

data class AppSettings(
    val app: AppInfo = AppInfo(),
    val window: WindowSettings = WindowSettings(),
    val capture: CaptureSettings = CaptureSettings(),
    val clipboard: ClipboardSettings = ClipboardSettings(),
) {
    fun validate() {
        require(app.title.isNotBlank()) { "app.title must not be blank" }
        window.validate()
        capture.validate()
        clipboard.validate()
    }
}

data class AppInfo(
    val title: String = "SnipJet",
)

data class WindowSettings(
    val widthDp: Int = 480,
    val heightDp: Int = 360,
    /** Fixed editor width; 0 uses a screen fraction at startup. */
    val editorWidthDp: Int = 0,
    /** Fixed editor height; 0 uses a screen fraction at startup. */
    val editorHeightDp: Int = 0,
    val alwaysOnTop: Boolean = false,
    val position: String = "TopEnd",
    val hideDelayMs: Long = 300,
) {
    fun validate() {
        require(widthDp in 200..4000) { "window.widthDp must be between 200 and 4000" }
        require(heightDp in 120..3000) { "window.heightDp must be between 120 and 3000" }
        require(editorWidthDp == 0 || editorWidthDp in 400..4000) {
            "window.editorWidthDp must be 0 (auto) or between 400 and 4000"
        }
        require(editorHeightDp == 0 || editorHeightDp in 300..3000) {
            "window.editorHeightDp must be 0 (auto) or between 300 and 3000"
        }
        require((editorWidthDp == 0) == (editorHeightDp == 0)) {
            "window.editorWidthDp and window.editorHeightDp must both be 0 (auto) or both be set"
        }
        require(hideDelayMs in 0..5000) { "window.hideDelayMs must be between 0 and 5000" }
        require(position in ALLOWED_POSITIONS) {
            "window.position must be one of: ${ALLOWED_POSITIONS.joinToString()}"
        }
    }

    companion object {
        private val ALLOWED_POSITIONS = setOf(
            "TopEnd", "BottomEnd", "TopStart", "BottomStart", "Center",
        )
    }
}

data class CaptureSettings(
    val command: String = "gnome-screenshot",
    val timeoutSeconds: Long = 300,
    val tempPrefix: String = "snipjet-",
    val pathCandidates: List<String> = DEFAULT_PATH_CANDIDATES,
) {
    fun validate() {
        require(command.isNotBlank()) { "capture.command must not be blank" }
        require(timeoutSeconds in 1..3600) { "capture.timeoutSeconds must be between 1 and 3600" }
        require(tempPrefix.isNotBlank()) { "capture.tempPrefix must not be blank" }
    }

    companion object {
        private val DEFAULT_PATH_CANDIDATES = listOf(
            "/usr/bin/gnome-screenshot",
            "/bin/gnome-screenshot",
            "/usr/local/bin/gnome-screenshot",
        )
    }
}

data class ClipboardSettings(
    val wlCopyCommand: String = "wl-copy",
    val wlCopyTypeFlag: String = "--type",
    val wlCopyMime: String = "image/png",
    val pathCandidates: List<String> = DEFAULT_PATH_CANDIDATES,
) {
    fun validate() {
        require(wlCopyCommand.isNotBlank()) { "clipboard.wlCopyCommand must not be blank" }
        require(wlCopyTypeFlag.isNotBlank()) { "clipboard.wlCopyTypeFlag must not be blank" }
        require(wlCopyMime.isNotBlank()) { "clipboard.wlCopyMime must not be blank" }
    }

    companion object {
        private val DEFAULT_PATH_CANDIDATES = listOf(
            "/usr/bin/wl-copy",
            "/bin/wl-copy",
            "/usr/local/bin/wl-copy",
        )
    }
}

internal object AppSettingsLoader {
    private const val BUNDLED_RESOURCE = "application.yml"
    private const val USER_CONFIG_DIR = ".config/snipjet"
    private const val USER_CONFIG_FILE = "application.yml"

    fun load(): AppSettings {
        val merged = deepMerge(loadBundledRoot(), loadUserRoot())
        return parseSettings(merged).also { it.validate() }
    }

    private fun loadBundledRoot(): Map<String, Any?> =
        loadYamlMap(Thread.currentThread().contextClassLoader.getResourceAsStream(BUNDLED_RESOURCE))
            ?: error("Missing classpath resource $BUNDLED_RESOURCE")

    private fun loadUserRoot(): Map<String, Any?> {
        val path = userConfigPath()
        if (!Files.isRegularFile(path)) return emptyMap()
        return loadYamlMap(Files.newInputStream(path)) ?: emptyMap()
    }

    private fun userConfigPath(): Path {
        val home = System.getProperty("user.home")?.takeIf { it.isNotBlank() }
            ?: return Path.of(USER_CONFIG_DIR, USER_CONFIG_FILE)
        return Path.of(home, USER_CONFIG_DIR, USER_CONFIG_FILE)
    }

    private fun loadYamlMap(stream: java.io.InputStream?): Map<String, Any?>? {
        stream ?: return null
        return stream.use { input ->
            @Suppress("UNCHECKED_CAST")
            Yaml().load(InputStreamReader(input, StandardCharsets.UTF_8)) as? Map<String, Any?>
        } ?: emptyMap()
    }

    private fun deepMerge(
        base: Map<String, Any?>,
        override: Map<String, Any?>,
    ): Map<String, Any?> {
        if (override.isEmpty()) return base
        val result = base.toMutableMap()
        for ((key, value) in override) {
            val baseValue = result[key]
            result[key] = when {
                baseValue is Map<*, *> && value is Map<*, *> ->
                    deepMerge(baseValue as Map<String, Any?>, value as Map<String, Any?>)
                else -> value
            }
        }
        return result
    }

    private fun parseSettings(root: Map<String, Any?>): AppSettings {
        val appSection = root["app"] as? Map<*, *>
        val windowSection = root["window"] as? Map<*, *>
        val captureSection = root["capture"] as? Map<*, *>
        val clipboardSection = root["clipboard"] as? Map<*, *>
        return AppSettings(
            app = AppInfo(
                title = string(appSection, "title") ?: AppInfo().title,
            ),
            window = WindowSettings(
                widthDp = int(windowSection, "widthDp") ?: WindowSettings().widthDp,
                heightDp = int(windowSection, "heightDp") ?: WindowSettings().heightDp,
                editorWidthDp = int(windowSection, "editorWidthDp") ?: WindowSettings().editorWidthDp,
                editorHeightDp = int(windowSection, "editorHeightDp") ?: WindowSettings().editorHeightDp,
                alwaysOnTop = bool(windowSection, "alwaysOnTop") ?: WindowSettings().alwaysOnTop,
                position = string(windowSection, "position") ?: WindowSettings().position,
                hideDelayMs = long(windowSection, "hideDelayMs") ?: WindowSettings().hideDelayMs,
            ),
            capture = CaptureSettings(
                command = string(captureSection, "command") ?: CaptureSettings().command,
                timeoutSeconds = long(captureSection, "timeoutSeconds") ?: CaptureSettings().timeoutSeconds,
                tempPrefix = string(captureSection, "tempPrefix") ?: CaptureSettings().tempPrefix,
                pathCandidates = stringList(captureSection, "pathCandidates")
                    ?: CaptureSettings().pathCandidates,
            ),
            clipboard = ClipboardSettings(
                wlCopyCommand = string(clipboardSection, "wlCopyCommand")
                    ?: ClipboardSettings().wlCopyCommand,
                wlCopyTypeFlag = string(clipboardSection, "wlCopyTypeFlag")
                    ?: ClipboardSettings().wlCopyTypeFlag,
                wlCopyMime = string(clipboardSection, "wlCopyMime") ?: ClipboardSettings().wlCopyMime,
                pathCandidates = stringList(clipboardSection, "pathCandidates")
                    ?: ClipboardSettings().pathCandidates,
            ),
        )
    }

    private fun string(section: Map<*, *>?, key: String): String? =
        section?.get(key)?.toString()?.takeIf { it.isNotBlank() }

    private fun int(section: Map<*, *>?, key: String): Int? =
        (section?.get(key) as? Number)?.toInt()

    private fun long(section: Map<*, *>?, key: String): Long? =
        (section?.get(key) as? Number)?.toLong()

    private fun bool(section: Map<*, *>?, key: String): Boolean? =
        section?.get(key) as? Boolean

    private fun stringList(section: Map<*, *>?, key: String): List<String>? {
        val list = section?.get(key) as? List<*> ?: return null
        return list.mapNotNull { it?.toString() }.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }
    }
}
