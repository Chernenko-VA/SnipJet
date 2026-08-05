package ru.chernenko.snipjet

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.runBlocking
import ru.chernenko.snipjet.capture.AreaCaptureRunner
import ru.chernenko.snipjet.capture.CaptureOutcome
import ru.chernenko.snipjet.config.AppConfig
import ru.chernenko.snipjet.platform.centeredDpPosition
import ru.chernenko.snipjet.platform.editorScreenDpSize
import ru.chernenko.snipjet.ui.CaptureWindowController
import ru.chernenko.snipjet.ui.SnipJetApp
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val captureFirst = args.any { it == "--capture" || it == "-c" }
    val session = SnipJetSession()
    val settings = AppConfig.settings

    if (captureFirst) {
        when (val outcome = runBlocking { AreaCaptureRunner().captureArea() }) {
            is CaptureOutcome.Success -> session.openTab(outcome.image)
            CaptureOutcome.Cancelled -> exitProcess(0)
            else -> {}
        }
    }

    application {
        val statusAlignment = settings.window.position.toComposeAlignment()

        val windowState = if (session.editorOpen) {
            val editorSize = editorScreenDpSize(settings.window)
            rememberWindowState(
                size = editorSize,
                position = centeredDpPosition(editorSize).composePosition,
            )
        } else {
            rememberWindowState(
                size = DpSize(settings.window.widthDp.dp, settings.window.heightDp.dp),
                position = WindowPosition.Aligned(statusAlignment),
            )
        }
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
