package com.example.gymapplication.gymUI

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PremiumDonutChart(
    data: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 50f
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animateSweep by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "sweep_anim"
    )

    LaunchedEffect(Unit) { animationPlayed = true }

    Canvas(modifier = modifier) {
        val total = data.sum()
        var startAngle = -90f

        data.forEachIndexed { index, value ->
            val sweepAngle = (value / total) * 360f * animateSweep
            val gap = if (data.size > 1 && sweepAngle > 5f) (360f * 0.03f) else 0f
            val actualSweep = maxOf(0.1f, sweepAngle - gap)

            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle + (gap / 2),
                sweepAngle = actualSweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = size
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun AnalysisWorkloadGraph(
    dataPoints: List<GraphDataPoint>,
    unit: String,
    modifier: Modifier,
    isFullView: Boolean = false,
    onGraphClick: (() -> Unit)? = null
) {
    val displayPoints = if (isFullView) dataPoints else dataPoints.takeLast(15)

    if (displayPoints.size < 2) return

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

    LaunchedEffect(scrollState.maxValue, displayPoints.size) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

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
                    "${it.roundToInt()} $unit",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceColor.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        var boxModifier = Modifier
            .weight(1f)
            .horizontalScroll(scrollState)

        if (isFullView) {
            boxModifier = boxModifier
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val index = ((offset.x - 60f) / itemWidthPx).roundToInt()
                            .coerceIn(0, displayPoints.size - 1)
                        selectedIndex = if (selectedIndex == index) null else index
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.5f, 3f)
                        coroutineScope.launch { scrollState.scrollBy(-pan.x) }
                    }
                }
        } else {
            boxModifier = boxModifier.clickable { onGraphClick?.invoke() }
        }

        Box(modifier = boxModifier) {
            Canvas(
                modifier = Modifier
                    .width(with(density) {
                        ((70.dp * zoomScale * (displayPoints.size - 1)) + 120.dp).coerceAtLeast(
                            100.dp
                        )
                    })
                    .fillMaxHeight()
            ) {
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
                        val textLayout = textMeasurer.measure(
                            "${displayPoints[index].value} $unit",
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
                }
            }
        }
    }
}

