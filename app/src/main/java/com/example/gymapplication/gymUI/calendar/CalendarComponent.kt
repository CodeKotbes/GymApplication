package com.example.gymapplication.gymUI.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarZoomDialog(imageUri: String, onClose: () -> Unit) {
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
                }
        ) {
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
            ) {
                Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.White)
            }
        }
    }
}

@Composable
fun GymCalendar(
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    finishedDates: Set<String>,
    plannedDates: Set<String>
) {
    var currentMonthMillis by rememberSaveable { mutableLongStateOf(selectedDateMillis) }
    val locale = Locale.GERMANY

    val currentMonthCal = Calendar.getInstance(locale).apply {
        timeInMillis = currentMonthMillis
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val monthYearFormat = SimpleDateFormat("MMMM yyyy", locale)
    val monthYearString = monthYearFormat.format(currentMonthCal.time)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                currentMonthCal.add(Calendar.MONTH, -1)
                currentMonthMillis = currentMonthCal.timeInMillis
            }) { Icon(Icons.Default.ArrowBack, contentDescription = "Zurück") }

            Text(
                monthYearString.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )

            IconButton(onClick = {
                currentMonthCal.add(Calendar.MONTH, 1)
                currentMonthMillis = currentMonthCal.timeInMillis
            }) { Icon(Icons.Default.ArrowForward, contentDescription = "Vor") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val weekdays = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val firstDayOfWeek = currentMonthCal.get(Calendar.DAY_OF_WEEK)
        val startOffset = (firstDayOfWeek + 5) % 7
        val daysInMonth = currentMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7
        val dateFormat = SimpleDateFormat("yyyyMMdd", locale)
        val selectedStr = dateFormat.format(Date(selectedDateMillis))
        val todayStr = dateFormat.format(Date())

        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - startOffset + 1

                    if (dayNum in 1..daysInMonth) {
                        val cellCal = Calendar.getInstance(locale).apply {
                            timeInMillis = currentMonthMillis
                            set(Calendar.DAY_OF_MONTH, dayNum)
                        }
                        val dateStr = dateFormat.format(cellCal.time)
                        val isSelected = dateStr == selectedStr
                        val isToday = dateStr == todayStr

                        val hasFinished = finishedDates.contains(dateStr)
                        val hasPlanned = plannedDates.contains(dateStr)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onDateSelected(cellCal.timeInMillis) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNum.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isToday || isSelected) FontWeight.Black else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )

                            if (hasFinished || hasPlanned) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    if (hasFinished) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                    if (hasPlanned) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(
                                                        alpha = 0.7f
                                                    ) else MaterialTheme.colorScheme.secondary
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}