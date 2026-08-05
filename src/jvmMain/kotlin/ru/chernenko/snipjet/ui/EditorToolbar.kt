package ru.chernenko.snipjet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EditorToolbar(
    selected: EditorTool,
    onSelect: (EditorTool) -> Unit,
    modifier: Modifier = Modifier,
    enabledTools: Set<EditorTool> = EditorTool.entries.toSet(),
) {
    Surface(
        modifier = modifier.width(56.dp),
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EditorTool.entries.forEach { tool ->
                val enabled = tool in enabledTools
                val isSelected = tool == selected
                IconButton(
                    onClick = { onSelect(tool) },
                    enabled = enabled,
                ) {
                    Icon(
                        painter = rememberToolIcon(tool),
                        contentDescription = tool.name,
                        modifier = Modifier.size(20.dp),
                        tint = when {
                            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            isSelected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}
