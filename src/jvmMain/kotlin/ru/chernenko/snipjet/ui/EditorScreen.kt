package ru.chernenko.snipjet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint as SkiaPaint
import org.jetbrains.skia.PaintMode
import ru.chernenko.snipjet.clipboard.ImageClipboard
import ru.chernenko.snipjet.clipboard.LinuxImageClipboard
import ru.chernenko.snipjet.config.MessageKeys
import ru.chernenko.snipjet.config.Messages
import ru.chernenko.snipjet.editor.EditorAnnotation
import ru.chernenko.snipjet.editor.EditorTab
import ru.chernenko.snipjet.editor.StrokeAnnotation
import ru.chernenko.snipjet.editor.TextAnnotation
import ru.chernenko.snipjet.editor.composeAnnotatedPng
import ru.chernenko.snipjet.editor.eraseAnnotationsAlongPath
import ru.chernenko.snipjet.editor.matchTypeface
import ru.chernenko.snipjet.editor.resolveDefaultTextFontFamily
import ru.chernenko.snipjet.editor.systemFontFamilies
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private data class ToolStrokeSettings(
    val alpha: Float,
    val widthPx: Float,
)

private data class TextToolSettings(
    val alpha: Float,
    val sizePt: Int,
)

private val DefaultPenSettings = ToolStrokeSettings(alpha = 1f, widthPx = 4f)
private val DefaultMarkerSettings = ToolStrokeSettings(alpha = 0.45f, widthPx = 16f)
private val DefaultTextSettings = TextToolSettings(alpha = 1f, sizePt = DefaultTextFontSizePt)
private const val EraserRadiusPx = 20f
private const val TextLineHeightFactor = 1.2f
private const val CaretBlinkMs = 530L

