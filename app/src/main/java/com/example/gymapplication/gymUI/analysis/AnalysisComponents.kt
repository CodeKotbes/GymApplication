package com.example.gymapplication.gymUI.analysis

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PremiumDonutChart(
    data: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 50f,
    highlightedIndices: List<Int> = emptyList(),
    outlineColor: Color = Color.White
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animateSweep by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "sweep_anim"
    )

    LaunchedEffect(Unit) { animationPlayed = true }

    Canvas(modifier = modifier) {
        drawArc(
            color = Color.Gray.copy(alpha = 0.15f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth),
            size = size
        )

        val total = data.sum()
        var startAngle = -90f

        data.forEachIndexed { index, value ->
            val sweepAngle = (value / total) * 360f * animateSweep
            val gap = if (data.size > 1 && sweepAngle > 6f) 4f else 0f
            val actualSweep = maxOf(0.1f, sweepAngle - gap)

            if (sweepAngle > 0f) {
                val isHighlighted =
                    highlightedIndices.isNotEmpty() && highlightedIndices.contains(index)
                val isAnyHighlighted = highlightedIndices.isNotEmpty()
                val targetAlpha = if (!isAnyHighlighted || isHighlighted) 1f else 0.25f
                val segmentColor = colors[index % colors.size].copy(alpha = targetAlpha)

                if (isHighlighted) {
                    drawArc(
                        color = outlineColor,
                        startAngle = startAngle + (gap / 2),
                        sweepAngle = actualSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth + 8f, cap = StrokeCap.Round),
                        size = size
                    )
                }

                drawArc(
                    color = segmentColor,
                    startAngle = startAngle + (gap / 2),
                    sweepAngle = actualSweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = size
                )
            }
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

    LaunchedEffect(scrollState.maxValue, displayPoints.size) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    val itemWidthPx = with(density) { (70.dp * zoomScale).toPx() }

    Row(modifier = modifier.padding(vertical = 16.dp)) {

        Canvas(
            modifier = Modifier
                .width(55.dp)
                .fillMaxHeight()
                .padding(end = 8.dp)
        ) {
            val graphHeight = size.height - 60f
            if (graphHeight <= 0) return@Canvas

            ySteps.forEach { valWeight ->
                val normY = 1f - ((valWeight - minValue) / valueRange)
                val y = (normY * graphHeight).coerceIn(0f, graphHeight)

                val formattedValue = if (valueRange < 10f) {
                    String.format(Locale.getDefault(), "%.1f", valWeight)
                } else {
                    valWeight.roundToInt().toString()
                }

                val textLayout = textMeasurer.measure(
                    text = "$formattedValue $unit",
                    style = TextStyle(
                        color = onSurfaceColor.copy(alpha = 0.7f),
                        fontSize = density.run { 10.sp },
                        fontWeight = FontWeight.Medium
                    )
                )
                drawText(
                    textLayout,
                    topLeft = Offset(
                        size.width - textLayout.size.width,
                        y - textLayout.size.height / 2f
                    )
                )
            }
        }

        var boxModifier = Modifier
            .weight(1f)
            .horizontalScroll(scrollState)

        if (isFullView) {
            boxModifier = boxModifier
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val index = ((offset.x - 30f) / itemWidthPx).roundToInt()
                            .coerceIn(0, displayPoints.size - 1)
                        selectedIndex = if (selectedIndex == index) null else index
                    }
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.size > 1) {
                                val zoomChange = event.calculateZoom()
                                zoomScale = (zoomScale * zoomChange).coerceIn(0.5f, 3f)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
        } else {
            boxModifier = boxModifier.clickable { onGraphClick?.invoke() }
        }

        Box(modifier = boxModifier) {
            Canvas(
                modifier = Modifier
                    .width(with(density) {
                        ((70.dp * zoomScale * (displayPoints.size - 1)) + 60.dp).coerceAtLeast(
                            100.dp
                        )
                    })
                    .fillMaxHeight()
            ) {
                val graphHeight = size.height - 60f
                if (graphHeight <= 0) return@Canvas

                val points = displayPoints.mapIndexed { index, point ->
                    val x = index * itemWidthPx + 30f
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
                                p0.x + (p1.x - p0.x) / 2, p0.y,
                                p0.x + (p1.x - p0.x) / 2, p1.y,
                                p1.x, p1.y
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
                    val dayLayout = textMeasurer.measure(
                        dayStr,
                        style = TextStyle(
                            color = onSurfaceColor,
                            fontSize = density.run { 10.sp },
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        dayLayout,
                        topLeft = Offset(offset.x - dayLayout.size.width / 2f, graphHeight + 10f)
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
    val bwSteps = listOf(maxBW, maxBW - (rangeBW * 0.5f), minBW)

    val maxStr = strengths.maxOf { it.value }
    val minStr = strengths.minOf { it.value }
    val rangeStr = if (maxStr == minStr) 10f else (maxStr - minStr)
    val strSteps = listOf(maxStr, maxStr - (rangeStr * 0.5f), minStr)

    val bwColor = Color(0xFF1E88E5)
    val strColor = Color(0xFF43A047)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState.maxValue, displayDates.size) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    val itemWidthPx = with(density) { (70.dp * zoomScale).toPx() }

    Row(modifier = modifier.padding(vertical = 16.dp)) {

        Canvas(modifier = Modifier
            .width(45.dp)
            .fillMaxHeight()
            .padding(end = 4.dp)) {
            val graphHeight = size.height - 60f
            if (graphHeight <= 0) return@Canvas
            bwSteps.forEach { value ->
                val normY = 1f - ((value - minBW) / rangeBW)
                val y = (normY * graphHeight).coerceIn(0f, graphHeight)

                val formattedValue = if (rangeBW < 10f) String.format(
                    Locale.getDefault(),
                    "%.1f",
                    value
                ) else value.roundToInt().toString()

                val textLayout = textMeasurer.measure(
                    "${formattedValue}kg",
                    style = TextStyle(
                        color = bwColor,
                        fontSize = density.run { 10.sp },
                        fontWeight = FontWeight.Bold
                    )
                )
                drawText(
                    textLayout,
                    topLeft = Offset(
                        size.width - textLayout.size.width,
                        y - textLayout.size.height / 2f
                    )
                )
            }
        }

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
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.size > 1) {
                                val zoomChange = event.calculateZoom()
                                zoomScale = (zoomScale * zoomChange).coerceIn(0.5f, 3f)
                                event.changes.forEach { it.consume() }
                            }
                        }
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
                        moveTo(bwPoints[0].x, bwPoints[0].y)
                        for (i in 1 until bwPoints.size) lineTo(bwPoints[i].x, bwPoints[i].y)
                    }
                }
                drawPath(bwPath, bwColor, style = Stroke(width = 6f, cap = StrokeCap.Round))

                val strPath = Path().apply {
                    if (strPoints.isNotEmpty()) {
                        moveTo(strPoints[0].x, strPoints[0].y)
                        for (i in 1 until strPoints.size) lineTo(strPoints[i].x, strPoints[i].y)
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
                    val dayLayout = textMeasurer.measure(
                        dayStr,
                        style = TextStyle(
                            color = onSurfaceColor,
                            fontSize = density.run { 10.sp },
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        dayLayout,
                        topLeft = Offset(x - dayLayout.size.width / 2f, graphHeight + 10f)
                    )
                }
            }
        }

        Canvas(modifier = Modifier
            .width(45.dp)
            .fillMaxHeight()
            .padding(start = 4.dp)) {
            val graphHeight = size.height - 60f
            if (graphHeight <= 0) return@Canvas
            strSteps.forEach { value ->
                val normY = 1f - ((value - minStr) / rangeStr)
                val y = (normY * graphHeight).coerceIn(0f, graphHeight)

                val formattedValue = if (rangeStr < 10f) String.format(
                    Locale.getDefault(),
                    "%.1f",
                    value
                ) else value.roundToInt().toString()

                val textLayout = textMeasurer.measure(
                    "${formattedValue}kg",
                    style = TextStyle(
                        color = strColor,
                        fontSize = density.run { 10.sp },
                        fontWeight = FontWeight.Bold
                    )
                )
                drawText(textLayout, topLeft = Offset(0f, y - textLayout.size.height / 2f))
            }
        }
    }
}
