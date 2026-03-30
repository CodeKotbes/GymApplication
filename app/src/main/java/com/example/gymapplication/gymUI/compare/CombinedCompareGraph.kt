package com.example.gymapplication.gymUI.compare

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.gymapplication.gymUI.analysis.GraphDataPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.indexOf
import kotlin.math.roundToInt

@Composable
fun CombinedCompareGraph(
    myData: List<GraphDataPoint>,
    friendData: List<GraphDataPoint>,
    myName: String,
    friendName: String,
    unit: String,
    modifier: Modifier,
    isFullView: Boolean = false
) {
    val allPoints = myData + friendData
    if (allPoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Noch keine Daten vorhanden.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val allUniqueDatesStr = allPoints.mapNotNull {
        try {
            dateFormat.format(Date(it.dateMillis))
        } catch (e: Exception) {
            null
        }
    }.distinct().sorted()
    val displayDates = if (isFullView) allUniqueDatesStr else allUniqueDatesStr.takeLast(15)

    val maxValue = allPoints.maxOf { it.value }
    val minValue = allPoints.minOf { it.value }.coerceAtMost(maxValue - 10f)
    val valueRange = if (maxValue - minValue > 0f) maxValue - minValue else 10f
    val ySteps =
        listOf(maxValue, maxValue - (valueRange * 0.33f), maxValue - (valueRange * 0.66f), minValue)
    val myColor = MaterialTheme.colorScheme.primary
    val friendColor = Color(0xFFF97316)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var mySelectedIndex by remember { mutableStateOf<Int?>(null) }
    var friendSelectedIndex by remember { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()

    LaunchedEffect(displayDates.size) { scrollState.scrollTo(scrollState.maxValue) }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    val itemWidthPx = with(density) { (70.dp * zoomScale).toPx() }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(myColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    myName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = myColor
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(friendColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    friendName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = friendColor
                )
            }
        }

        Row(modifier = Modifier.weight(1f)) {
            Canvas(modifier = Modifier
                .width(55.dp)
                .fillMaxHeight()
                .padding(end = 8.dp)) {
                val graphHeight = size.height - 60f
                if (graphHeight <= 0) return@Canvas

                ySteps.forEach { valWeight ->
                    val normY = 1f - ((valWeight - minValue) / valueRange)
                    val y = (normY * graphHeight).coerceIn(0f, graphHeight)
                    val formattedValue = if (valueRange < 10f) String.format(
                        Locale.getDefault(),
                        "%.1f",
                        valWeight
                    ) else valWeight.roundToInt().toString()
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

            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
                    .pointerInput(isFullView, displayDates.size) {
                        if (isFullView) {
                            detectTapGestures { offset ->
                                if (displayDates.isNotEmpty()) {
                                    val clickedIndex = ((offset.x - 30f) / itemWidthPx).roundToInt()
                                        .coerceIn(0, displayDates.size - 1)
                                    val clickedDateStr = displayDates[clickedIndex]

                                    val myPoint = myData.find {
                                        try {
                                            dateFormat.format(Date(it.dateMillis)) == clickedDateStr
                                        } catch (e: Exception) {
                                            false
                                        }
                                    }
                                    val friendPoint = friendData.find {
                                        try {
                                            dateFormat.format(Date(it.dateMillis)) == clickedDateStr
                                        } catch (e: Exception) {
                                            false
                                        }
                                    }

                                    if (myPoint != null || friendPoint != null) {
                                        mySelectedIndex = myData.indexOf(myPoint).takeIf { it >= 0 }
                                        friendSelectedIndex =
                                            friendData.indexOf(friendPoint).takeIf { it >= 0 }
                                    } else {
                                        mySelectedIndex = null; friendSelectedIndex = null
                                    }
                                }
                            }
                        }
                    }
                    .pointerInput(isFullView) {
                        if (isFullView) {
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
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .width(with(density) {
                            ((70.dp * zoomScale * displayDates.size) + 60.dp).coerceAtLeast(
                                100.dp
                            )
                        })
                        .fillMaxHeight()
                ) {
                    val graphHeight = size.height - 60f
                    if (graphHeight <= 0) return@Canvas

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

                    fun drawLineGraph(
                        dataList: List<GraphDataPoint>,
                        lineColor: Color,
                        isMe: Boolean
                    ) {
                        val pointsToDraw = mutableListOf<Pair<Offset, GraphDataPoint>>()

                        displayDates.forEachIndexed { index, dateStr ->
                            val point = dataList.find {
                                try {
                                    dateFormat.format(Date(it.dateMillis)) == dateStr
                                } catch (e: Exception) {
                                    false
                                }
                            }
                            if (point != null) {
                                val x = index * itemWidthPx + 30f
                                val normalizedY = 1f - ((point.value - minValue) / valueRange)
                                pointsToDraw.add(
                                    Offset(
                                        x,
                                        (normalizedY * graphHeight).coerceIn(0f, graphHeight)
                                    ) to point
                                )
                            }
                        }

                        val linePath = Path().apply {
                            if (pointsToDraw.isNotEmpty()) {
                                moveTo(pointsToDraw[0].first.x, pointsToDraw[0].first.y)
                                for (i in 0 until pointsToDraw.size - 1) {
                                    val p0 = pointsToDraw[i].first
                                    val p1 = pointsToDraw[i + 1].first
                                    cubicTo(
                                        p0.x + (p1.x - p0.x) / 2, p0.y,
                                        p0.x + (p1.x - p0.x) / 2, p1.y,
                                        p1.x, p1.y
                                    )
                                }
                            }
                        }
                        drawPath(
                            linePath,
                            lineColor,
                            style = Stroke(width = 6f, cap = StrokeCap.Round)
                        )

                        pointsToDraw.forEach { (offset, point) ->
                            val activeIndex = if (isMe) mySelectedIndex else friendSelectedIndex
                            val isSelected =
                                isFullView && activeIndex != null && activeIndex >= 0 && activeIndex < dataList.size && dataList[activeIndex] == point

                            drawCircle(
                                if (isSelected) Color.White else lineColor,
                                radius = if (isSelected) 10f else 6f,
                                center = offset
                            )

                            if (isSelected) {
                                val valStr = point.value
                                val textLayout = textMeasurer.measure(
                                    "$valStr $unit",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                val tooltipX =
                                    (offset.x - textLayout.size.width / 2f).coerceAtLeast(5f)
                                drawRoundRect(
                                    color = lineColor,
                                    topLeft = Offset(tooltipX, offset.y - 75f),
                                    size = Size(
                                        textLayout.size.width + 20f,
                                        textLayout.size.height + 10f
                                    ),
                                    cornerRadius = CornerRadius(15f, 15f)
                                )
                                drawText(
                                    textLayout,
                                    topLeft = Offset(tooltipX + 10f, offset.y - 70f)
                                )
                            }
                        }
                    }

                    drawLineGraph(friendData, friendColor, false)
                    drawLineGraph(myData, myColor, true)

                    displayDates.forEachIndexed { index, dateStr ->
                        val x = index * itemWidthPx + 30f
                        val dayStr = try {
                            dateStr.substring(8, 10)
                        } catch (e: Exception) {
                            ""
                        }
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

                        if (isFullView && index % 4 == 0) {
                            try {
                                val monthStr = SimpleDateFormat(
                                    "MMM",
                                    Locale.getDefault()
                                ).format(dateFormat.parse(dateStr)!!)
                                val monthLayout = textMeasurer.measure(
                                    monthStr,
                                    style = TextStyle(
                                        color = onSurfaceColor.copy(alpha = 0.5f),
                                        fontSize = density.run { 8.sp })
                                )
                                drawText(
                                    monthLayout,
                                    topLeft = Offset(
                                        x - monthLayout.size.width / 2f,
                                        graphHeight + 35f
                                    )
                                )
                            } catch (e: Exception) {
                            }
                        }
                    }
                }
            }
        }
    }
}