package ru.chernenko.snipjet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
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

val EditorFontSizesPt: List<Int> = listOf(
    8, 9, 10, 11, 12, 14, 16, 18, 20, 24, 28, 32, 36, 48, 72,
)

const val DefaultTextFontSizePt = 14

/** Converts typographic points to screenshot pixels at 96 DPI. */
fun fontSizePtToPx(pt: Int): Float = pt * 96f / 72f

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorColorPalette(
    selected: Color,
    onSelect: (Color) -> Unit,
    alpha: Float,
    onAlphaChange: (Float) -> Unit,
    widthPx: Float,
    onWidthChange: (Float) -> Unit,
    showTextOptions: Boolean = false,
    fontFamily: String = "",
    fontFamilies: List<String> = emptyList(),
    onFontFamilyChange: (String) -> Unit = {},
    fontSizePt: Int = DefaultTextFontSizePt,
    fontSizesPt: List<Int> = EditorFontSizesPt,
    onFontSizePtChange: (Int) -> Unit = {},
    bold: Boolean = false,
    onBoldChange: (Boolean) -> Unit = {},
    italic: Boolean = false,
    onItalicChange: (Boolean) -> Unit = {},
    underline: Boolean = false,
    onUnderlineChange: (Boolean) -> Unit = {},
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
                if (showTextOptions) {
                    Text(
                        text = Messages.get(MessageKeys.EDITOR_FONT_SIZE),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    var sizeMenuExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = sizeMenuExpanded,
                        onExpandedChange = { sizeMenuExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = "$fontSizePt pt",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = sizeMenuExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            textStyle = MaterialTheme.typography.labelSmall,
                        )
                        ExposedDropdownMenu(
                            expanded = sizeMenuExpanded,
                            onDismissRequest = { sizeMenuExpanded = false },
                            modifier = Modifier.heightIn(max = 240.dp),
                        ) {
                            fontSizesPt.forEach { size ->
                                DropdownMenuItem(
                                    text = { Text("$size pt") },
                                    onClick = {
                                        onFontSizePtChange(size)
                                        sizeMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                } else {
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

            if (showTextOptions) {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = Messages.get(MessageKeys.EDITOR_FONT),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    var fontMenuExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = fontMenuExpanded,
                        onExpandedChange = { fontMenuExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = fontFamily,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontMenuExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            textStyle = MaterialTheme.typography.labelSmall,
                        )
                        ExposedDropdownMenu(
                            expanded = fontMenuExpanded,
                            onDismissRequest = { fontMenuExpanded = false },
                            modifier = Modifier.heightIn(max = 240.dp),
                        ) {
                            fontFamilies.forEach { family ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            family,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    onClick = {
                                        onFontFamilyChange(family)
                                        fontMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FilterChip(
                            selected = bold,
                            onClick = { onBoldChange(!bold) },
                            label = {
                                Text(
                                    Messages.get(MessageKeys.EDITOR_BOLD),
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                        FilterChip(
                            selected = italic,
                            onClick = { onItalicChange(!italic) },
                            label = {
                                Text(
                                    Messages.get(MessageKeys.EDITOR_ITALIC),
                                    fontStyle = FontStyle.Italic,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                        FilterChip(
                            selected = underline,
                            onClick = { onUnderlineChange(!underline) },
                            label = {
                                Text(
                                    Messages.get(MessageKeys.EDITOR_UNDERLINE),
                                    textDecoration = TextDecoration.Underline,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
