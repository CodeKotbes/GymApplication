package com.example.gymapplication.gymUI

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EquipmentDetailScreen(equipment: Equipment, viewModel: GymViewModel, onBack: () -> Unit) {
    val logsFlow = remember(equipment.id) { viewModel.getLogsFlow(equipment.id) }
    val logs by logsFlow.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var weightInput by remember { mutableStateOf("") }
    var repsInput by remember { mutableStateOf("") }
    var setsInput by remember { mutableStateOf("") }
    var fullscreenImageUri by remember { mutableStateOf<String?>(null) }
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    var logToEdit by remember { mutableStateOf<WorkoutLog?>(null) }
    var editLogWeight by remember { mutableStateOf("") }
    var editLogReps by remember { mutableStateOf("") }
    var selectedDateStr by remember { mutableStateOf<String?>(null) }
    var groupDateToEdit by remember { mutableStateOf<String?>(null) }
    val groupCalendar = remember { java.util.Calendar.getInstance() }
    val groupDatePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val newCal = java.util.Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            val newDateMillis = newCal.timeInMillis
            val logsToUpdate =
                logs.filter { dateFormat.format(Date(it.dateMillis)) == groupDateToEdit }
            val oldMidnightCal = java.util.Calendar.getInstance().apply {
                try {
                    dateFormat.parse(groupDateToEdit!!)?.let { time = it }
                } catch (e: Exception) {
                }
            }
            val timeOffset = newDateMillis - oldMidnightCal.timeInMillis
            logsToUpdate.forEach { log ->
                val updatedTime = log.dateMillis + timeOffset
                viewModel.updateWorkoutLog(log, log.weight, log.reps, updatedTime)
            }
            if (selectedDateStr == groupDateToEdit) {
                selectedDateStr = dateFormat.format(Date(newDateMillis))
            }
            groupDateToEdit = null
        },
        groupCalendar.get(java.util.Calendar.YEAR),
        groupCalendar.get(java.util.Calendar.MONTH),
        groupCalendar.get(java.util.Calendar.DAY_OF_MONTH)
    )
    groupDatePickerDialog.setOnDismissListener { groupDateToEdit = null }

    var customDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val calendar = remember { java.util.Calendar.getInstance() }
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = java.util.Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            customDateMillis = cal.timeInMillis
        },
        calendar.get(java.util.Calendar.YEAR),
        calendar.get(java.util.Calendar.MONTH),
        calendar.get(java.util.Calendar.DAY_OF_MONTH)
    )

    if (fullscreenImageUri != null) {
        ZoomableImageDialog(imageUri = fullscreenImageUri!!) { fullscreenImageUri = null }
    }

    if (selectedDateStr != null) {
        BackHandler { selectedDateStr = null }
        val logsForSelectedDate =
            logs.filter { dateFormat.format(Date(it.dateMillis)) == selectedDateStr }
        LaunchedEffect(logsForSelectedDate.size) {
            if (logsForSelectedDate.isEmpty() && selectedDateStr != null) {
                selectedDateStr = null
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            selectedDateStr = null
                        }) { Icon(Icons.Default.ArrowBack, contentDescription = "Zurück") }
                        Text(
                            text = "TRAINING AM $selectedDateStr",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            itemsIndexed(logsForSelectedDate.sortedBy { it.dateMillis }) { index, log ->
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
                                "Satz ${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${log.weight} kg × ${log.reps} Wdh.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = null
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Bearbeiten") },
                                    onClick = {
                                        showMenu = false; logToEdit = log; editLogWeight =
                                        log.weight.toString(); editLogReps = log.reps.toString()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Löschen",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = { showMenu = false; viewModel.deleteWorkoutLog(log) }
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(120.dp)) }
        }
    } else {
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
                        text = equipment.name.uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (equipment.imageUri != null) {
                    AsyncImage(
                        model = equipment.imageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(MaterialTheme.shapes.large)
                            .clickable { fullscreenImageUri = equipment.imageUri },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "TRAINING AUFZEICHNEN",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { datePickerDialog.show() }) {
                        val dateString =
                            if (dateFormat.format(Date(customDateMillis)) == dateFormat.format(Date())) "Heute" else dateFormat.format(
                                Date(customDateMillis)
                            )
                        Text(dateString, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = setsInput,
                        onValueChange = { setsInput = it },
                        label = { Text("Sätze") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = repsInput,
                        onValueChange = { repsInput = it },
                        label = { Text("Wdh.") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("kg") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        keyboardController?.hide()
                        val w = weightInput.replace(",", ".").toFloatOrNull()
                        val r = repsInput.toIntOrNull()
                        val s = setsInput.toIntOrNull() ?: 1
                        if (w != null && r != null) {
                            viewModel.saveWorkoutLog(equipment.id, w, r, s, customDateMillis)
                            weightInput = ""; repsInput = ""; setsInput = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = weightInput.isNotBlank() && repsInput.isNotBlank()
                ) { Text("TRAINING SPEICHERN", fontWeight = FontWeight.Bold) }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "TRAININGSEINHEITEN",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val groupedLogs = logs.groupBy { dateFormat.format(Date(it.dateMillis)) }

            groupedLogs.forEach { (dateStr, logsForDay) ->
                item {
                    var showUnitMenu by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp)
                            .clickable { selectedDateStr = dateStr },
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${logsForDay.size} Sätze",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box {
                                IconButton(onClick = {
                                    showUnitMenu = true
                                }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                                DropdownMenu(
                                    expanded = showUnitMenu,
                                    onDismissRequest = { showUnitMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Datum ändern") },
                                        onClick = {
                                            showUnitMenu = false
                                            groupDateToEdit = dateStr
                                            try {
                                                dateFormat.parse(dateStr)
                                                    ?.let { groupCalendar.time = it }
                                            } catch (e: Exception) {
                                            }
                                            groupDatePickerDialog.updateDate(
                                                groupCalendar.get(java.util.Calendar.YEAR),
                                                groupCalendar.get(java.util.Calendar.MONTH),
                                                groupCalendar.get(java.util.Calendar.DAY_OF_MONTH)
                                            )
                                            groupDatePickerDialog.show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(120.dp)) }
        }
    }

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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = editLogWeight,
                        onValueChange = { editLogWeight = it },
                        label = { Text("kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.medium
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    keyboardController?.hide()
                    val w = editLogWeight.replace(",", ".").toFloatOrNull()
                    val r = editLogReps.toIntOrNull()
                    if (w != null && r != null) {
                        viewModel.updateWorkoutLog(logToEdit!!, w, r, logToEdit!!.dateMillis)
                        logToEdit = null
                    }
                }, shape = MaterialTheme.shapes.medium) { Text("UPDATE") }
            },
            dismissButton = { TextButton(onClick = { logToEdit = null }) { Text("ABBRECHEN") } }
        )
    }
}

