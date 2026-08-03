package ru.chernenko.snipjet.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap

@Composable
fun SnipJetApp(
    onVisibilityForCapture: (visible: Boolean) -> Unit,
    onEditorOpen: (open: Boolean) -> Unit,
    onExit: () -> Unit,
) {
    var editorImage by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(editorImage != null) {
        onEditorOpen(editorImage != null)
    }

    val image = editorImage
    if (image != null) {
        EditorScreen(
            image = image,
            onNewCapture = { editorImage = null },
            onExit = onExit,
        )
    } else {
        StatusApp(
            onVisibilityForCapture = onVisibilityForCapture,
            onCaptureReady = { editorImage = it },
            onExit = onExit,
        )
    }
}
