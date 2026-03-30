package com.example.gymapplication.gymUI.history

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gymapplication.data.Equipment
import com.example.gymapplication.data.WorkoutLog
import com.example.gymapplication.gymUI.GymViewModel
import com.example.gymapplication.gymUI.analysis.FullscreenGraphDialog
import com.example.gymapplication.gymUI.analysis.GenericGraph
import com.example.gymapplication.gymUI.analysis.GraphDataPoint
import com.example.gymapplication.gymUI.analysis.HistoryZoomDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryDetailScreen(equipment: Equipment, viewModel: GymViewModel, onBack: () -> Unit) {
    val logsFlow = remember(equipment.id) { viewModel.getLogsFlow(equipment.id) }
    val logs by logsFlow.collectAsState(initial = emptyList())
    val equipmentList by viewModel.equipmentList.collectAsState(initial = emptyList())
    val currentEquipment = equipmentList.find { it.id == equipment.id } ?: equipment
    val targetValue = currentEquipment.targetValue
    val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val sharedPrefs = context.getSharedPreferences("gym_targets", Context.MODE_PRIVATE)
    var selectedStartMillis by remember { mutableStateOf<Long?>(null) }
    var showTargetDialog by rememberSaveable { mutableStateOf(false) }
    var targetInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(equipment.id) {
        val savedStart = sharedPrefs.getLong("start_eq_${equipment.id}", -1L)
        if (savedStart != -1L) selectedStartMillis = savedStart
    }

    var logToEdit by remember { mutableStateOf<WorkoutLog?>(null) }
    var logToDelete by remember { mutableStateOf<WorkoutLog?>(null) }
    var editLogWeight by rememberSaveable { mutableStateOf("") }
    var editLogReps by rememberSaveable { mutableStateOf("") }
    var showFullscreenGraph by rememberSaveable { mutableStateOf(false) }
    var fullscreenImageUri by rememberSaveable { mutableStateOf<String?>(null) }

    if (fullscreenImageUri != null) {
        HistoryZoomDialog(imageUri = fullscreenImageUri!!) { fullscreenImageUri = null }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null
                    )
                }
                Text(
                    text = "VERLAUF: ${equipment.name.uppercase()}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (equipment.imageUri != null) {
                AsyncImage(
                    model = equipment.imageUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.large)
                        .clickable { fullscreenImageUri = equipment.imageUri },
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Keine Einträge.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                val dailyMaxWeights = logs.groupBy {
                    SimpleDateFormat(
                        "yyyyMMdd",
                        Locale.getDefault()
                    ).format(Date(it.dateMillis))
                }
                    .values.map { dayLogs -> dayLogs.maxByOrNull { it.weight }!! }
                    .sortedBy { it.dateMillis }

                val availableEntries = dailyMaxWeights.map { Pair(it.dateMillis, it.weight) }

                val startLog = if (selectedStartMillis != null) {
                    dailyMaxWeights.find { it.dateMillis == selectedStartMillis }
                        ?: dailyMaxWeights.firstOrNull()
                } else {
                    dailyMaxWeights.firstOrNull()
                }

                val startVal = startLog?.weight
                val currentVal = dailyMaxWeights.lastOrNull()?.weight
                val prevVal =
                    if (dailyMaxWeights.size > 1) dailyMaxWeights[dailyMaxWeights.size - 2].weight else null

                ProgressDashboardCard(
                    title = "KRAFT-DASHBOARD",
                    unit = "kg",
                    startValue = startVal,
                    currentValue = currentVal,
                    previousValue = prevVal,
                    targetValue = targetValue,
                    onSetTargetClick = {
                        targetInput = targetValue?.toString() ?: ""; showTargetDialog = true
                    },
                    isLowerBetter = false,
                    availableEntries = availableEntries,
                    onStartSelected = { selectedMillis ->
                        selectedStartMillis = selectedMillis
                        sharedPrefs.edit().putLong("start_eq_${equipment.id}", selectedMillis)
                            .apply()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "ENTWICKLUNG",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clickable { showFullscreenGraph = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large
                ) {
                    GenericGraph(
                        dataPoints = logs.map { GraphDataPoint(it.weight, it.dateMillis) },
                        unit = "kg",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "ALLE EINTRÄGE",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        items(logs.sortedByDescending { it.dateMillis }) { log ->
            var showMenu by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
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
                    Column {
                        Text(
                            text = dateFormat.format(Date(log.dateMillis)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${log.weight} kg x ${log.reps} Reps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Optionen"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            shape = MaterialTheme.shapes.medium,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Bearbeiten",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Bearbeiten",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showMenu = false; logToEdit = log; editLogWeight =
                                    log.weight.toString(); editLogReps = log.reps.toString()
                                })
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Löschen",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Löschen",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = { showMenu = false; logToDelete = log })
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(120.dp)) }
    }

    if (showTargetDialog) {
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = { Text("ZIEL FESTLEGEN", fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    label = { Text("Zielwert in kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    keyboardController?.hide()
                    val floatVal = targetInput.replace(",", ".").toFloatOrNull()
                    viewModel.saveEquipmentTarget(equipment.id, floatVal)

                    showTargetDialog = false
                }, shape = MaterialTheme.shapes.medium) { Text("SPEICHERN") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTargetDialog = false
                }) { Text("ABBRECHEN") }
            }
        )
    }

    if (showFullscreenGraph) FullscreenGraphDialog(dataPoints = logs.map {
        GraphDataPoint(
            it.weight,
            it.dateMillis
        )
    }, unit = "kg", onClose = { showFullscreenGraph = false })

    if (logToEdit != null) {
        AlertDialog(
            onDismissRequest = { logToEdit = null },
            title = { Text("EINTRAG ANPASSEN", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editLogReps,
                        onValueChange = { editLogReps = it },
                        label = { Text("Wdh.") },
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editLogWeight,
                        onValueChange = { editLogWeight = it },
                        label = { Text("kg") },
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    keyboardController?.hide()
                    val w = editLogWeight.replace(",", ".").toFloatOrNull()
                    val r = editLogReps.toIntOrNull()
                    if (w != null && r != null) {
                        viewModel.updateWorkoutLog(
                            logToEdit!!,
                            w,
                            r,
                            logToEdit!!.dateMillis
                        ); logToEdit = null
                    }
                }, shape = MaterialTheme.shapes.medium) { Text("UPDATE") }
            },
            dismissButton = { TextButton(onClick = { logToEdit = null }) { Text("ABBRECHEN") } }
        )
    }

    if (logToDelete != null) {
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("SATZ LÖSCHEN?", fontWeight = FontWeight.Black) },
            text = { Text("Möchtest du diesen Satz wirklich entfernen?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteWorkoutLog(logToDelete!!); logToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("LÖSCHEN") }
            },
            dismissButton = { TextButton(onClick = { logToDelete = null }) { Text("ABBRECHEN") } }
        )
    }
}