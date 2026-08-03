package ru.chernenko.snipjet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ru.chernenko.snipjet.config.MessageKeys
import ru.chernenko.snipjet.config.Messages
import kotlin.math.min

@Composable
fun EditorScreen(
    image: ImageBitmap,
    onNewCapture: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawScreenshotFitted(image)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            Button(onClick = onNewCapture) {
                Text(Messages.get(MessageKeys.STATUS_NEW_CAPTURE))
            }
            OutlinedButton(onClick = onExit) {
                Text(
                    Messages.get(MessageKeys.STATUS_EXIT),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

private fun DrawScope.drawScreenshotFitted(image: ImageBitmap) {
    val viewportW = size.width
    val viewportH = size.height
    if (viewportW <= 0f || viewportH <= 0f) return
    val srcW = image.width
    val srcH = image.height
    if (srcW <= 0 || srcH <= 0) return

    val scale = min(viewportW / srcW, viewportH / srcH)
    val dstW = (srcW * scale).toInt().coerceAtLeast(1)
    val dstH = (srcH * scale).toInt().coerceAtLeast(1)
    val left = ((viewportW - dstW) / 2f).toInt()
    val top = ((viewportH - dstH) / 2f).toInt()

    drawImage(
        image = image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(srcW, srcH),
        dstOffset = IntOffset(left, top),
        dstSize = IntSize(dstW, dstH),
    )
}
