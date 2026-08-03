package ru.chernenko.snipjet.config

import org.yaml.snakeyaml.Yaml
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Loads [application.yml] from the classpath once.
 */
object AppConfig {
    private val root: Map<String, Any?> = loadRoot()

    val appTitle: String get() = string("app", "title") ?: "SnipJet"

    val windowWidthDp: Int get() = int("window", "widthDp") ?: 420
    val windowHeightDp: Int get() = int("window", "heightDp") ?: 220
    val editorWidthDp: Int get() = int("window", "editorWidthDp") ?: 900
    val editorHeightDp: Int get() = int("window", "editorHeightDp") ?: 700
    val windowAlwaysOnTop: Boolean get() = bool("window", "alwaysOnTop") ?: false
    val windowPosition: String get() = string("window", "position") ?: "TopEnd"
    val windowHideDelayMs: Long get() = long("window", "hideDelayMs") ?: 300L

    val captureCommand: String get() = string("capture", "command") ?: "gnome-screenshot"
    val captureTimeoutSeconds: Long get() = long("capture", "timeoutSeconds") ?: 300L
    val captureTempPrefix: String get() = string("capture", "tempPrefix") ?: "snipjet-"

    @Suppress("UNCHECKED_CAST")
    val capturePathCandidates: List<String>
        get() {
            val section = root["capture"] as? Map<*, *> ?: return emptyList()
            val list = section["pathCandidates"] as? List<*> ?: return emptyList()
            return list.mapNotNull { it?.toString() }.filter { it.isNotBlank() }
        }

    val clipboardWlCopyCommand: String get() = string("clipboard", "wlCopyCommand") ?: "wl-copy"
    val clipboardWlCopyTypeFlag: String get() = string("clipboard", "wlCopyTypeFlag") ?: "--type"
    val clipboardWlCopyMime: String get() = string("clipboard", "wlCopyMime") ?: "image/png"

    @Suppress("UNCHECKED_CAST")
    val clipboardPathCandidates: List<String>
        get() {
            val section = root["clipboard"] as? Map<*, *> ?: return emptyList()
            val list = section["pathCandidates"] as? List<*> ?: return emptyList()
            return list.mapNotNull { it?.toString() }.filter { it.isNotBlank() }
        }

    private fun loadRoot(): Map<String, Any?> {
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("application.yml")
            ?: error("Missing classpath resource application.yml")
        return stream.use { input ->
            @Suppress("UNCHECKED_CAST")
            Yaml().load(InputStreamReader(input, StandardCharsets.UTF_8)) as Map<String, Any?>
        }
    }

    private fun section(name: String): Map<*, *>? = root[name] as? Map<*, *>

    private fun string(sectionName: String, key: String): String? =
        section(sectionName)?.get(key)?.toString()?.takeIf { it.isNotBlank() }

    private fun int(sectionName: String, key: String): Int? =
        (section(sectionName)?.get(key) as? Number)?.toInt()

    private fun long(sectionName: String, key: String): Long? =
        (section(sectionName)?.get(key) as? Number)?.toLong()

    private fun bool(sectionName: String, key: String): Boolean? =
        section(sectionName)?.get(key) as? Boolean
}
