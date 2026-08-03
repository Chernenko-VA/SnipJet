package ru.chernenko.snipjet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var windowVisible by remember { mutableStateOf(true) }
    val icon = remember { loadAppIcon() }

    Window(
        onCloseRequest = ::exitApplication,
        title = AppConfig.appTitle,
        state = windowState,
        visible = windowVisible,
        alwaysOnTop = AppConfig.windowAlwaysOnTop,
        icon = icon,
        resizable = false,
    ) {
        StatusApp(
            onVisibilityForCapture = { visible -> windowVisible = visible },
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
