package ru.chernenko.snipjet.config

import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle

/**
 * UI and error strings from classpath [message.properties].
 */
object Messages {
    private val bundle: ResourceBundle =
        ResourceBundle.getBundle("message", Locale.getDefault())

    fun get(key: String, vararg args: Any?): String {
        val pattern = bundle.getString(key)
        return if (args.isEmpty()) pattern else MessageFormat.format(pattern, *args)
    }
}
