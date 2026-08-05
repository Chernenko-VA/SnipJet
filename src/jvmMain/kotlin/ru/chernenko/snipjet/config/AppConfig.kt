package ru.chernenko.snipjet.config

/**
 * Application settings loaded once from bundled [application.yml],
 * overridden by `~/.config/snipjet/application.yml` when present.
 */
object AppConfig {
    private val settings: AppSettings = AppSettingsLoader.load()

    val appTitle: String get() = settings.app.title

    val windowWidthDp: Int get() = settings.window.widthDp
    val windowHeightDp: Int get() = settings.window.heightDp
    val editorWidthDp: Int get() = settings.window.editorWidthDp
    val editorHeightDp: Int get() = settings.window.editorHeightDp
    val windowAlwaysOnTop: Boolean get() = settings.window.alwaysOnTop
    val windowPosition: String get() = settings.window.position
    val windowHideDelayMs: Long get() = settings.window.hideDelayMs

    val captureCommand: String get() = settings.capture.command
    val captureTimeoutSeconds: Long get() = settings.capture.timeoutSeconds
    val captureTempPrefix: String get() = settings.capture.tempPrefix
    val capturePathCandidates: List<String> get() = settings.capture.pathCandidates

    val clipboardWlCopyCommand: String get() = settings.clipboard.wlCopyCommand
    val clipboardWlCopyTypeFlag: String get() = settings.clipboard.wlCopyTypeFlag
    val clipboardWlCopyMime: String get() = settings.clipboard.wlCopyMime
    val clipboardPathCandidates: List<String> get() = settings.clipboard.pathCandidates
}
