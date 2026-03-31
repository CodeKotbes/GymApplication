package com.example.gymapplication.gymUI.equipment

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gymapplication.data.Equipment
import com.example.gymapplication.data.WorkoutLog
import com.example.gymapplication.gymUI.workout.PastSessionNoteCard
import com.example.gymapplication.gymUI.SharedComponents.ZoomableImageDialog
import com.example.gymapplication.gymUI.viewmodel.GymViewModel
import com.example.gymapplication.gymUI.viewmodel.deleteWorkoutLog
import com.example.gymapplication.gymUI.viewmodel.getLogsFlow
import com.example.gymapplication.gymUI.viewmodel.saveWorkoutLog
import com.example.gymapplication.gymUI.viewmodel.updateWorkoutLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentDetailScreen(equipment: Equipment, viewModel: GymViewModel, onBack: () -> Unit) {
    Locale.setDefault(Locale.GERMANY)

    val logsFlow = remember(equipment.id) { viewModel.getLogsFlow(equipment.id) }
    val logs by logsFlow.collectAsState(initial = emptyList())
    val equipmentList by viewModel.equipmentList.collectAsState(initial = emptyList())
    val currentEquipment = equipmentList.find { it.id == equipment.id } ?: equipment
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var weightInput by rememberSaveable { mutableStateOf("") }
    var repsInput by rememberSaveable { mutableStateOf("") }
    var setsInput by rememberSaveable { mutableStateOf("") }
    var fullscreenImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    var logToEdit by remember { mutableStateOf<WorkoutLog?>(null) }
    var logToDelete by remember { mutableStateOf<WorkoutLog?>(null) }
    var editLogWeight by rememberSaveable { mutableStateOf("") }
    var editLogReps by rememberSaveable { mutableStateOf("") }
    var selectedDateStr by rememberSaveable { mutableStateOf<String?>(null) }
    var groupDateToEdit by rememberSaveable { mutableStateOf<String?>(null) }
    val configuration = LocalConfiguration.current
    val germanConfig = remember(configuration) {
        android.content.res.Configuration(configuration).apply {
            setLocale(Locale.GERMANY)
        }
    }
    var customDateMillis by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = customDateMillis)
    var showGroupDatePicker by rememberSaveable { mutableStateOf(false) }
    val groupDatePickerState = rememberDatePickerState()
    val customDatePickerColors = DatePickerDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.primary,
        headlineContentColor = MaterialTheme.colorScheme.primary,
        weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
        selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
        todayContentColor = MaterialTheme.colorScheme.primary,
        todayDateBorderColor = MaterialTheme.colorScheme.primary,
        dayContentColor = MaterialTheme.colorScheme.onSurface
    )

    if (showDatePicker) {
        CompositionLocalProvider(LocalConfiguration provides germanConfig) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                shape = MaterialTheme.shapes.large,
                colors = customDatePickerColors,
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { customDateMillis = it }
                        showDatePicker = false
                    }) {
                        Text(
                            "ÜBERNEHMEN",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(
                            "ABBRECHEN",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = customDatePickerColors
                )
            }
        }
    }

    if (showGroupDatePicker) {
        CompositionLocalProvider(LocalConfiguration provides germanConfig) {
            DatePickerDialog(
                onDismissRequest = {
                    showGroupDatePicker = false
                    groupDateToEdit = null
                },
                shape = MaterialTheme.shapes.large,
                colors = customDatePickerColors,
                confirmButton = {
                    TextButton(onClick = {
                        groupDatePickerState.selectedDateMillis?.let { newDateMillis ->
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
                        }
                        showGroupDatePicker = false
                        groupDateToEdit = null
                    }) {
                        Text(
                            "ÄNDERN",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showGroupDatePicker = false
                        groupDateToEdit = null
                    }) { Text("ABBRECHEN", color = MaterialTheme.colorScheme.primary) }
                }
            ) {
                DatePicker(
                    state = groupDatePickerState,
                    colors = customDatePickerColors
                )
            }
        }
    }

    if (fullscreenImageUri != null) {
        ZoomableImageDialog(imageUri = fullscreenImageUri!!) { fullscreenImageUri = null }
    }

    if (selectedDateStr != null) {
        BackHandler { selectedDateStr = null }
        val logsForSelectedDate =
            logs.filter { dateFormat.format(Date(it.dateMillis)) == selectedDateStr }
                .sortedBy { it.dateMillis }

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
                            "TRAINING AM $selectedDateStr",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                val firstLog = logsForSelectedDate.firstOrNull()
                if (firstLog?.sessionId != null) {
                    PastSessionNoteCard(
                        sessionId = firstLog.sessionId,
                        equipmentId = equipment.id,
                        originalText = firstLog.sessionNote ?: "",
                        originalImagesString = firstLog.sessionNoteImageUris,
                        viewModel = viewModel,
                        onImageClick = { fullscreenImageUri = it }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (logsForSelectedDate.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "SÄTZE",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            logsForSelectedDate.forEachIndexed { index, log ->
                                var showMenu by remember { mutableStateOf(false) }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
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
                                        IconButton(onClick = {
                                            showMenu = true
                                        }) {
                                            Icon(
                                                Icons.Default.MoreVert,
                                                contentDescription = null
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
                                                    showMenu = false
                                                    logToEdit = log
                                                    editLogWeight = log.weight.toString()
                                                    editLogReps = log.reps.toString()
                                                }
                                            )

                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 12.dp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.1f
                                                )
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
                                                onClick = {
                                                    showMenu = false
                                                    logToDelete = log
                                                }
                                            )
                                        }
                                    }
                                }

                                if (index < logsForSelectedDate.size - 1) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.1f
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Keine Sätze mehr vorhanden.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.ime)) }
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
                        text = currentEquipment.name.uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (currentEquipment.imageUri != null) {
                    AsyncImage(
                        model = currentEquipment.imageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(MaterialTheme.shapes.large)
                            .clickable { fullscreenImageUri = currentEquipment.imageUri },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                EquipmentGeneralNoteCard(
                    equipment = currentEquipment,
                    viewModel = viewModel,
                    onImageClick = { fullscreenImageUri = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

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
                    TextButton(onClick = { showDatePicker = true }) {
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = repsInput,
                        onValueChange = { repsInput = it },
                        label = { Text("Wdh.") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("kg") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
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
                            viewModel.saveWorkoutLog(currentEquipment.id, w, r, s, customDateMillis)
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
                                    onDismissRequest = { showUnitMenu = false },
                                    shape = MaterialTheme.shapes.medium,
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Datum ändern",
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.DateRange,
                                                contentDescription = "Datum ändern",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        onClick = {
                                            showUnitMenu = false
                                            groupDateToEdit = dateStr
                                            groupDatePickerState.selectedDateMillis = try {
                                                dateFormat.parse(dateStr)?.time
                                            } catch (e: Exception) {
                                                System.currentTimeMillis()
                                            }
                                            showGroupDatePicker = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.ime)) }
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = editLogWeight,
                        onValueChange = { editLogWeight = it },
                        label = { Text("kg") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
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

    if (logToDelete != null) {
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("SATZ LÖSCHEN?", fontWeight = FontWeight.Black) },
            text = { Text("Möchtest du diesen Satz wirklich entfernen?") },
            confirmButton = {
                Button(
                    onClick = {
                        val isLastLogForDate =
                            logs.count { dateFormat.format(Date(it.dateMillis)) == selectedDateStr } <= 1
                        viewModel.deleteWorkoutLog(logToDelete!!)
                        logToDelete = null
                        if (isLastLogForDate && selectedDateStr != null) {
                            selectedDateStr = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("LÖSCHEN") }
            },
            dismissButton = { TextButton(onClick = { logToDelete = null }) { Text("ABBRECHEN") } }
        )
    }
}