package ru.chernenko.snipjet.config

import androidx.compose.ui.Alignment
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

enum class WindowAnchor {
    TopEnd,
    BottomEnd,
    TopStart,
    BottomStart,
    Center,
    ;

    fun toComposeAlignment(): Alignment = when (this) {
        TopEnd -> Alignment.TopEnd
        BottomEnd -> Alignment.BottomEnd
        TopStart -> Alignment.TopStart
        BottomStart -> Alignment.BottomStart
        Center -> Alignment.Center
    }

    companion object {
        fun parse(value: String?): WindowAnchor = entries.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: TopEnd
    }
}

data class WindowSettings(
    val widthDp: Int = 480,
    val heightDp: Int = 360,
    /** Fixed editor width; 0 uses a screen fraction at startup. */
    val editorWidthDp: Int = 0,
    /** Fixed editor height; 0 uses a screen fraction at startup. */
    val editorHeightDp: Int = 0,
    val alwaysOnTop: Boolean = false,
    val position: WindowAnchor = WindowAnchor.TopEnd,
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
    }
}

data class CaptureSettings(
    val command: String = "gnome-screenshot",
    val timeoutSeconds: Long = 300,
    val tempPrefix: String = "snipjet-",
    val hideDelayMs: Long = 300,
) {
    val pathCandidates: List<String> get() = standardLinuxPaths(command)

    fun validate() {
        require(command.isNotBlank()) { "capture.command must not be blank" }
        require(timeoutSeconds in 1..3600) { "capture.timeoutSeconds must be between 1 and 3600" }
        require(tempPrefix.isNotBlank()) { "capture.tempPrefix must not be blank" }
        require(hideDelayMs in 0..5000) { "capture.hideDelayMs must be between 0 and 5000" }
    }
}

data class ClipboardSettings(
    val wlCopyCommand: String = "wl-copy",
    val wlCopyTypeFlag: String = "--type",
    val wlCopyMime: String = "image/png",
) {
    val pathCandidates: List<String> get() = standardLinuxPaths(wlCopyCommand)

    fun validate() {
        require(wlCopyCommand.isNotBlank()) { "clipboard.wlCopyCommand must not be blank" }
        require(wlCopyTypeFlag.isNotBlank()) { "clipboard.wlCopyTypeFlag must not be blank" }
        require(wlCopyMime.isNotBlank()) { "clipboard.wlCopyMime must not be blank" }
    }
}

internal fun standardLinuxPaths(command: String): List<String> = listOf(
    "/usr/bin/$command",
    "/bin/$command",
    "/usr/local/bin/$command",
)

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
        val defaults = AppSettings()
        val appSection = root["app"] as? Map<*, *>
        val windowSection = root["window"] as? Map<*, *>
        val captureSection = root["capture"] as? Map<*, *>
        val clipboardSection = root["clipboard"] as? Map<*, *>
        val hideDelayMs = long(captureSection, "hideDelayMs")
            ?: long(windowSection, "hideDelayMs")
            ?: defaults.capture.hideDelayMs
        return AppSettings(
            app = defaults.app.copy(
                title = string(appSection, "title") ?: defaults.app.title,
            ),
            window = defaults.window.copy(
                widthDp = int(windowSection, "widthDp") ?: defaults.window.widthDp,
                heightDp = int(windowSection, "heightDp") ?: defaults.window.heightDp,
                editorWidthDp = int(windowSection, "editorWidthDp") ?: defaults.window.editorWidthDp,
                editorHeightDp = int(windowSection, "editorHeightDp") ?: defaults.window.editorHeightDp,
                alwaysOnTop = bool(windowSection, "alwaysOnTop") ?: defaults.window.alwaysOnTop,
                position = WindowAnchor.parse(string(windowSection, "position")),
            ),
            capture = defaults.capture.copy(
                command = string(captureSection, "command") ?: defaults.capture.command,
                timeoutSeconds = long(captureSection, "timeoutSeconds") ?: defaults.capture.timeoutSeconds,
                tempPrefix = string(captureSection, "tempPrefix") ?: defaults.capture.tempPrefix,
                hideDelayMs = hideDelayMs,
            ),
            clipboard = defaults.clipboard.copy(
                wlCopyCommand = string(clipboardSection, "wlCopyCommand")
                    ?: defaults.clipboard.wlCopyCommand,
                wlCopyTypeFlag = string(clipboardSection, "wlCopyTypeFlag")
                    ?: defaults.clipboard.wlCopyTypeFlag,
                wlCopyMime = string(clipboardSection, "wlCopyMime") ?: defaults.clipboard.wlCopyMime,
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
}
