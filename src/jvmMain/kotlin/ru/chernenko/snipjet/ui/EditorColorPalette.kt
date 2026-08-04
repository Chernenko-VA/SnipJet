package ru.chernenko.snipjet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.chernenko.snipjet.config.MessageKeys
import ru.chernenko.snipjet.config.Messages

val EditorPaletteColors: List<Color> = listOf(
    Color(0xFFE53935), // red
    Color(0xFF43A047), // green
    Color(0xFF1E88E5), // blue
    Color(0xFFFDD835), // yellow
    Color(0xFF212121), // black
    Color(0xFFFFFFFF), // white
)

const val StrokeAlphaMin = 0.1f
const val StrokeAlphaMax = 1f
const val StrokeWidthMinPx = 2f
const val StrokeWidthMaxPx = 40f

private const val PaletteColumns = 4
private val SwatchSize = 28.dp
private val SwatchGap = 8.dp
private val PaletteHorizontalPadding = 12.dp

/** Width for 4 swatches in a row plus gaps and padding. */
val EditorColorPaletteWidth =
    PaletteHorizontalPadding * 2 + SwatchSize * PaletteColumns + SwatchGap * (PaletteColumns - 1)

@Composable
fun EditorColorPalette(
    selected: Color,
    onSelect: (Color) -> Unit,
    alpha: Float,
    onAlphaChange: (Float) -> Unit,
    widthPx: Float,
    onWidthChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width(EditorColorPaletteWidth),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier.padding(vertical = 12.dp, horizontal = PaletteHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EditorPaletteColors.chunked(PaletteColumns).forEach { rowColors ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SwatchGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    rowColors.forEach { color ->
                        val isSelected = color == selected
                        Box(
                            Modifier
                                .size(SwatchSize)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = CircleShape,
                                )
                                .clickable { onSelect(color) },
                        )
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text(
                        text = Messages.get(MessageKeys.EDITOR_OPACITY),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = alpha.coerceIn(StrokeAlphaMin, StrokeAlphaMax),
                        onValueChange = onAlphaChange,
                        valueRange = StrokeAlphaMin..StrokeAlphaMax,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text(
                        text = Messages.get(MessageKeys.EDITOR_STROKE_SIZE),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = widthPx.coerceIn(StrokeWidthMinPx, StrokeWidthMaxPx),
                        onValueChange = onWidthChange,
                        valueRange = StrokeWidthMinPx..StrokeWidthMaxPx,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
