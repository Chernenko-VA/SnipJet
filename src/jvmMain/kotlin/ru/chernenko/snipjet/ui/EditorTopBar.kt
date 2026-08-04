package ru.chernenko.snipjet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.chernenko.snipjet.config.AppConfig
import ru.chernenko.snipjet.config.MessageKeys
import ru.chernenko.snipjet.config.Messages
import ru.chernenko.snipjet.loadAppIcon

@Composable
fun EditorTopBar(
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCopy: () -> Unit,
    onSave: () -> Unit,
    onNewCapture: () -> Unit,
    undoEnabled: Boolean = false,
    redoEnabled: Boolean = false,
    copyEnabled: Boolean = false,
    saveEnabled: Boolean = false,
) {
    val brandIcon = remember { loadAppIcon() }
    val undoIcon = rememberSvgIcon("icon/undo.svg")
    val redoIcon = rememberSvgIcon("icon/redo.svg")
    val copyIcon = rememberSvgIcon("icon/copy.svg")
    val saveIcon = rememberSvgIcon("icon/save.svg")
    val newCaptureIcon = rememberSvgIcon("icon/new_capture.svg")

    Surface(tonalElevation = 1.dp) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = brandIcon,
                    contentDescription = AppConfig.appTitle,
                    Modifier.size(28.dp),
                    tint = Color.Unspecified,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = AppConfig.appTitle,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(onClick = onUndo, enabled = undoEnabled) {
                    Icon(
                        painter = undoIcon,
                        contentDescription = Messages.get(MessageKeys.EDITOR_UNDO),
                        Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onRedo, enabled = redoEnabled) {
                    Icon(
                        painter = redoIcon,
                        contentDescription = Messages.get(MessageKeys.EDITOR_REDO),
                        Modifier.size(20.dp),
                    )
                }

                VerticalDivider(Modifier.height(24.dp).padding(horizontal = 4.dp))

                OutlinedButton(onClick = onCopy, enabled = copyEnabled) {
                    Icon(
                        painter = copyIcon,
                        contentDescription = null,
                        Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(Messages.get(MessageKeys.EDITOR_COPY))
                }
                Button(onClick = onSave, enabled = saveEnabled) {
                    Icon(
                        painter = saveIcon,
                        contentDescription = null,
                        Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(Messages.get(MessageKeys.EDITOR_SAVE))
                }
                OutlinedButton(onClick = onNewCapture) {
                    Icon(
                        painter = newCaptureIcon,
                        contentDescription = null,
                        Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(Messages.get(MessageKeys.STATUS_NEW_CAPTURE))
                }
            }
        }
    }
}

@Composable
internal fun rememberSvgIcon(path: String): Painter {
    val density = LocalDensity.current
    return remember(path, density) {
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(path)
            ?: error("Missing resource $path")
        stream.use { loadSvgPainter(it, density) }
    }
}
