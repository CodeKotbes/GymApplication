package com.example.gymapplication.gymUI

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gymapplication.data.Equipment
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(viewModel: GymViewModel) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("KÖRPER", "GEWICHTE", "REKORDE", "ANALYSE")
    var selectedEquipmentId by rememberSaveable { mutableStateOf<Int?>(null) }
    val equipmentList by viewModel.equipmentList.collectAsState(initial = emptyList())
    val selectedEquipment = equipmentList.find { it.id == selectedEquipmentId }
    var selectedBodyType by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedBodyUnit by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedEfficiencyEqId by rememberSaveable { mutableStateOf<Int?>(null) }
    var expandedMuscleGroups by rememberSaveable { mutableStateOf(emptyList<String>()) }

    if (selectedEquipment != null) {
        BackHandler { selectedEquipmentId = null }
        HistoryDetailScreen(
            equipment = selectedEquipment,
            viewModel = viewModel,
            onBack = { selectedEquipmentId = null })
        return
    }

    if (selectedBodyType != null && selectedBodyUnit != null) {
        BackHandler { selectedBodyType = null; selectedBodyUnit = null }
        BodyDetailScreen(
            type = selectedBodyType!!,
            unit = selectedBodyUnit!!,
            viewModel = viewModel,
            onBack = { selectedBodyType = null; selectedBodyUnit = null })
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
                    Modifier.tabIndicatorOffset(
                        tabPositions[selectedTab]
                    ), color = MaterialTheme.colorScheme.primary
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
                selectedBodyType = type; selectedBodyUnit = unit
            }

            1 -> EquipmentProgressContent(viewModel) { selectedEquipmentId = it.id }
            2 -> PersonalRecordsContent(viewModel)
            3 -> AnalysisContent(
                viewModel = viewModel,
                selectedEfficiencyEqId = selectedEfficiencyEqId,
                onEfficiencyEqSelected = { selectedEfficiencyEqId = it },
                expandedMuscleGroups = expandedMuscleGroups,
                onToggleMuscleGroup = { muscle ->
                    expandedMuscleGroups = if (expandedMuscleGroups.contains(muscle)) {
                        expandedMuscleGroups - muscle
                    } else {
                        expandedMuscleGroups + muscle
                    }
                }
            )
        }
    }
}

