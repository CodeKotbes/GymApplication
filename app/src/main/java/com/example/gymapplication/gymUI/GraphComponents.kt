package com.example.gymapplication.gymUI

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class GraphDataPoint(val value: Float, val dateMillis: Long)

@Composable
fun GenericGraph(
    dataPoints: List<GraphDataPoint>,
    unit: String,
    modifier: Modifier,
    isFullView: Boolean = false
) {
    val sortedPoints =
        remember(dataPoints) { dataPoints.sortedBy { it.dateMillis }.distinctBy { it.dateMillis } }
    val displayPoints = if (isFullView) sortedPoints else sortedPoints.takeLast(15)

    if (displayPoints.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Sammle mehr Daten...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxValue = displayPoints.maxOf { it.value }
    val minValue = displayPoints.minOf { it.value }
    val valueRange = if (maxValue == minValue) 10f else (maxValue - minValue)
    val ySteps =
        listOf(maxValue, maxValue - (valueRange * 0.33f), maxValue - (valueRange * 0.66f), minValue)

    val lineColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(displayPoints.size) { scrollState.scrollTo(scrollState.maxValue) }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    val itemWidthPx = with(density) { (70.dp * zoomScale).toPx() }

    Row(modifier = modifier.padding(vertical = 16.dp)) {
        Column(
            modifier = Modifier
                .width(65.dp)
                .fillMaxHeight()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            ySteps.forEach {
                Text(
                    text = "${it.roundToInt()} $unit",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceColor.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState)
                .pointerInput(isFullView, displayPoints.size) {
                    if (isFullView) {
                        detectTapGestures { offset ->
                            val index = ((offset.x - 60f) / itemWidthPx).roundToInt()
                                .coerceIn(0, displayPoints.size - 1)
                            selectedIndex = if (selectedIndex == index) null else index
                        }
                    }
                }
                .pointerInput(isFullView) {
                    if (isFullView) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScale = (zoomScale * zoom).coerceIn(0.5f, 3f)
                            coroutineScope.launch { scrollState.scrollBy(-pan.x) }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier
                .width(with(density) {
                    ((70.dp * zoomScale * (displayPoints.size - 1)) + 120.dp).coerceAtLeast(
                        100.dp
                    )
                })
                .fillMaxHeight()) {
                val graphHeight = size.height - 60f
                if (graphHeight <= 0) return@Canvas
                val points = displayPoints.mapIndexed { index, point ->
                    val x = index * itemWidthPx + 60f
                    val normalizedY = 1f - ((point.value - minValue) / valueRange)
                    Offset(x, (normalizedY * graphHeight).coerceIn(0f, graphHeight))
                }

                ySteps.forEach { valWeight ->
                    val normY = 1f - ((valWeight - minValue) / valueRange)
                    val y = (normY * graphHeight).coerceIn(0f, graphHeight)
                    drawLine(
                        color = onSurfaceColor.copy(alpha = 0.1f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }

                val linePath = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points[0].x, points[0].y)
                        for (i in 0 until points.size - 1) {
                            val p0 = points[i];
                            val p1 = points[i + 1]
                            cubicTo(
                                p0.x + (p1.x - p0.x) / 2,
                                p0.y,
                                p0.x + (p1.x - p0.x) / 2,
                                p1.y,
                                p1.x,
                                p1.y
                            )
                        }
                    }
                }
                drawPath(linePath, lineColor, style = Stroke(width = 6f, cap = StrokeCap.Round))

                points.forEachIndexed { index, offset ->
                    val isSelected = isFullView && selectedIndex == index
                    drawCircle(
                        if (isSelected) Color.White else lineColor,
                        radius = if (isSelected) 10f else 6f,
                        center = offset
                    )

                    if (isSelected) {
                        val labelText = "${displayPoints[index].value} $unit"
                        val textLayout = textMeasurer.measure(
                            labelText,
                            style = TextStyle(fontWeight = FontWeight.Bold, color = Color.Black)
                        )
                        val tooltipX = (offset.x - textLayout.size.width / 2f).coerceAtLeast(5f)
                        drawRoundRect(
                            color = lineColor,
                            topLeft = Offset(tooltipX, offset.y - 75f),
                            size = Size(textLayout.size.width + 20f, textLayout.size.height + 10f),
                            cornerRadius = CornerRadius(15f, 15f)
                        )
                        drawText(textLayout, topLeft = Offset(tooltipX + 10f, offset.y - 70f))
                    }

                    val dayStr = SimpleDateFormat(
                        "dd",
                        Locale.getDefault()
                    ).format(Date(displayPoints[index].dateMillis))
                    drawText(
                        textMeasurer = textMeasurer,
                        text = dayStr,
                        topLeft = Offset(offset.x - 12f, graphHeight + 10f),
                        style = TextStyle(
                            color = onSurfaceColor,
                            fontSize = density.run { 10.sp },
                            fontWeight = FontWeight.Bold
                        )
                    )

                    if (isFullView && index % 4 == 0) {
                        val monthStr = SimpleDateFormat(
                            "MMM",
                            Locale.getDefault()
                        ).format(Date(displayPoints[index].dateMillis))
                        drawText(
                            textMeasurer = textMeasurer,
                            text = monthStr,
                            topLeft = Offset(offset.x - 15f, graphHeight + 35f),
                            style = TextStyle(
                                color = onSurfaceColor.copy(alpha = 0.5f),
                                fontSize = density.run { 8.sp })
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullscreenGraphDialog(dataPoints: List<GraphDataPoint>, unit: String, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GESAMTVERLAUF",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Schließen",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.large
                        )
                        .padding(8.dp)
                ) {
                    GenericGraph(
                        dataPoints = dataPoints,
                        unit = unit,
                        modifier = Modifier.fillMaxSize(),
                        isFullView = true
                    )
                }
                Text(
                    text = "Nutze zwei Finger zum Zoomen • Tippe auf Punkte für Details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 16.dp, bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
fun HistoryZoomDialog(imageUri: String, onClose: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale == 1f) {
                            offset = Offset.Zero
                        } else {
                            offset += pan
                        }
                    }
                }) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) { Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.White) }
        }
    }
}

fun android.content.Context.createImageFile(): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return File.createTempFile("JPEG_${timeStamp}_", ".jpg", cacheDir)
}