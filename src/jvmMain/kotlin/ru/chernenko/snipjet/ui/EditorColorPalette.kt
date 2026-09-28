package ru.chernenko.snipjet.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.chernenko.snipjet.config.MessageKeys
import ru.chernenko.snipjet.config.Messages
import ru.chernenko.snipjet.editor.ShapeKind
import kotlin.math.min

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

private const val PaletteColumns = 6
private val SwatchSize = 28.dp
private val SwatchGap = 8.dp
private val PaletteHorizontalPadding = 12.dp
private val BrushPreviewMaxDp = 48.dp

/** Width for all palette swatches in a single row plus gaps and padding. */
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
    showShapeOptions: Boolean = false,
    showBrushPreview: Boolean = !showTextOptions,
    selectedShapeKind: ShapeKind = ShapeKind.Line,
    onShapeKindChange: (ShapeKind) -> Unit = {},
    shapeFilled: Boolean = false,
    onShapeFilledChange: (Boolean) -> Unit = {},
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
            if (showShapeOptions) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShapeKind.entries.forEach { kind ->
                        val selectedKind = kind == selectedShapeKind
                        IconButton(
                            onClick = { onShapeKindChange(kind) },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Image(
                                painter = rememberShapeKindIcon(kind),
                                contentDescription = kind.name,
                                modifier = Modifier.size(20.dp),
                                colorFilter = ColorFilter.tint(
                                    if (selectedKind) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                ),
                            )
                        }
                    }
                }
            }

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

            if (showShapeOptions) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onShapeFilledChange(!shapeFilled) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = shapeFilled,
                        onCheckedChange = onShapeFilledChange,
                    )
                    Text(
                        text = Messages.get(MessageKeys.EDITOR_SHAPE_FILL),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
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
                    var fontQuery by remember(fontFamily) { mutableStateOf(fontFamily) }
                    var highlightedIndex by remember { mutableStateOf(0) }
                    val filteredFonts = remember(fontQuery, fontFamilies, fontFamily) {
                        // Keep full list while the field still shows the current selection.
                        if (fontQuery.isBlank() || fontQuery.equals(fontFamily, ignoreCase = true)) {
                            fontFamilies
                        } else {
                            fontFamilies.filter { it.contains(fontQuery, ignoreCase = true) }
                        }
                    }
                    fun dismissFontMenu() {
                        fontMenuExpanded = false
                        fontQuery = fontFamily
                    }
                    fun selectFont(family: String) {
                        onFontFamilyChange(family)
                        fontQuery = family
                        fontMenuExpanded = false
                    }
                    fun highlightCurrentFont() {
                        val idx = filteredFonts.indexOfFirst { it.equals(fontFamily, ignoreCase = true) }
                        highlightedIndex = if (idx >= 0) idx else 0
                    }
                    LaunchedEffect(filteredFonts, fontMenuExpanded) {
                        if (!fontMenuExpanded) return@LaunchedEffect
                        if (filteredFonts.isEmpty()) {
                            highlightedIndex = 0
                        } else {
                            highlightedIndex = highlightedIndex.coerceIn(0, filteredFonts.lastIndex)
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = fontMenuExpanded,
                        onExpandedChange = { expand ->
                            fontMenuExpanded = expand
                            if (expand) {
                                fontQuery = fontFamily
                                highlightCurrentFont()
                            } else {
                                fontQuery = fontFamily
                            }
                        },
                    ) {
                        OutlinedTextField(
                            value = fontQuery,
                            onValueChange = { query ->
                                fontQuery = query
                                fontMenuExpanded = true
                                highlightedIndex = 0
                            },
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontMenuExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                                .fillMaxWidth()
                                .onFocusChanged { state ->
                                    if (state.isFocused) {
                                        fontMenuExpanded = true
                                        fontQuery = fontFamily
                                        highlightCurrentFont()
                                    }
                                }
                                .onPreviewKeyEvent { event ->
                                    if (!fontMenuExpanded || event.type != KeyEventType.KeyDown) {
                                        return@onPreviewKeyEvent false
                                    }
                                    when (event.key) {
                                        Key.DirectionDown -> {
                                            if (filteredFonts.isNotEmpty()) {
                                                highlightedIndex =
                                                    (highlightedIndex + 1).coerceAtMost(filteredFonts.lastIndex)
                                            }
                                            true
                                        }
                                        Key.DirectionUp -> {
                                            if (filteredFonts.isNotEmpty()) {
                                                highlightedIndex = (highlightedIndex - 1).coerceAtLeast(0)
                                            }
                                            true
                                        }
                                        Key.Enter, Key.NumPadEnter -> {
                                            filteredFonts.getOrNull(highlightedIndex)?.let { selectFont(it) }
                                            true
                                        }
                                        Key.Escape -> {
                                            dismissFontMenu()
                                            true
                                        }
                                        else -> false
                                    }
                                },
                            textStyle = MaterialTheme.typography.labelSmall,
                        )
                        ExposedDropdownMenu(
                            expanded = fontMenuExpanded,
                            onDismissRequest = { dismissFontMenu() },
                            modifier = Modifier.heightIn(max = 240.dp),
                        ) {
                            if (filteredFonts.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(Messages.get(MessageKeys.EDITOR_FONT_NO_MATCH))
                                    },
                                    onClick = {},
                                    enabled = false,
                                )
                            } else {
                                filteredFonts.forEachIndexed { index, family ->
                                    val bringIntoViewRequester = remember(family) { BringIntoViewRequester() }
                                    LaunchedEffect(highlightedIndex) {
                                        if (index == highlightedIndex) {
                                            bringIntoViewRequester.bringIntoView()
                                        }
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                family,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        onClick = { selectFont(family) },
                                        modifier = Modifier
                                            .bringIntoViewRequester(bringIntoViewRequester)
                                            .then(
                                                if (index == highlightedIndex) {
                                                    Modifier.background(
                                                        MaterialTheme.colorScheme.secondaryContainer,
                                                    )
                                                } else {
                                                    Modifier
                                                },
                                            ),
                                    )
                                }
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

            if (showBrushPreview) {
                BrushPreviewCircle(
                    color = selected.copy(alpha = alpha.coerceIn(StrokeAlphaMin, StrokeAlphaMax)),
                    diameterPx = widthPx.coerceIn(StrokeWidthMinPx, StrokeWidthMaxPx),
                )
            }
        }
    }
}

@Composable
private fun BrushPreviewCircle(
    color: Color,
    diameterPx: Float,
) {
    val diameterDp = min(diameterPx, BrushPreviewMaxDp.value).dp
    Box(
        modifier = Modifier
            .size(BrushPreviewMaxDp)
            .wrapContentSize(Alignment.Center),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(diameterDp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = CircleShape,
                ),
        )
    }
}
