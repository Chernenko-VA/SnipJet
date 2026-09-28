package ru.chernenko.snipjet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import ru.chernenko.snipjet.capture.AreaCaptureRunner
import ru.chernenko.snipjet.capture.CaptureOutcome
import ru.chernenko.snipjet.config.AppConfig
import ru.chernenko.snipjet.platform.SingleInstance
import ru.chernenko.snipjet.platform.centeredDpPosition
import ru.chernenko.snipjet.platform.editorScreenDpSize
import ru.chernenko.snipjet.ui.CaptureWindowController
import ru.chernenko.snipjet.ui.SnipJetApp
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val captureFirst = args.any { it == "--capture" || it == "-c" }

    if (captureFirst && SingleInstance.tryNotifyPrimary(SingleInstance.COMMAND_CAPTURE)) {
        exitProcess(0)
    }

    val session = SnipJetSession()
    val settings = AppConfig.settings
    val ipcCaptureRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val singleInstanceServer = SingleInstance.Server { command ->
        if (command == SingleInstance.COMMAND_CAPTURE) {
            ipcCaptureRequests.tryEmit(Unit)
        }
    }
    if (!singleInstanceServer.start()) {
        // Another live primary owns the socket — do not steal it.
        if (captureFirst) {
            SingleInstance.tryNotifyPrimary(SingleInstance.COMMAND_CAPTURE)
        }
        exitProcess(0)
    }
    Runtime.getRuntime().addShutdownHook(
        Thread { singleInstanceServer.close() },
    )

    if (captureFirst) {
        when (val outcome = runBlocking { AreaCaptureRunner().captureArea() }) {
            is CaptureOutcome.Success -> session.openTab(outcome.image)
            CaptureOutcome.Cancelled -> {
                singleInstanceServer.close()
                exitProcess(0)
            }
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
        // Brief pin after capture so the window can take focus on Wayland without
        // fighting Compose by mutating AWT alwaysOnTop directly.
        var raiseAboveOthers by remember { mutableStateOf(false) }

        Window(
            onCloseRequest = {
                singleInstanceServer.close()
                exitApplication()
            },
            title = settings.app.title,
            state = windowState,
            alwaysOnTop = settings.window.alwaysOnTop || raiseAboveOthers,
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
            windowController.temporaryRaiseHandler = { raiseAboveOthers = it }
            SnipJetApp(
                session = session,
                onVisibilityForCapture = windowController::onVisibilityForCapture,
                onEditorOpen = windowController::onEditorOpen,
                onExit = {
                    singleInstanceServer.close()
                    exitApplication()
                },
                ipcCaptureRequests = ipcCaptureRequests,
                onBringToFront = windowController::bringToFront,
            )
        }
    }
}
