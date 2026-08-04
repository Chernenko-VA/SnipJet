package ru.chernenko.snipjet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.chernenko.snipjet.clipboard.ImageClipboard
import ru.chernenko.snipjet.clipboard.LinuxImageClipboard
import ru.chernenko.snipjet.editor.EditorTab
import ru.chernenko.snipjet.editor.StrokeAnnotation
import ru.chernenko.snipjet.editor.composeAnnotatedPng
import ru.chernenko.snipjet.editor.eraseStrokesAlongPath

private data class ToolStrokeSettings(
    val alpha: Float,
    val widthPx: Float,
)

private val DefaultPenSettings = ToolStrokeSettings(alpha = 1f, widthPx = 4f)
private val DefaultMarkerSettings = ToolStrokeSettings(alpha = 0.45f, widthPx = 16f)
private const val EraserRadiusPx = 20f

@Composable
fun EditorScreen(
    tab: EditorTab,
    tabs: List<EditorTab>,
    onSelectTab: (Long) -> Unit,
    onCloseTab: (Long) -> Unit,
    onStrokesChange: (tabId: Long, strokes: List<StrokeAnnotation>) -> Unit,
    onNewCapture: () -> Unit,
    clipboard: ImageClipboard = remember { LinuxImageClipboard() },
) {
    val image = tab.image
    val strokes = tab.strokes
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val imageWidth = with(density) { image.width.toDp() }
    val imageHeight = with(density) { image.height.toDp() }

    var selectedTool by remember { mutableStateOf(EditorTool.Pen) }
    var colorPanelOpen by remember { mutableStateOf(true) }
    var selectedColor by remember { mutableStateOf(EditorPaletteColors.first()) }
    var penSettings by remember { mutableStateOf(DefaultPenSettings) }
    var markerSettings by remember { mutableStateOf(DefaultMarkerSettings) }
    var copyInProgress by remember { mutableStateOf(false) }

    val activeStrokeSettings = when (selectedTool) {
        EditorTool.Marker -> markerSettings
        else -> penSettings
    }
    val enabledTools = remember { setOf(EditorTool.Pen, EditorTool.Marker, EditorTool.Eraser) }
    val canvasBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val dotColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    fun copyToClipboard() {
        if (copyInProgress) return
        copyInProgress = true
        val strokesSnapshot = strokes
        val imageSnapshot = image
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val pngBytes = composeAnnotatedPng(imageSnapshot, strokesSnapshot)
                    clipboard.copyPngBytes(pngBytes)
                }
            } catch (_: Exception) {
                // Keep editor usable; clipboard errors are non-fatal here.
            } finally {
                copyInProgress = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        EditorTopBar(
            onUndo = {},
            onRedo = {},
            onCopy = ::copyToClipboard,
            onSave = {},
            onNewCapture = onNewCapture,
            copyEnabled = !copyInProgress,
        )
        HorizontalDivider()
        EditorTabBar(
            tabs = tabs,
            activeTabId = tab.id,
            onSelect = onSelectTab,
            onClose = onCloseTab,
        )
        HorizontalDivider()

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            EditorToolbar(
                selected = selectedTool,
                enabledTools = enabledTools,
                onSelect = { tool ->
                    if (tool !in enabledTools) return@EditorToolbar
                    val usesColorPanel = tool == EditorTool.Pen || tool == EditorTool.Marker
                    if (usesColorPanel) {
                        if (selectedTool == tool && colorPanelOpen) {
                            colorPanelOpen = false
                        } else {
                            selectedTool = tool
                            colorPanelOpen = true
                        }
                    } else {
                        selectedTool = tool
                        colorPanelOpen = false
                    }
                },
                modifier = Modifier.fillMaxHeight(),
            )
            VerticalDivider()

            if (colorPanelOpen) {
                EditorColorPalette(
                    selected = selectedColor,
                    onSelect = { selectedColor = it },
                    alpha = activeStrokeSettings.alpha,
                    onAlphaChange = { alpha ->
                        when (selectedTool) {
                            EditorTool.Marker -> markerSettings = markerSettings.copy(alpha = alpha)
                            else -> penSettings = penSettings.copy(alpha = alpha)
                        }
                    },
                    widthPx = activeStrokeSettings.widthPx,
                    onWidthChange = { widthPx ->
                        when (selectedTool) {
                            EditorTool.Marker -> markerSettings = markerSettings.copy(widthPx = widthPx)
                            else -> penSettings = penSettings.copy(widthPx = widthPx)
                        }
                    },
                    modifier = Modifier.fillMaxHeight(),
                )
                VerticalDivider()
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(canvasBg),
            ) {
                key(tab.id) {
                    var activePoints by remember { mutableStateOf<List<Offset>?>(null) }
                    val verticalScroll = rememberScrollState()
                    val horizontalScroll = rememberScrollState()

                    Box(modifier = Modifier.fillMaxSize()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val step = 16.dp.toPx()
                            var y = step
                            while (y < size.height) {
                                var x = step
                                while (x < size.width) {
                                    drawCircle(color = dotColor, radius = 1.2f, center = Offset(x, y))
                                    x += step
                                }
                                y += step
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 12.dp, bottom = 12.dp)
                                .verticalScroll(verticalScroll)
                                .horizontalScroll(horizontalScroll),
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .size(imageWidth, imageHeight),
                            ) {
                                Image(
                                    painter = BitmapPainter(image),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(
                                            tab.id,
                                            selectedTool,
                                            selectedColor,
                                            activeStrokeSettings,
                                            image.width,
                                            image.height,
                                            strokes,
                                        ) {
                                            val tabId = tab.id
                                            when (selectedTool) {
                                                EditorTool.Pen, EditorTool.Marker -> {
                                                    val strokeWidth = activeStrokeSettings.widthPx
                                                    val strokeColor =
                                                        selectedColor.copy(alpha = activeStrokeSettings.alpha)
                                                    detectDragGestures(
                                                        onDragStart = { start ->
                                                            activePoints = listOf(
                                                                start.toImageOffset(
                                                                    size.width.toFloat(),
                                                                    size.height.toFloat(),
                                                                    image.width,
                                                                    image.height,
                                                                ),
                                                            )
                                                        },
                                                        onDrag = { change, _ ->
                                                            change.consume()
                                                            val point = change.position.toImageOffset(
                                                                size.width.toFloat(),
                                                                size.height.toFloat(),
                                                                image.width,
                                                                image.height,
                                                            )
                                                            activePoints = (activePoints ?: emptyList()) + point
                                                        },
                                                        onDragEnd = {
                                                            val points = activePoints
                                                            activePoints = null
                                                            if (points != null && points.size >= 2) {
                                                                onStrokesChange(
                                                                    tabId,
                                                                    strokes + StrokeAnnotation(
                                                                        points = points,
                                                                        color = strokeColor,
                                                                        widthPx = strokeWidth,
                                                                    ),
                                                                )
                                                            }
                                                        },
                                                        onDragCancel = { activePoints = null },
                                                    )
                                                }
                                                EditorTool.Eraser -> {
                                                    // Press+release (tap) and drag both erase; detectDragGestures skips taps.
                                                    awaitEachGesture {
                                                        val down = awaitFirstDown()
                                                        down.consume()
                                                        val path = mutableListOf(
                                                            down.position.toImageOffset(
                                                                size.width.toFloat(),
                                                                size.height.toFloat(),
                                                                image.width,
                                                                image.height,
                                                            ),
                                                        )
                                                        activePoints = path.toList()

                                                        while (true) {
                                                            val event = awaitPointerEvent()
                                                            val change = event.changes.firstOrNull {
                                                                it.id == down.id
                                                            } ?: break
                                                            if (change.changedToUp()) {
                                                                change.consume()
                                                                break
                                                            }
                                                            if (change.pressed) {
                                                                change.consume()
                                                                path += change.position.toImageOffset(
                                                                    size.width.toFloat(),
                                                                    size.height.toFloat(),
                                                                    image.width,
                                                                    image.height,
                                                                )
                                                                activePoints = path.toList()
                                                            }
                                                        }

                                                        val points = path.toList()
                                                        activePoints = null
                                                        if (points.isNotEmpty()) {
                                                            onStrokesChange(
                                                                tabId,
                                                                eraseStrokesAlongPath(
                                                                    strokes,
                                                                    points,
                                                                    EraserRadiusPx,
                                                                ),
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                ) {
                                    val scaleX = size.width / image.width.toFloat()
                                    val scaleY = size.height / image.height.toFloat()
                                    val visibleStrokes =
                                        if (selectedTool == EditorTool.Eraser && activePoints != null) {
                                            eraseStrokesAlongPath(
                                                strokes,
                                                activePoints.orEmpty(),
                                                EraserRadiusPx,
                                            )
                                        } else {
                                            strokes
                                        }
                                    visibleStrokes.forEach { stroke ->
                                        drawAnnotationStroke(stroke, scaleX, scaleY)
                                    }
                                    if (selectedTool == EditorTool.Pen || selectedTool == EditorTool.Marker) {
                                        activePoints?.let { points ->
                                            if (points.size >= 2) {
                                                drawAnnotationStroke(
                                                    StrokeAnnotation(
                                                        points = points,
                                                        color = selectedColor.copy(
                                                            alpha = activeStrokeSettings.alpha,
                                                        ),
                                                        widthPx = activeStrokeSettings.widthPx,
                                                    ),
                                                    scaleX,
                                                    scaleY,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(verticalScroll),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight(),
                        )
                        HorizontalScrollbar(
                            adapter = rememberScrollbarAdapter(horizontalScroll),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(end = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun Offset.toImageOffset(
    canvasWidth: Float,
    canvasHeight: Float,
    imageWidth: Int,
    imageHeight: Int,
): Offset {
    val x = (x / canvasWidth * imageWidth).coerceIn(0f, imageWidth.toFloat())
    val y = (y / canvasHeight * imageHeight).coerceIn(0f, imageHeight.toFloat())
    return Offset(x, y)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAnnotationStroke(
    stroke: StrokeAnnotation,
    scaleX: Float,
    scaleY: Float,
) {
    val points = stroke.points
    if (points.size < 2) return
    val path = Path().apply {
        val first = points.first()
        moveTo(first.x * scaleX, first.y * scaleY)
        for (i in 1 until points.size) {
            val p = points[i]
            lineTo(p.x * scaleX, p.y * scaleY)
        }
    }
    drawPath(
        path = path,
        color = stroke.color,
        style = Stroke(
            width = stroke.widthPx * ((scaleX + scaleY) / 2f),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}
