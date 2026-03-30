package com.example.gymapplication.gymUI.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymapplication.data.BodyMetric
import com.example.gymapplication.gymUI.ProgressSlider.InteractiveProgressSlider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Composable
fun ImageCompareDialog(
    metric1: BodyMetric,
    metric2: BodyMetric,
    unit: String,
    type: String,
    onClose: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val sorted = listOf(metric1, metric2).sortedBy { it.dateMillis }
    val m1 = sorted[0]
    val m2 = sorted[1]

    val diffInMillis = m2.dateMillis - m1.dateMillis
    val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
    val timePassedText = if (diffInDays > 30) "${diffInDays / 30} Monate" else "$diffInDays Tage"

    val diffValue = m2.value - m1.value
    val prefix = if (diffValue > 0) "+" else ""
    val diffColor = if (type.contains("Gewicht", ignoreCase = true)) {
        if (diffValue > 0) Color(0xFFF44336) else Color(0xFF4CAF50)
    } else {
        if (diffValue > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "INTERAKTIVER VERGLEICH",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Zeitraum: $timePassedText",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Schließen"
                        )
                    }
                }

                if (m1.imageUri != null && m2.imageUri != null) {
                    InteractiveProgressSlider(
                        beforeUri = m1.imageUri,
                        afterUri = m2.imageUri,
                        beforeDateMillis = m1.dateMillis,
                        beforeValue = m1.value,
                        afterDateMillis = m2.dateMillis,
                        afterValue = m2.value,
                        unit = unit,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (diffValue != 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = diffColor.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "GESAMTVERÄNDERUNG:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "$prefix${
                                        String.format(
                                            Locale.getDefault(),
                                            "%.1f",
                                            diffValue
                                        )
                                    } $unit",
                                    color = diffColor,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ProgressDashboardCard(
    title: String,
    unit: String,
    startValue: Float?,
    currentValue: Float?,
    previousValue: Float?,
    targetValue: Float?,
    onSetTargetClick: () -> Unit,
    isLowerBetter: Boolean,
    availableEntries: List<Pair<Long, Float>> = emptyList(),
    onStartSelected: (Long) -> Unit = {}
) {
    var startDropdownExpanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                if (availableEntries.isNotEmpty()) startDropdownExpanded = true
                            }
                            .padding(horizontal = 4.dp)
                    ) {
                        Text("START", style = MaterialTheme.typography.labelSmall)
                        Text(
                            if (startValue != null) "$startValue $unit" else "-",
                            fontWeight = FontWeight.Black
                        )
                    }

                    DropdownMenu(
                        expanded = startDropdownExpanded,
                        onDismissRequest = { startDropdownExpanded = false },
                        shape = MaterialTheme.shapes.medium,
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        availableEntries.forEachIndexed { index, entry ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            dateFormat.format(Date(entry.first)),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            "${entry.second} $unit",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = "Datum",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    onStartSelected(entry.first); startDropdownExpanded = false
                                }
                            )
                            if (index < availableEntries.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("AKTUELL", style = MaterialTheme.typography.labelSmall)
                    Text(
                        if (currentValue != null) "$currentValue $unit" else "-",
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSetTargetClick() }) {
                    Text("ZIEL", style = MaterialTheme.typography.labelSmall)
                    Text(
                        if (targetValue != null) "$targetValue $unit" else "Setzen",
                        fontWeight = FontWeight.Black
                    )
                }
            }

            if (targetValue != null && startValue != null && currentValue != null && startValue != targetValue) {
                Spacer(modifier = Modifier.height(16.dp))
                val totalDiff = targetValue - startValue
                val currentDiff = currentValue - startValue
                val rawProgress = currentDiff / totalDiff
                val progress = rawProgress.coerceIn(0f, 1f)

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(MaterialTheme.shapes.medium),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                Text(
                    "${(progress * 100).toInt()}% erreicht",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }

            if (currentValue != null && previousValue != null) {
                val diff = currentValue - previousValue
                if (abs(diff) >= 0.1f) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))

                    val isGood = if (isLowerBetter) diff < 0 else diff > 0
                    val color = if (isGood) Color(0xFF4CAF50) else Color(0xFFF44336)
                    val prefix = if (diff > 0) "+" else ""

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Veränderung zum letzten Mal: ",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "$prefix${String.format(Locale.getDefault(), "%.1f", diff)} $unit",
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}