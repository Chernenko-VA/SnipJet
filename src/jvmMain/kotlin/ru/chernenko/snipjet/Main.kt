package ru.chernenko.snipjet

import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.runBlocking
import ru.chernenko.snipjet.capture.AreaCaptureRunner
import ru.chernenko.snipjet.capture.CaptureOutcome
import ru.chernenko.snipjet.config.AppConfig
import ru.chernenko.snipjet.platform.centeredDpPosition
import ru.chernenko.snipjet.platform.editorScreenDpSize
import ru.chernenko.snipjet.ui.CaptureWindowController
import ru.chernenko.snipjet.ui.EditorApp
import ru.chernenko.snipjet.ui.StatusApp
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val captureFirst = args.any { it == "--capture" || it == "-c" }
    val session = SnipJetSession()
    val settings = AppConfig.settings
    var bootStatusOutcome: CaptureOutcome? = null
    var bootPendingPng: ByteArray? = null

    if (captureFirst) {
        when (val outcome = runBlocking { AreaCaptureRunner().captureArea() }) {
            is CaptureOutcome.Success -> {
                session.openTab(outcome.image)
                bootPendingPng = outcome.pngBytes
            }
            CaptureOutcome.Cancelled -> exitProcess(0)
            else -> bootStatusOutcome = outcome
        }
    }

    application {
        val captureRunner = remember { AreaCaptureRunner() }
        val icon = remember { loadAppIcon() }
        val statusAlignment = settings.window.position.toComposeAlignment()

        var capturing by remember { mutableStateOf(false) }
        var statusOutcome by remember { mutableStateOf(bootStatusOutcome) }
        var pendingPng by remember { mutableStateOf(bootPendingPng) }

        when {
            session.editorOpen -> {
                val editorSize = editorScreenDpSize(settings.window)
                val editorState = rememberWindowState(
                    size = editorSize,
                    position = centeredDpPosition(editorSize).composePosition,
                )
                Window(
                    onCloseRequest = ::exitApplication,
                    title = settings.app.title,
                    state = editorState,
                    alwaysOnTop = settings.window.alwaysOnTop,
                    icon = icon,
                    resizable = true,
                ) {
                    val windowController = remember(window, editorState) {
                        CaptureWindowController(window = window, windowState = editorState)
                    }
                    EditorApp(
                        session = session,
                        captureRunner = captureRunner,
                        onVisibilityForCapture = windowController::onVisibilityForCapture,
                        pendingBackgroundCopyPng = pendingPng,
                    )
                }
            }

            capturing -> {
                // Keep the application alive with no visible UI — same as --capture.
                val hostState = rememberWindowState(size = DpSize(1.dp, 1.dp))
                Window(
                    onCloseRequest = {},
                    state = hostState,
                    visible = false,
                    undecorated = true,
                    transparent = true,
                    resizable = false,
                ) {
                    LaunchedEffect(Unit) {
                        when (val outcome = captureRunner.captureArea()) {
                            is CaptureOutcome.Success -> {
                                session.openTab(outcome.image)
                                pendingPng = outcome.pngBytes
                                capturing = false
                            }
                            CaptureOutcome.Cancelled -> {
                                statusOutcome = CaptureOutcome.Cancelled
                                capturing = false
                            }
                            else -> {
                                statusOutcome = outcome
                                capturing = false
                            }
                        }
                    }
                }
            }

            else -> {
                val statusState = rememberWindowState(
                    size = DpSize(settings.window.widthDp.dp, settings.window.heightDp.dp),
                    position = WindowPosition.Aligned(statusAlignment),
                )
                Window(
                    onCloseRequest = ::exitApplication,
                    title = settings.app.title,
                    state = statusState,
                    alwaysOnTop = settings.window.alwaysOnTop,
                    icon = icon,
                    resizable = true,
                ) {
                    StatusApp(
                        onStartCapture = {
                            statusOutcome = null
                            capturing = true
                        },
                        onExit = ::exitApplication,
                        captureRunner = captureRunner,
                        initialOutcome = statusOutcome,
                    )
                }
            }
        }
    }
}
