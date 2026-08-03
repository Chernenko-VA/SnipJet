package ru.chernenko.snipjet

import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ru.chernenko.snipjet.config.AppConfig
import ru.chernenko.snipjet.ui.StatusApp

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(AppConfig.windowWidthDp.dp, AppConfig.windowHeightDp.dp),
        position = WindowPosition.Aligned(windowAlignment(AppConfig.windowPosition)),
    )
    val icon = remember { loadAppIcon() }

    Window(
        onCloseRequest = ::exitApplication,
        title = AppConfig.appTitle,
        state = windowState,
        alwaysOnTop = AppConfig.windowAlwaysOnTop,
        icon = icon,
        resizable = false,
    ) {
        StatusApp(
            onVisibilityForCapture = { visible -> windowState.isMinimized = !visible },
            onExit = ::exitApplication,
        )
    }
}

private fun windowAlignment(name: String): Alignment = when (name) {
    "BottomEnd" -> Alignment.BottomEnd
    "TopStart" -> Alignment.TopStart
    "BottomStart" -> Alignment.BottomStart
    "Center" -> Alignment.Center
    else -> Alignment.TopEnd
}
