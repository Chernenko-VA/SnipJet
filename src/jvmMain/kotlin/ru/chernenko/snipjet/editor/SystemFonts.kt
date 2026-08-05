package ru.chernenko.snipjet.editor

import java.awt.GraphicsEnvironment

fun systemFontFamilies(): List<String> =
    GraphicsEnvironment.getLocalGraphicsEnvironment()
        .availableFontFamilyNames
        .toList()
        .sorted()

fun resolveDefaultTextFontFamily(families: List<String> = systemFontFamilies()): String {
    families.firstOrNull { it.equals(DefaultTextFontFamily, ignoreCase = true) }?.let { return it }
    families.firstOrNull { it.contains("Courier", ignoreCase = true) }?.let { return it }
    families.firstOrNull { it.equals("Monospaced", ignoreCase = true) }?.let { return it }
    return families.firstOrNull() ?: DefaultTextFontFamily
}