@Composable
fun AnalysisContent(
    viewModel: GymViewModel,
    selectedEfficiencyEqId: Int?,
    onEfficiencyEqSelected: (Int?) -> Unit,
    expandedMuscleGroups: List<String>,
    onToggleMuscleGroup: (String) -> Unit
) {
    val volumeStats by viewModel.dailyVolumeStats.collectAsState()
    val detailedStats by viewModel.detailedMuscleStats.collectAsState()

    val equipmentList by viewModel.equipmentList.collectAsState()
    val efficiencyLogs by (selectedEfficiencyEqId?.let { viewModel.getLogsFlow(it) } ?: flowOf(
        emptyList()
    )).collectAsState(initial = emptyList())
    val bodyWeightMetrics by viewModel.getBodyMetrics("Gewicht")
        .collectAsState(initial = emptyList())
    var showWorkloadFullscreen by remember { mutableStateOf(false) }
    var showEfficiencyFullscreen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                "WORKLOAD (GESAMTVOLUMEN)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large
            ) {
                val graphData = volumeStats.mapNotNull { stat ->
                    try {
                        val date =
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(stat.dateStr)
                        if (date != null) GraphDataPoint(stat.totalVolume, date.time) else null
                    } catch (e: Exception) {
                        null
                    }
                }.sortedBy { it.dateMillis }

                if (graphData.size >= 2) {
                    AnalysisWorkloadGraph(
                        dataPoints = graphData,
                        unit = "kg",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        isFullView = false,
                        onGraphClick = { showWorkloadFullscreen = true }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Trainiere noch etwas, um dein Volumen zu sehen!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (showWorkloadFullscreen) {
                val graphData = volumeStats.mapNotNull { stat ->
                    try {
                        val date =
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(stat.dateStr)
                        if (date != null) GraphDataPoint(stat.totalVolume, date.time) else null
                    } catch (e: Exception) {
                        null
                    }
                }.sortedBy { it.dateMillis }
                AnalysisFullscreenWorkloadDialog(
                    dataPoints = graphData,
                    onClose = { showWorkloadFullscreen = false })
            }
        }

        item {
            Text(
                "MUSKEL-BALANCE",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            val muscleGroupTotals = detailedStats.groupBy { it.muscleGroup }
                .mapValues { entry -> entry.value.sumOf { it.totalSets } }.toList()
                .sortedByDescending { it.second }
            val totalSetsOverall = muscleGroupTotals.sumOf { it.second }

            val pieColors = listOf(
                Color(0xFFE53935),
                Color(0xFF1E88E5),
                Color(0xFF43A047),
                Color(0xFFFFB300),
                Color(0xFF8E24AA),
                Color(0xFF00ACC1),
                Color(0xFFF4511E),
                Color(0xFF3949AB),
                Color(0xFF00897B)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (totalSetsOverall > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PremiumDonutChart(
                                data = muscleGroupTotals.map { it.second.toFloat() },
                                colors = pieColors,
                                modifier = Modifier.size(160.dp),
                                strokeWidth = 45f
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        muscleGroupTotals.forEachIndexed { index, (muscle, total) ->
                            val isExpanded = expandedMuscleGroups.contains(muscle)
                            val percentage = (total.toFloat() / totalSetsOverall) * 100
                            val groupColor = pieColors[index % pieColors.size]

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable { onToggleMuscleGroup(muscle) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(groupColor)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        muscle.uppercase(),
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "${percentage.toInt()} %",
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start = 12.dp,
                                                end = 12.dp,
                                                top = 16.dp,
                                                bottom = 24.dp
                                            )
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = 0.3f
                                                ), MaterialTheme.shapes.medium
                                            )
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            "ÜBUNGS-AUFSCHLÜSSELUNG",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))

                                        val exercises =
                                            detailedStats.filter { it.muscleGroup == muscle }
                                                .sortedByDescending { it.totalSets }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            PremiumDonutChart(
                                                data = exercises.map { it.totalSets.toFloat() },
                                                colors = exercises.indices.map { exIndex -> pieColors[(index + exIndex + 2) % pieColors.size] },
                                                modifier = Modifier.size(100.dp),
                                                strokeWidth = 30f
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        exercises.forEachIndexed { exIndex, ex ->
                                            val exPercentage =
                                                (ex.totalSets.toFloat() / total) * 100
                                            val exColor =
                                                pieColors[(index + exIndex + 2) % pieColors.size]

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(exColor)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        ex.equipmentName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Text(
                                                    "${exPercentage.toInt()} %",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (index < muscleGroupTotals.size - 1) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.05f
                                    )
                                )
                            }
                        }
                    } else {
                        Text(
                            "Keine Daten für die letzten 30 Tage.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text(
                "EFFICIENCY FACTOR",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Vergleiche den Verlauf deines Körpergewichts mit deiner Kraftentwicklung.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    var expanded by remember { mutableStateOf(false) }
                    val selectedEq = equipmentList.find { it.id == selectedEfficiencyEqId }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                selectedEq?.name?.uppercase() ?: "ÜBUNG WÄHLEN",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .heightIn(max = 300.dp),
                            shape = MaterialTheme.shapes.medium,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            equipmentList.forEachIndexed { index, eq ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            eq.name.uppercase(),
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.FitnessCenter,
                                            contentDescription = "Gerät",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        onEfficiencyEqSelected(eq.id)
                                        expanded = false
                                    }
                                )
                                if (index < equipmentList.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (selectedEfficiencyEqId != null) {
                        val dailyMaxStrength = efficiencyLogs.groupBy {
                            SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.getDefault()
                            ).format(Date(it.dateMillis))
                        }.mapNotNull { (dateStr, logs) ->
                            val date =
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                            if (date != null) GraphDataPoint(
                                logs.maxOf { it.weight },
                                date.time
                            ) else null
                        }.sortedBy { it.dateMillis }

                        val dailyBodyWeight =
                            bodyWeightMetrics.map { GraphDataPoint(it.value, it.dateMillis) }
                                .sortedBy { it.dateMillis }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
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

                        Spacer(modifier = Modifier.height(16.dp))

                        if (dailyMaxStrength.size >= 2 && dailyBodyWeight.size >= 2) {
                            AnalysisEfficiencyGraph(
                                bodyWeights = dailyBodyWeight,
                                strengths = dailyMaxStrength,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                isFullView = false,
                                onGraphClick = { showEfficiencyFullscreen = true }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Sammle mehr Daten für beide Werte...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (showEfficiencyFullscreen) {
                            AnalysisFullscreenEfficiencyDialog(
                                bodyWeights = dailyBodyWeight,
                                strengths = dailyMaxStrength,
                                onClose = { showEfficiencyFullscreen = false })
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(120.dp)) }
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
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            pr.equipmentName.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("gym_targets", Context.MODE_PRIVATE)
    var targetValue by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(type, metrics.size) {
        val saved = sharedPrefs.getFloat("target_body_$type", -1f)
        targetValue = if (saved != -1f) saved else null
    }

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
                    val rawProgress = (currentVal - startVal) / (targetValue!! - startVal)
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
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

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
                                    ); "Zunehmen" -> if (diff >= 0) Color(0xFF4CAF50) else Color(
                                    0xFFF44336
                                ); else -> MaterialTheme.colorScheme.secondary
                                }
                            }

                            diff > 0 -> Color(0xFF4CAF50); else -> Color(0xFFF44336)
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

    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("gym_targets", Context.MODE_PRIVATE)
    var targetValue by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(equipment.id, logs.size) {
        val saved = sharedPrefs.getFloat("target_eq_${equipment.id}", -1f)
        targetValue = if (saved != -1f) saved else null
    }

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
                }
                    .values.map { dayLogs -> dayLogs.maxByOrNull { it.weight }!! }
                    .sortedBy { it.dateMillis }

                val startVal = dailyMaxWeights.first().weight
                val currentVal = dailyMaxWeights.last().weight

                if (startVal != targetValue) {
                    val rawProgress = (currentVal - startVal) / (targetValue!! - startVal)
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
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

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
                        Text(
                            "${if (diff > 0) "+" else ""}${
                                String.format(
                                    Locale.getDefault(),
                                    "%.1f",
                                    diff
                                )
                            } kg",
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