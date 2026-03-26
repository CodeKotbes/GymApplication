package com.example.gymapplication.gymUI

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.gymapplication.data.BodyMetric
import com.example.gymapplication.data.Equipment
import com.example.gymapplication.data.WorkoutLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyDetailScreen(type: String, unit: String, viewModel: GymViewModel, onBack: () -> Unit) {
    val metrics by viewModel.getBodyMetrics(type).collectAsState(initial = emptyList())
    val currentGoal by viewModel.weightGoal.collectAsState()

    val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var showAddDialog by remember { mutableStateOf(false) }
    var metricToEdit by remember { mutableStateOf<BodyMetric?>(null) }
    var inputValue by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var fullscreenImageUri by remember { mutableStateOf<String?>(null) }
    var showFullscreenGraph by remember { mutableStateOf(false) }

    var tempCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val tempCameraUri = tempCameraUriString?.let { Uri.parse(it) }

    val photoPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                selectedImageUri = uri
            }
        }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) selectedImageUri = tempCameraUri
        }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                    Text(
                        text = type.uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                }
                IconButton(onClick = {
                    inputValue = ""; selectedImageUri = null; metricToEdit =
                    null; selectedDateMillis = System.currentTimeMillis(); showAddDialog = true
                }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Hinzufügen",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (type.contains("Gewicht", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "DEIN ZIEL:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val goals = listOf("Abnehmen", "Zunehmen", "Halten")
                    goals.forEach { goal ->
                        FilterChip(
                            selected = currentGoal == goal,
                            onClick = { viewModel.setWeightGoal(goal) },
                            label = { Text(goal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (metrics.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Noch keine Daten vorhanden.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
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
                        dataPoints = metrics.map { GraphDataPoint(it.value, it.dateMillis) },
                        unit = unit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        isFullView = false
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

        items(metrics.reversed()) { metric ->
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (metric.imageUri != null) {
                            AsyncImage(
                                model = metric.imageUri,
                                contentDescription = "Progress Bild",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable { fullscreenImageUri = metric.imageUri },
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Column {
                            Text(
                                text = dateFormat.format(Date(metric.dateMillis)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${metric.value} $unit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Optionen"
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Bearbeiten") },
                                onClick = {
                                    showMenu = false; metricToEdit = metric; inputValue =
                                    metric.value.toString(); selectedImageUri =
                                    metric.imageUri?.let { Uri.parse(it) }; selectedDateMillis =
                                    metric.dateMillis; showAddDialog = true
                                })
                            DropdownMenuItem(text = {
                                Text(
                                    "Löschen",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }, onClick = { showMenu = false; viewModel.deleteBodyMetric(metric) })
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(120.dp)) }
    }

    if (showFullscreenGraph) {
        FullscreenGraphDialog(
            dataPoints = metrics.map { GraphDataPoint(it.value, it.dateMillis) },
            unit = unit,
            onClose = { showFullscreenGraph = false })
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false; metricToEdit = null; selectedImageUri = null
            },
            title = {
                Text(
                    if (metricToEdit == null) "WERT EINTRAGEN" else "WERT BEARBEITEN",
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        label = { Text("$type in $unit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            val cal =
                                Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                            android.app.DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    selectedDateMillis =
                                        Calendar.getInstance().apply { set(y, m, d) }.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            "Datum: ${
                                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(
                                    Date(selectedDateMillis)
                                )
                            }", color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (selectedImageUri != null) {
                        Column {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { fullscreenImageUri = selectedImageUri.toString() },
                                contentScale = ContentScale.Crop
                            )
                            TextButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier.align(Alignment.End)
                            ) { Text("Bild entfernen", color = MaterialTheme.colorScheme.error) }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val photoFile = context.createImageFile()
                                        val photoUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            photoFile
                                        )
                                        tempCameraUriString = photoUri.toString()
                                        cameraLauncher.launch(photoUri)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Fehler: ${e.message}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                shape = MaterialTheme.shapes.medium
                            ) { Text("KAMERA") }
                            OutlinedButton(onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }, shape = MaterialTheme.shapes.medium) { Text("GALERIE") }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    keyboardController?.hide()
                    inputValue.replace(",", ".").toFloatOrNull()?.let { value ->
                        if (metricToEdit == null) viewModel.addBodyMetric(
                            type,
                            value,
                            selectedImageUri?.toString(),
                            selectedDateMillis
                        )
                        else viewModel.updateBodyMetric(
                            metricToEdit!!,
                            value,
                            selectedImageUri?.toString(),
                            selectedDateMillis
                        )
                    }
                    showAddDialog = false; metricToEdit = null; selectedImageUri = null
                }, shape = MaterialTheme.shapes.medium) { Text("SPEICHERN") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false; metricToEdit = null; selectedImageUri = null
                }) { Text("ABBRECHEN") }
            }
        )
    }
}

@Composable
fun HistoryDetailScreen(equipment: Equipment, viewModel: GymViewModel, onBack: () -> Unit) {
    val logsFlow = remember(equipment.id) { viewModel.getLogsFlow(equipment.id) }
    val logs by logsFlow.collectAsState(initial = emptyList())
    val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
    val keyboardController = LocalSoftwareKeyboardController.current

    var logToEdit by remember { mutableStateOf<WorkoutLog?>(null) }
    var editLogWeight by remember { mutableStateOf("") }
    var editLogReps by remember { mutableStateOf("") }
    var showFullscreenGraph by remember { mutableStateOf(false) }
    var fullscreenImageUri by remember { mutableStateOf<String?>(null) }

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
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Bearbeiten") },
                                onClick = {
                                    showMenu = false; logToEdit = log; editLogWeight =
                                    log.weight.toString(); editLogReps = log.reps.toString()
                                })
                            DropdownMenuItem(text = {
                                Text(
                                    "Löschen",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }, onClick = { showMenu = false; viewModel.deleteWorkoutLog(log) })
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(120.dp)) }
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
}