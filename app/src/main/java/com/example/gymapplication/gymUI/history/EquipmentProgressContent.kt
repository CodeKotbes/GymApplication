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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gymapplication.data.Equipment
import com.example.gymapplication.gymUI.GymViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EquipmentProgressContent(viewModel: GymViewModel, onEquipmentClick: (Equipment) -> Unit) {
    val equipmentList by viewModel.equipmentList.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Wähle eine Übung für Details",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        items(equipmentList) { equipment ->
            EquipmentCategoryCard(
                equipment = equipment,
                viewModel = viewModel,
                onClick = { onEquipmentClick(equipment) })
        }
        item { Spacer(modifier = Modifier.height(120.dp)) }
    }
}

@Composable
fun EquipmentCategoryCard(equipment: Equipment, viewModel: GymViewModel, onClick: () -> Unit) {
    val logs by viewModel.getLogsFlow(equipment.id).collectAsState(initial = emptyList())
    val trend by viewModel.getEquipmentTrend(equipment.id).collectAsState(initial = null)
    val latestLog = logs.maxByOrNull { it.dateMillis }
    val targetValue = equipment.targetValue

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (equipment.imageUri != null) {
                AsyncImage(
                    model = equipment.imageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(65.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    equipment.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    equipment.muscleGroup.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (targetValue != null && logs.isNotEmpty()) {
                val dailyMaxWeights = logs.groupBy {
                    SimpleDateFormat(
                        "yyyyMMdd",
                        Locale.getDefault()
                    ).format(Date(it.dateMillis))
                }.values.map { dayLogs -> dayLogs.maxByOrNull { it.weight }!! }
                    .sortedBy { it.dateMillis }
                val startVal = dailyMaxWeights.first().weight
                val currentVal = dailyMaxWeights.last().weight
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
                            "$targetValue kg",
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
                if (latestLog != null) {
                    Text(
                        "${latestLog.weight} kg",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    trend?.let { diff ->
                        val color =
                            if (diff > 0) Color(0xFF4CAF50) else if (diff < 0) Color(0xFFF44336) else Color.Gray
                        val prefix = if (diff > 0) "+" else ""
                        Text(
                            text = "$prefix${String.format(Locale.getDefault(), "%.1f", diff)} kg",
                            color = color,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}