package com.example.gymapplication.gymUI.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.gymapplication.gymUI.analysis.AnalysisEfficiencyGraph
import com.example.gymapplication.gymUI.analysis.AnalysisFullscreenEfficiencyDialog
import com.example.gymapplication.gymUI.analysis.AnalysisFullscreenWorkloadDialog
import com.example.gymapplication.gymUI.analysis.AnalysisWorkloadGraph
import com.example.gymapplication.gymUI.analysis.GraphDataPoint
import com.example.gymapplication.gymUI.analysis.PremiumDonutChart
import com.example.gymapplication.gymUI.viewmodel.GymViewModel
import com.example.gymapplication.gymUI.viewmodel.getBodyMetrics
import com.example.gymapplication.gymUI.viewmodel.getLogsFlow
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2

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
                Color(0xFF6366F1), Color(0xFF10B981), Color(0xFFF59E0B),
                Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF06B6D4),
                Color(0xFFF43F5E), Color(0xFF14B8A6), Color(0xFFF97316)
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
                                strokeWidth = 45f,
                                highlightedIndices = muscleGroupTotals.indices.filter {
                                    expandedMuscleGroups.contains(
                                        muscleGroupTotals[it].first
                                    )
                                },
                                outlineColor = MaterialTheme.colorScheme.onSurface
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
                                    var selectedExercise by remember { mutableStateOf<String?>(null) }

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
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                MaterialTheme.shapes.medium
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
                                        val highlightedExIndices =
                                            exercises.indices.filter { exercises[it].equipmentName == selectedExercise }

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
                                                strokeWidth = 30f,
                                                highlightedIndices = highlightedExIndices,
                                                outlineColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        exercises.forEachIndexed { exIndex, ex ->
                                            val exPercentage =
                                                (ex.totalSets.toFloat() / total) * 100
                                            val exColor =
                                                pieColors[(index + exIndex + 2) % pieColors.size]
                                            val isExSelected = selectedExercise == ex.equipmentName

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(MaterialTheme.shapes.small)
                                                    .clickable {
                                                        selectedExercise =
                                                            if (isExSelected) null else ex.equipmentName
                                                    }
                                                    .padding(vertical = 8.dp, horizontal = 4.dp),
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
                                                        fontWeight = if (isExSelected) FontWeight.Black else FontWeight.Bold,
                                                        color = if (isExSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Text(
                                                    "${exPercentage.toInt()} %",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isExSelected) FontWeight.Black else FontWeight.Bold,
                                                    color = if (isExSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
                                            contentDescription = "Übung",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = { onEfficiencyEqSelected(eq.id); expanded = false }
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

                        val dailyBodyWeight = bodyWeightMetrics.map {
                            GraphDataPoint(
                                it.value,
                                it.dateMillis
                            )
                        }.sortedBy { it.dateMillis }

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