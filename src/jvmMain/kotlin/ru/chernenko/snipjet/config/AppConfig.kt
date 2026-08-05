package ru.chernenko.snipjet.config

/**
 * Application settings loaded once from bundled [application.yml],
 * overridden by `~/.config/snipjet/application.yml` when present.
 */
object AppConfig {
    val settings: AppSettings = AppSettingsLoader.load()
}
