package com.example.gymapplication.gymUI.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymapplication.gymUI.GymViewModel
import java.util.Locale

@Composable
fun BodyProgressContent(viewModel: GymViewModel, onMetricClick: (String, String) -> Unit) {
    val metricTypes = listOf(
        "Gewicht" to "kg",
        "Bizeps" to "cm",
        "Bauch" to "cm",
        "Brust" to "cm",
        "Oberschenkel" to "cm"
    )
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                "Wähle eine Kategorie für Details",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        items(metricTypes) { (type, unit) ->
            BodyCategoryCard(
                type,
                unit,
                viewModel,
                onClick = { onMetricClick(type, unit) })
        }
        item { Spacer(modifier = Modifier.height(120.dp)) }
    }
}

@Composable
fun BodyCategoryCard(type: String, unit: String, viewModel: GymViewModel, onClick: () -> Unit) {
    val metrics by viewModel.getBodyMetrics(type).collectAsState(initial = emptyList())
    val trend by viewModel.getBodyMetricTrend(type).collectAsState(initial = null)
    val latestMetric = metrics.maxByOrNull { it.dateMillis }
    val goal by viewModel.weightGoal.collectAsState()
    val allTargets by viewModel.bodyTargets.collectAsState()
    val targetValue = allTargets.find { it.type == type }?.targetValue

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                type.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .weight(1.2f)
                    .padding(end = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (targetValue != null && metrics.isNotEmpty()) {
                val sorted = metrics.sortedBy { it.dateMillis }
                val startVal = sorted.first().value
                val currentVal = sorted.last().value
                if (startVal != targetValue) {
                    val rawProgress = (currentVal - startVal) / (targetValue - startVal)
                    val progress = rawProgress.coerceIn(0f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            "$targetValue $unit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(MaterialTheme.shapes.small),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    }
                } else Spacer(modifier = Modifier.weight(1f))
            } else Spacer(modifier = Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                if (latestMetric != null) {
                    Text(
                        "${latestMetric.value} $unit",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    trend?.let { diff ->
                        val trendColor = when {
                            type.contains("Gewicht", ignoreCase = true) -> {
                                when (goal) {
                                    "Abnehmen" -> if (diff <= 0) Color(0xFF4CAF50) else Color(
                                        0xFFF44336
                                    )

                                    "Zunehmen" -> if (diff >= 0) Color(0xFF4CAF50) else Color(
                                        0xFFF44336
                                    )

                                    else -> MaterialTheme.colorScheme.secondary
                                }
                            }

                            diff > 0 -> Color(0xFF4CAF50)
                            else -> Color(0xFFF44336)
                        }
                        val prefix = if (diff > 0) "+" else ""
                        Text(
                            text = "$prefix${
                                String.format(
                                    Locale.getDefault(),
                                    "%.1f",
                                    diff
                                )
                            } $unit",
                            color = trendColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}