@Composable
fun AnalysisEfficiencyGraph(
    bodyWeights: List<GraphDataPoint>,
    strengths: List<GraphDataPoint>,
    modifier: Modifier,
    isFullView: Boolean = false,
    onGraphClick: (() -> Unit)? = null
) {
    val allDates = remember(bodyWeights, strengths) {
        (bodyWeights.map { it.dateMillis } + strengths.map { it.dateMillis }).distinct().sorted()
    }
    val displayDates = if (isFullView) allDates else allDates.takeLast(15)

    if (displayDates.size < 2) return

    val maxBW = bodyWeights.maxOf { it.value }
    val minBW = bodyWeights.minOf { it.value }
    val rangeBW = if (maxBW == minBW) 10f else (maxBW - minBW)
    val maxStr = strengths.maxOf { it.value }
    val minStr = strengths.minOf { it.value }
    val rangeStr = if (maxStr == minStr) 10f else (maxStr - minStr)
    val bwColor = Color(0xFF1E88E5)
    val strColor = Color(0xFF43A047)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(scrollState.maxValue, displayDates.size) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    val itemWidthPx = with(density) { (70.dp * zoomScale).toPx() }

    Row(modifier = modifier.padding(vertical = 16.dp)) {

        var boxModifier = Modifier
            .weight(1f)
            .horizontalScroll(scrollState)

        if (isFullView) {
            boxModifier = boxModifier
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val index = ((offset.x - 30f) / itemWidthPx).roundToInt()
                            .coerceIn(0, displayDates.size - 1)
                        selectedIndex = if (selectedIndex == index) null else index
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.5f, 3f)
                        coroutineScope.launch { scrollState.scrollBy(-pan.x) }
                    }
                }
        } else {
            boxModifier = boxModifier.clickable { onGraphClick?.invoke() }
        }

        Box(modifier = boxModifier) {
            Canvas(
                modifier = Modifier
                    .width(with(density) {
                        ((70.dp * zoomScale * (displayDates.size - 1)) + 60.dp).coerceAtLeast(
                            100.dp
                        )
                    })
                    .fillMaxHeight()
            ) {
                val graphHeight = size.height - 60f
                if (graphHeight <= 0) return@Canvas

                val bwPoints = mutableListOf<Offset>()
                val strPoints = mutableListOf<Offset>()

                displayDates.forEachIndexed { index, time ->
                    val x = index * itemWidthPx + 30f
                    val bwMatch = bodyWeights.find { it.dateMillis == time }
                    if (bwMatch != null) bwPoints.add(
                        Offset(
                            x,
                            (1f - ((bwMatch.value - minBW) / rangeBW)) * graphHeight
                        )
                    )

                    val strMatch = strengths.find { it.dateMillis == time }
                    if (strMatch != null) strPoints.add(
                        Offset(
                            x,
                            (1f - ((strMatch.value - minStr) / rangeStr)) * graphHeight
                        )
                    )
                }

                val bwPath = Path().apply {
                    if (bwPoints.isNotEmpty()) {
                        moveTo(
                            bwPoints[0].x,
                            bwPoints[0].y
                        ); for (i in 1 until bwPoints.size) lineTo(bwPoints[i].x, bwPoints[i].y)
                    }
                }
                drawPath(bwPath, bwColor, style = Stroke(width = 6f, cap = StrokeCap.Round))

                val strPath = Path().apply {
                    if (strPoints.isNotEmpty()) {
                        moveTo(
                            strPoints[0].x,
                            strPoints[0].y
                        ); for (i in 1 until strPoints.size) lineTo(strPoints[i].x, strPoints[i].y)
                    }
                }
                drawPath(strPath, strColor, style = Stroke(width = 6f, cap = StrokeCap.Round))

                displayDates.forEachIndexed { index, time ->
                    val x = index * itemWidthPx + 30f
                    val bwMatch = bodyWeights.find { it.dateMillis == time }
                    val strMatch = strengths.find { it.dateMillis == time }
                    val isSelected = isFullView && selectedIndex == index

                    if (bwMatch != null) drawCircle(
                        if (isSelected) Color.White else bwColor,
                        radius = if (isSelected) 10f else 6f,
                        center = Offset(x, (1f - ((bwMatch.value - minBW) / rangeBW)) * graphHeight)
                    )
                    if (strMatch != null) drawCircle(
                        if (isSelected) Color.White else strColor,
                        radius = if (isSelected) 10f else 6f,
                        center = Offset(
                            x,
                            (1f - ((strMatch.value - minStr) / rangeStr)) * graphHeight
                        )
                    )

                    if (isSelected) {
                        var tooltipYOffset = 75f
                        listOfNotNull(
                            bwMatch?.let { "${it.value} kg" to bwColor },
                            strMatch?.let { "${it.value} kg" to strColor }
                        ).forEach { (text, color) ->
                            val textLayout = textMeasurer.measure(
                                text,
                                style = TextStyle(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                            val tooltipX = (x - textLayout.size.width / 2f).coerceAtLeast(5f)
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(tooltipX, graphHeight / 2 - tooltipYOffset),
                                size = Size(
                                    textLayout.size.width + 20f,
                                    textLayout.size.height + 10f
                                ),
                                cornerRadius = CornerRadius(15f, 15f)
                            )
                            drawText(
                                textLayout,
                                topLeft = Offset(
                                    tooltipX + 10f,
                                    graphHeight / 2 - tooltipYOffset + 5f
                                )
                            )
                            tooltipYOffset -= 45f
                        }
                    }

                    val dayStr = SimpleDateFormat("dd", Locale.getDefault()).format(Date(time))
                    drawText(
                        textMeasurer = textMeasurer,
                        text = dayStr,
                        topLeft = Offset(x - 12f, graphHeight + 10f),
                        style = TextStyle(
                            color = onSurfaceColor,
                            fontSize = density.run { 10.sp },
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisFullscreenWorkloadDialog(dataPoints: List<GraphDataPoint>, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "WORKLOAD VERLAUF",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Schließen"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.shapes.large
                        )
                        .padding(8.dp)
                ) {
                    AnalysisWorkloadGraph(
                        dataPoints = dataPoints,
                        unit = "kg",
                        modifier = Modifier.fillMaxSize(),
                        isFullView = true
                    )
                }
                Text(
                    "Nutze zwei Finger zum Zoomen • Tippe auf Punkte für Details",
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
fun AnalysisFullscreenEfficiencyDialog(
    bodyWeights: List<GraphDataPoint>,
    strengths: List<GraphDataPoint>,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "EFFICIENCY FACTOR",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Schließen"
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E88E5))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Körpergewicht", style = MaterialTheme.typography.labelMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF43A047))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Max. Kraft", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.shapes.large
                        )
                        .padding(8.dp)
                ) {
                    AnalysisEfficiencyGraph(
                        bodyWeights = bodyWeights,
                        strengths = strengths,
                        modifier = Modifier.fillMaxSize(),
                        isFullView = true
                    )
                }
                Text(
                    "Nutze zwei Finger zum Zoomen • Tippe auf Punkte für Details",
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