@Composable
fun EditorScreen(
    tab: EditorTab,
    tabs: List<EditorTab>,
    onSelectTab: (Long) -> Unit,
    onCloseTab: (Long) -> Unit,
    onAnnotationsChange: (tabId: Long, annotations: List<EditorAnnotation>) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    undoEnabled: Boolean,
    redoEnabled: Boolean,
    onNewCapture: () -> Unit,
    clipboard: ImageClipboard = remember { LinuxImageClipboard() },
) {
    val image = tab.image
    val annotations = tab.annotations
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val imageWidth = with(density) { image.width.toDp() }
    val imageHeight = with(density) { image.height.toDp() }
    val focusRequester = remember { FocusRequester() }
    val fontFamilies = remember { systemFontFamilies() }
    val defaultFont = remember(fontFamilies) { resolveDefaultTextFontFamily(fontFamilies) }

    var selectedTool by remember { mutableStateOf(EditorTool.Pen) }
    var colorPanelOpen by remember { mutableStateOf(true) }
    var selectedColor by remember { mutableStateOf(EditorPaletteColors.first()) }
    var penSettings by remember { mutableStateOf(DefaultPenSettings) }
    var markerSettings by remember { mutableStateOf(DefaultMarkerSettings) }
    var textSettings by remember { mutableStateOf(DefaultTextSettings) }
    var textFontFamily by remember { mutableStateOf(defaultFont) }
    var textBold by remember { mutableStateOf(false) }
    var textItalic by remember { mutableStateOf(false) }
    var textUnderline by remember { mutableStateOf(false) }
    var copyInProgress by remember { mutableStateOf(false) }
    var saveInProgress by remember { mutableStateOf(false) }
    var pendingTextPoint by remember { mutableStateOf<Offset?>(null) }
    var textDraft by remember { mutableStateOf("") }
    var textCaretIndex by remember { mutableStateOf(0) }
    var caretVisible by remember { mutableStateOf(true) }

    val activeStrokeSettings = when (selectedTool) {
        EditorTool.Marker -> markerSettings
        else -> penSettings
    }
    val enabledTools = remember {
        setOf(EditorTool.Pen, EditorTool.Marker, EditorTool.Eraser, EditorTool.Text)
    }
    val canvasBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val dotColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val textSizePx = fontSizePtToPx(textSettings.sizePt)
    val isEditingText = pendingTextPoint != null

    fun cancelTextDraft() {
        pendingTextPoint = null
        textDraft = ""
        textCaretIndex = 0
    }

    fun confirmText() {
        val point = pendingTextPoint
        val value = textDraft
        cancelTextDraft()
        if (point == null || value.isBlank()) return
        onAnnotationsChange(
            tab.id,
            annotations + TextAnnotation(
                position = point,
                text = value,
                color = selectedColor.copy(alpha = textSettings.alpha),
                sizePx = textSizePx,
                fontFamily = textFontFamily,
                bold = textBold,
                italic = textItalic,
                underline = textUnderline,
            ),
        )
    }

    fun moveTextCaret(index: Int) {
        textCaretIndex = index.coerceIn(0, textDraft.length)
        caretVisible = true
    }

    fun insertTextAtCaret(value: String) {
        val index = textCaretIndex.coerceIn(0, textDraft.length)
        textDraft = textDraft.substring(0, index) + value + textDraft.substring(index)
        moveTextCaret(index + value.length)
    }

    LaunchedEffect(tab.id) {
        cancelTextDraft()
        focusRequester.requestFocus()
    }

    LaunchedEffect(pendingTextPoint, textDraft, textCaretIndex) {
        if (pendingTextPoint == null) return@LaunchedEffect
        caretVisible = true
        while (true) {
            delay(CaretBlinkMs)
            caretVisible = !caretVisible
        }
    }

    fun copyToClipboard() {
        if (copyInProgress) return
        copyInProgress = true
        val annotationsSnapshot = annotations
        val imageSnapshot = image
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val pngBytes = composeAnnotatedPng(imageSnapshot, annotationsSnapshot)
                    clipboard.copyPngBytes(pngBytes)
                }
            } catch (_: Exception) {
                // Keep editor usable; clipboard errors are non-fatal here.
            } finally {
                copyInProgress = false
            }
        }
    }

    fun saveToFile() {
        if (saveInProgress) return
        saveInProgress = true
        val annotationsSnapshot = annotations
        val imageSnapshot = image
        scope.launch {
            try {
                val path = withContext(Dispatchers.IO) {
                    chooseSavePngPath()
                } ?: return@launch
                withContext(Dispatchers.IO) {
                    val pngBytes = composeAnnotatedPng(imageSnapshot, annotationsSnapshot)
                    File(path).writeBytes(pngBytes)
                }
            } catch (_: Exception) {
                // Keep editor usable; save errors are non-fatal here.
            } finally {
                saveInProgress = false
            }
        }
    }

    fun handleTextTyping(event: androidx.compose.ui.input.key.KeyEvent): Boolean {
        if (!isEditingText) return false
        val shortcut = event.isCtrlPressed || event.isMetaPressed
        val caret = textCaretIndex.coerceIn(0, textDraft.length)
        when {
            event.key == Key.Escape -> {
                cancelTextDraft()
                return true
            }
            (event.key == Key.Enter || event.key == Key.NumPadEnter) && shortcut -> {
                insertTextAtCaret("\n")
                return true
            }
            event.key == Key.Enter || event.key == Key.NumPadEnter -> {
                confirmText()
                return true
            }
            event.key == Key.Backspace -> {
                if (caret > 0) {
                    textDraft = textDraft.removeRange(caret - 1, caret)
                    moveTextCaret(caret - 1)
                }
                return true
            }
            event.key == Key.Delete -> {
                if (caret < textDraft.length) {
                    textDraft = textDraft.removeRange(caret, caret + 1)
                    moveTextCaret(caret)
                }
                return true
            }
            event.key == Key.DirectionLeft -> {
                moveTextCaret(caret - 1)
                return true
            }
            event.key == Key.DirectionRight -> {
                moveTextCaret(caret + 1)
                return true
            }
            event.key == Key.DirectionUp -> {
                moveTextCaret(moveCaretVertically(textDraft, caret, up = true))
                return true
            }
            event.key == Key.DirectionDown -> {
                moveTextCaret(moveCaretVertically(textDraft, caret, up = false))
                return true
            }
            event.key == Key.MoveHome -> {
                moveTextCaret(
                    if (shortcut) 0 else lineStartIndex(textDraft, caret),
                )
                return true
            }
            event.key == Key.MoveEnd -> {
                moveTextCaret(
                    if (shortcut) textDraft.length else lineEndIndex(textDraft, caret),
                )
                return true
            }
            isNonTypingKey(event.key) -> return true
            shortcut -> return false
            else -> {
                val code = event.utf16CodePoint
                if (isTextInputCodePoint(code)) {
                    insertTextAtCaret(code.toChar().toString())
                    return true
                }
                return false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (handleTextTyping(event)) return@onPreviewKeyEvent true
                val shortcut = event.isCtrlPressed || event.isMetaPressed
                when {
                    shortcut && event.key == Key.Z && undoEnabled -> {
                        onUndo()
                        true
                    }
                    shortcut && event.key == Key.Y && redoEnabled -> {
                        onRedo()
                        true
                    }
                    else -> false
                }
            },
    ) {
        EditorTopBar(
            onUndo = onUndo,
            onRedo = onRedo,
            onCopy = ::copyToClipboard,
            onSave = ::saveToFile,
            onNewCapture = onNewCapture,
            undoEnabled = undoEnabled,
            redoEnabled = redoEnabled,
            copyEnabled = !copyInProgress,
            saveEnabled = !saveInProgress,
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
                    if (isEditingText && tool != EditorTool.Text) {
                        confirmText()
                    }
                    val usesColorPanel = tool == EditorTool.Pen ||
                        tool == EditorTool.Marker ||
                        tool == EditorTool.Text
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
                    alpha = if (selectedTool == EditorTool.Text) {
                        textSettings.alpha
                    } else {
                        activeStrokeSettings.alpha
                    },
                    onAlphaChange = { alpha ->
                        when (selectedTool) {
                            EditorTool.Marker -> markerSettings = markerSettings.copy(alpha = alpha)
                            EditorTool.Text -> textSettings = textSettings.copy(alpha = alpha)
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
                    showTextOptions = selectedTool == EditorTool.Text,
                    fontFamily = textFontFamily,
                    fontFamilies = fontFamilies,
                    onFontFamilyChange = { textFontFamily = it },
                    fontSizePt = textSettings.sizePt,
                    onFontSizePtChange = { textSettings = textSettings.copy(sizePt = it) },
                    bold = textBold,
                    onBoldChange = { textBold = it },
                    italic = textItalic,
                    onItalicChange = { textItalic = it },
                    underline = textUnderline,
                    onUnderlineChange = { textUnderline = it },
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
                                            textSettings,
                                            textFontFamily,
                                            textBold,
                                            textItalic,
                                            textUnderline,
                                            textSizePx,
                                            pendingTextPoint,
                                            textDraft,
                                            image.width,
                                            image.height,
                                            annotations,
                                        ) {
                                            val tabId = tab.id
                                            when (selectedTool) {
                                                EditorTool.Pen, EditorTool.Marker -> {
                                                    val strokeWidth = activeStrokeSettings.widthPx
                                                    val strokeColor = selectedColor.copy(
                                                        alpha = activeStrokeSettings.alpha,
                                                    )
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
                                                            activePoints =
                                                                (activePoints ?: emptyList()) + point
                                                        },
                                                        onDragEnd = {
                                                            val points = activePoints
                                                            activePoints = null
                                                            if (points != null && points.size >= 2) {
                                                                onAnnotationsChange(
                                                                    tabId,
                                                                    annotations + StrokeAnnotation(
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
                                                            onAnnotationsChange(
                                                                tabId,
                                                                eraseAnnotationsAlongPath(
                                                                    annotations,
                                                                    points,
                                                                    EraserRadiusPx,
                                                                ),
                                                            )
                                                        }
                                                    }
                                                }
                                                EditorTool.Text -> {
                                                    detectTapGestures { tap ->
                                                        val point = tap.toImageOffset(
                                                            size.width.toFloat(),
                                                            size.height.toFloat(),
                                                            image.width,
                                                            image.height,
                                                        )
                                                        if (isEditingText) {
                                                            confirmText()
                                                        }
                                                        pendingTextPoint = point
                                                        textDraft = ""
                                                        textCaretIndex = 0
                                                        focusRequester.requestFocus()
                                                    }
                                                }
                                            }
                                        },
                                ) {
                                    val scaleX = size.width / image.width.toFloat()
                                    val scaleY = size.height / image.height.toFloat()
                                    val visibleAnnotations =
                                        if (selectedTool == EditorTool.Eraser && activePoints != null) {
                                            eraseAnnotationsAlongPath(
                                                annotations,
                                                activePoints.orEmpty(),
                                                EraserRadiusPx,
                                            )
                                        } else {
                                            annotations
                                        }
                                    visibleAnnotations.forEach { annotation ->
                                        drawAnnotation(annotation, scaleX, scaleY)
                                    }
                                    if (selectedTool == EditorTool.Pen || selectedTool == EditorTool.Marker) {
                                        activePoints?.let { points ->
                                            if (points.size >= 2) {
                                                drawAnnotation(
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
                                    pendingTextPoint?.let { point ->
                                        val draftAnnotation = TextAnnotation(
                                            position = point,
                                            text = textDraft,
                                            color = selectedColor.copy(alpha = textSettings.alpha),
                                            sizePx = textSizePx,
                                            fontFamily = textFontFamily,
                                            bold = textBold,
                                            italic = textItalic,
                                            underline = textUnderline,
                                        )
                                        if (textDraft.isNotEmpty()) {
                                            drawAnnotationText(draftAnnotation, scaleX, scaleY)
                                        }
                                        if (caretVisible) {
                                            drawTextCaret(
                                                draftAnnotation,
                                                scaleX,
                                                scaleY,
                                                textCaretIndex,
                                            )
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

private fun DrawScope.drawAnnotation(
    annotation: EditorAnnotation,
    scaleX: Float,
    scaleY: Float,
) {
    when (annotation) {
        is StrokeAnnotation -> drawAnnotationStroke(annotation, scaleX, scaleY)
        is TextAnnotation -> drawAnnotationText(annotation, scaleX, scaleY)
    }
}

private fun DrawScope.drawAnnotationStroke(
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

private fun DrawScope.drawAnnotationText(
    text: TextAnnotation,
    scaleX: Float,
    scaleY: Float,
) {
    if (text.text.isEmpty()) return
    val scale = (scaleX + scaleY) / 2f
    val sizePx = text.sizePx * scale
    val lineHeight = sizePx * TextLineHeightFactor
    val typeface = matchTypeface(text.fontFamily, text.bold, text.italic)
    val font = Font(typeface, sizePx)
    val originX = text.position.x * scaleX
    val originY = text.position.y * scaleY
    val paint = SkiaPaint().apply {
        color = text.color.toArgb()
        isAntiAlias = true
        mode = PaintMode.FILL
    }
    try {
        val lines = text.text.split('\n')
        lines.forEachIndexed { index, line ->
            val y = originY + index * lineHeight
            drawContext.canvas.nativeCanvas.drawString(line, originX, y, font, paint)
            if (text.underline && line.isNotEmpty()) {
                val width = font.measureTextWidth(line)
                paint.mode = PaintMode.STROKE
                paint.strokeWidth = (sizePx * 0.08f).coerceAtLeast(1f)
                val underlineY = y + sizePx * 0.12f
                drawContext.canvas.nativeCanvas.drawLine(
                    originX,
                    underlineY,
                    originX + width,
                    underlineY,
                    paint,
                )
                paint.mode = PaintMode.FILL
            }
        }
    } finally {
        paint.close()
        font.close()
        typeface.close()
    }
}

private fun DrawScope.drawTextCaret(
    text: TextAnnotation,
    scaleX: Float,
    scaleY: Float,
    caretIndex: Int,
) {
    val scale = (scaleX + scaleY) / 2f
    val sizePx = text.sizePx * scale
    val lineHeight = sizePx * TextLineHeightFactor
    val index = caretIndex.coerceIn(0, text.text.length)
    val prefix = text.text.substring(0, index)
    val linesBefore = prefix.split('\n')
    val lineIndex = (linesBefore.size - 1).coerceAtLeast(0)
    val linePrefix = linesBefore.lastOrNull().orEmpty()
    val originX = text.position.x * scaleX
    val originY = text.position.y * scaleY
    val typeface = matchTypeface(text.fontFamily, text.bold, text.italic)
    val font = Font(typeface, sizePx)
    val caretX = try {
        originX + font.measureTextWidth(linePrefix)
    } finally {
        font.close()
        typeface.close()
    }
    val baseline = originY + lineIndex * lineHeight
    val top = baseline - sizePx
    drawLine(
        color = text.color,
        start = Offset(caretX, top),
        end = Offset(caretX, baseline),
        strokeWidth = 1.5f * scale.coerceAtLeast(1f),
    )
}

private fun lineStartIndex(text: String, index: Int): Int {
    val i = index.coerceIn(0, text.length)
    if (i <= 0) return 0
    val newline = text.lastIndexOf('\n', i - 1)
    return if (newline < 0) 0 else newline + 1
}

private fun lineEndIndex(text: String, index: Int): Int {
    val i = index.coerceIn(0, text.length)
    val newline = text.indexOf('\n', i)
    return if (newline < 0) text.length else newline
}

private fun moveCaretVertically(text: String, index: Int, up: Boolean): Int {
    val i = index.coerceIn(0, text.length)
    val start = lineStartIndex(text, i)
    val column = i - start
    if (up) {
        if (start == 0) return 0
        val prevEnd = start - 1
        val prevStart = lineStartIndex(text, prevEnd)
        val prevLength = prevEnd - prevStart
        return prevStart + column.coerceAtMost(prevLength)
    }
    val end = lineEndIndex(text, i)
    if (end >= text.length) return text.length
    val nextStart = end + 1
    val nextEnd = lineEndIndex(text, nextStart)
    val nextLength = nextEnd - nextStart
    return nextStart + column.coerceAtMost(nextLength)
}

private val SaveFileTimestampFormat = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

private fun isNonTypingKey(key: Key): Boolean = when (key) {
    Key.ShiftLeft, Key.ShiftRight, Key.CtrlLeft, Key.CtrlRight,
    Key.AltLeft, Key.AltRight, Key.MetaLeft, Key.MetaRight, Key.SoftLeft, Key.SoftRight,
    Key.Tab, Key.CapsLock, Key.NumLock, Key.ScrollLock, Key.Function,
    Key.Insert, Key.PageUp, Key.PageDown,
    Key.PrintScreen, Key.Menu,
    Key.F1, Key.F2, Key.F3, Key.F4, Key.F5, Key.F6,
    Key.F7, Key.F8, Key.F9, Key.F10, Key.F11, Key.F12,
    -> true
    else -> false
}

private fun isTextInputCodePoint(code: Int): Boolean {
    if (code <= 0) return false
    if (code in Character.MIN_SURROGATE.code..Character.MAX_SURROGATE.code) return false
    if (Character.isISOControl(code)) return false
    if (Character.isIdentifierIgnorable(code)) return false
    return when (Character.getType(code)) {
        Character.CONTROL.toInt(),
        Character.FORMAT.toInt(),
        Character.PRIVATE_USE.toInt(),
        Character.SURROGATE.toInt(),
        Character.UNASSIGNED.toInt(),
        -> false
        else -> true
    }
}

private fun chooseSavePngPath(): String? {
    val dialog = FileDialog(null as Frame?, Messages.get(MessageKeys.EDITOR_SAVE), FileDialog.SAVE)
    dialog.file = "snipjet-${LocalDateTime.now().format(SaveFileTimestampFormat)}.png"
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val fileName = dialog.file ?: return null
    val withExt = if (fileName.endsWith(".png", ignoreCase = true)) {
        fileName
    } else {
        "$fileName.png"
    }
    return File(directory, withExt).absolutePath
}

