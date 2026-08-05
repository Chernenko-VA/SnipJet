package ru.chernenko.snipjet

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ru.chernenko.snipjet.config.AppConfig
import ru.chernenko.snipjet.ui.CaptureWindowController
import ru.chernenko.snipjet.ui.SnipJetApp

fun main() {
    val session = SnipJetSession()
    val settings = AppConfig.settings
    application {
        val statusAlignment = settings.window.position.toComposeAlignment()
        val windowState = rememberWindowState(
            size = DpSize(settings.window.widthDp.dp, settings.window.heightDp.dp),
            position = WindowPosition.Aligned(statusAlignment),
        )
        val icon = remember { loadAppIcon() }

        Window(
            onCloseRequest = ::exitApplication,
            title = settings.app.title,
            state = windowState,
            alwaysOnTop = settings.window.alwaysOnTop,
            icon = icon,
            resizable = true,
        ) {
            val windowController = remember(window, windowState) {
                CaptureWindowController(
                    window = window,
                    windowState = windowState,
                    session = session,
                    statusAlignment = statusAlignment,
                    windowSettings = settings.window,
                )
            }
            SnipJetApp(
                session = session,
                onVisibilityForCapture = windowController::onVisibilityForCapture,
                onEditorOpen = windowController::onEditorOpen,
                onExit = ::exitApplication,
            )
        }
    }
}
