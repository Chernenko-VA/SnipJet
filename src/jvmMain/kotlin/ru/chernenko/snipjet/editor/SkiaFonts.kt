package ru.chernenko.snipjet.editor

import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface

fun matchTypeface(family: String, bold: Boolean, italic: Boolean): Typeface {
    val style = when {
        bold && italic -> FontStyle.BOLD_ITALIC
        bold -> FontStyle.BOLD
        italic -> FontStyle.ITALIC
        else -> FontStyle.NORMAL
    }
    return FontMgr.default.matchFamilyStyle(family, style)
        ?: FontMgr.default.matchFamilyStyle(null, style)
        ?: Typeface.makeEmpty()
}
