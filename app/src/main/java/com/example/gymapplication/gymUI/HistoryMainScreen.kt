package com.example.gymapplication.gymUI

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gymapplication.data.Equipment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(viewModel: GymViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("KÖRPER", "GEWICHTE", "REKORDE")
    var selectedEquipment by remember { mutableStateOf<Equipment?>(null) }
    var selectedBodyMetric by remember { mutableStateOf<Pair<String, String>?>(null) }

    if (selectedEquipment != null) {
        BackHandler { selectedEquipment = null }
        HistoryDetailScreen(
            equipment = selectedEquipment!!,
            viewModel = viewModel,
            onBack = { selectedEquipment = null })
        return
    }
    if (selectedBodyMetric != null) {
        BackHandler { selectedBodyMetric = null }
        BodyDetailScreen(
            type = selectedBodyMetric!!.first,
            unit = selectedBodyMetric!!.second,
            viewModel = viewModel,
            onBack = { selectedBodyMetric = null })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "MEIN FORTSCHRITT",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(16.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> BodyProgressContent(viewModel) { type, unit ->
                selectedBodyMetric = Pair(type, unit)
            }

            1 -> EquipmentProgressContent(viewModel) { selectedEquipment = it }
            2 -> PersonalRecordsContent(viewModel)
        }
    }
}

@Composable
fun PersonalRecordsContent(viewModel: GymViewModel) {
    val prList by viewModel.personalRecords.collectAsState()
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    if (prList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Noch keine Rekorde aufgestellt.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                "DEINE PERSONAL RECORDS",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        itemsIndexed(prList) { _, pr ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            pr.equipmentName.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "Am: ${dateFormat.format(Date(pr.dateOfMaxWeight))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Theoretisches 1RM: ~${(pr.theoretical1RM * 10.0).roundToInt() / 10.0} kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${pr.maxWeight} kg",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "für ${pr.repsAtMaxWeight} Wdh.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(120.dp)) }
    }
}

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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        items(metricTypes) { (type, unit) ->
            BodyCategoryCard(
                type = type,
                unit = unit,
                viewModel = viewModel,
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                type.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            Column(horizontalAlignment = Alignment.End) {
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
                            text = "$prefix${String.format("%.1f", diff)} $unit",
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

@Composable
fun EquipmentProgressContent(viewModel: GymViewModel, onEquipmentClick: (Equipment) -> Unit) {
    val equipmentList by viewModel.equipmentList.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Wähle ein Gerät für Details",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    equipment.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    equipment.muscleGroup.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
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
                        Text(
                            "${if (diff > 0) "+" else ""}$diff kg",
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