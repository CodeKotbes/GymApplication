package com.example.gymapplication.gymUI.history

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.gymapplication.data.BodyMetric
import com.example.gymapplication.gymUI.analysis.FullscreenGraphDialog
import com.example.gymapplication.gymUI.analysis.GenericGraph
import com.example.gymapplication.gymUI.analysis.GraphDataPoint
import com.example.gymapplication.gymUI.analysis.HistoryZoomDialog
import com.example.gymapplication.gymUI.analysis.createImageFile
import com.example.gymapplication.gymUI.viewmodel.GymViewModel
import com.example.gymapplication.gymUI.viewmodel.addBodyMetric
import com.example.gymapplication.gymUI.viewmodel.deleteBodyMetric
import com.example.gymapplication.gymUI.viewmodel.getBodyMetrics
import com.example.gymapplication.gymUI.viewmodel.loadWeightGoal
import com.example.gymapplication.gymUI.viewmodel.saveBodyTarget
import com.example.gymapplication.gymUI.viewmodel.setWeightGoal
import com.example.gymapplication.gymUI.viewmodel.updateBodyMetric
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyDetailScreen(type: String, unit: String, viewModel: GymViewModel, onBack: () -> Unit) {
    Locale.setDefault(Locale.GERMANY)
    val metrics by viewModel.getBodyMetrics(type).collectAsState(initial = emptyList())
    val currentGoal by viewModel.weightGoal.collectAsState()
    val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val allTargets by viewModel.bodyTargets.collectAsState(initial = emptyList())
    val targetValue = allTargets.find { it.type == type }?.targetValue
    val sharedPrefs = context.getSharedPreferences("gym_targets", Context.MODE_PRIVATE)
    var selectedStartMillis by remember { mutableStateOf<Long?>(null) }
    var showTargetDialog by rememberSaveable { mutableStateOf(false) }
    var targetInput by rememberSaveable { mutableStateOf("") }
    var isCompareMode by rememberSaveable { mutableStateOf(false) }
    var selectedForCompare by remember { mutableStateOf<List<BodyMetric>>(emptyList()) }
    var showCompareDialog by remember { mutableStateOf(false) }
    LaunchedEffect(type) {
        viewModel.loadWeightGoal(context)
        val savedStart = sharedPrefs.getLong("start_body_$type", -1L)
        if (savedStart != -1L) selectedStartMillis = savedStart
    }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var metricToEdit by remember { mutableStateOf<BodyMetric?>(null) }
    var metricToDelete by remember { mutableStateOf<BodyMetric?>(null) }
    var inputValue by rememberSaveable { mutableStateOf("") }
    var selectedDateMillis by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var fullscreenImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var showFullscreenGraph by rememberSaveable { mutableStateOf(false) }
    var tempCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val tempCameraUri = tempCameraUriString?.let { Uri.parse(it) }

    val photoPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                val permanentUri = copyUriToInternalStorage(context, uri)
                selectedImageUri = permanentUri
            }
        }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) selectedImageUri = tempCameraUri
        }

    if (fullscreenImageUri != null) {
        HistoryZoomDialog(imageUri = fullscreenImageUri!!) { fullscreenImageUri = null }
    }

    if (showCompareDialog && selectedForCompare.size == 2) {
        ImageCompareDialog(
            metric1 = selectedForCompare[0],
            metric2 = selectedForCompare[1],
            unit = unit,
            type = type,
            onClose = {
                showCompareDialog = false
                isCompareMode = false
                selectedForCompare = emptyList()
            },
            onImageClick = { uri ->
                fullscreenImageUri = uri
            }
        )
    }

    val configuration = LocalConfiguration.current
    val germanConfig = remember(configuration) {
        android.content.res.Configuration(configuration).apply {
            setLocale(Locale.GERMANY)
        }
    }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

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
                        datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
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

    Box(modifier = Modifier.fillMaxSize()) {
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
                    if (isCompareMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                isCompareMode = false; selectedForCompare = emptyList()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Abbrechen")
                            }
                            Text(
                                text = "BILDER WÄHLEN (${selectedForCompare.size}/2)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
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
                        Row {
                            if (metrics.count { it.imageUri != null } >= 2) {
                                IconButton(onClick = { isCompareMode = true }) {
                                    Icon(
                                        Icons.Default.Compare,
                                        contentDescription = "Bilder vergleichen",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(onClick = {
                                inputValue = ""; selectedImageUri = null; metricToEdit = null
                                selectedDateMillis = System.currentTimeMillis()
                                datePickerState.selectedDateMillis = selectedDateMillis
                                showAddDialog = true
                            }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Hinzufügen",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (!isCompareMode) {
                    if (type.contains("Gewicht", ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Abnehmen", "Zunehmen", "Halten").forEach { goal ->
                                FilterChip(
                                    selected = currentGoal == goal,
                                    onClick = { viewModel.setWeightGoal(context, goal) },
                                    label = { Text(goal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                        val sortedMetrics = metrics.sortedBy { it.dateMillis }
                        val availableEntries = sortedMetrics.map { Pair(it.dateMillis, it.value) }

                        val startMetric = if (selectedStartMillis != null) {
                            sortedMetrics.find { it.dateMillis == selectedStartMillis }
                                ?: sortedMetrics.firstOrNull()
                        } else sortedMetrics.firstOrNull()

                        val startVal = startMetric?.value
                        val currentVal = sortedMetrics.last().value
                        val prevVal =
                            if (sortedMetrics.size > 1) sortedMetrics[sortedMetrics.size - 2].value else null
                        val isLowerBetter =
                            type.contains("Gewicht", ignoreCase = true) && currentGoal == "Abnehmen"

                        ProgressDashboardCard(
                            title = "DASHBOARD",
                            unit = unit,
                            startValue = startVal,
                            currentValue = currentVal,
                            previousValue = prevVal,
                            targetValue = targetValue,
                            onSetTargetClick = {
                                targetInput = targetValue?.toString() ?: ""; showTargetDialog = true
                            },
                            isLowerBetter = isLowerBetter,
                            availableEntries = availableEntries,
                            onStartSelected = { selectedMillis ->
                                selectedStartMillis = selectedMillis
                                sharedPrefs.edit().putLong("start_body_$type", selectedMillis)
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
                                dataPoints = metrics.map {
                                    GraphDataPoint(
                                        it.value,
                                        it.dateMillis
                                    )
                                },
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
                } else {
                    Text(
                        "Tippe auf zwei Einträge mit Bildern, um sie zu vergleichen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            items(metrics.reversed()) { metric ->
                val hasImage = metric.imageUri != null
                val isSelected = selectedForCompare.contains(metric)
                var showMenu by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).let {
                        if (isCompareMode && hasImage) {
                            it
                                .clickable {
                                    if (isSelected) {
                                        selectedForCompare = selectedForCompare - metric
                                    } else if (selectedForCompare.size < 2) {
                                        selectedForCompare = selectedForCompare + metric
                                    }
                                }
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = MaterialTheme.shapes.medium
                                )
                        } else it
                    },
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
                            if (hasImage) {
                                Box(modifier = Modifier.size(60.dp)) {
                                    AsyncImage(
                                        model = metric.imageUri,
                                        contentDescription = "Progress Bild",
                                        modifier = Modifier.fillMaxSize()
                                            .clip(MaterialTheme.shapes.small).let {
                                                if (!isCompareMode) it.clickable {
                                                    fullscreenImageUri = metric.imageUri
                                                } else it
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Color.Black.copy(alpha = 0.5f),
                                                    MaterialTheme.shapes.small
                                                ), contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Ausgewählt",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            } else if (isCompareMode) {
                                Spacer(modifier = Modifier.width(76.dp))
                            }

                            Column {
                                Text(
                                    dateFormat.format(Date(metric.dateMillis)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${metric.value} $unit",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCompareMode && !hasImage) MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.3f
                                    ) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (!isCompareMode) {
                            Box {
                                IconButton(onClick = {
                                    showMenu = true
                                }) { Icon(Icons.Default.MoreVert, contentDescription = "Optionen") }
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
                                            metricToEdit = metric
                                            inputValue = metric.value.toString()
                                            selectedImageUri =
                                                metric.imageUri?.let { Uri.parse(it) }
                                            selectedDateMillis = metric.dateMillis
                                            datePickerState.selectedDateMillis = selectedDateMillis
                                            showAddDialog = true
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
                                        onClick = { showMenu = false; metricToDelete = metric })
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(120.dp)) }
        }

        AnimatedVisibility(
            visible = isCompareMode,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            Button(
                onClick = { showCompareDialog = true },
                enabled = selectedForCompare.size == 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = if (selectedForCompare.size == 2) "VERGLEICH ANZEIGEN" else "WÄHLE NOCH ${2 - selectedForCompare.size} BILD(ER)",
                    fontWeight = FontWeight.Black
                )
            }
        }
    }

    if (showTargetDialog) {
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = { Text("ZIEL FESTLEGEN", fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    label = { Text("Zielwert in $unit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    keyboardController?.hide()
                    val floatVal = targetInput.replace(",", ".").toFloatOrNull()

                    viewModel.saveBodyTarget(type, floatVal)

                    showTargetDialog = false
                }, shape = MaterialTheme.shapes.medium) { Text("SPEICHERN") }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDialog = false }) { Text("ABBRECHEN") }
            }
        )
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
                        onClick = { showDatePicker = true },
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
                                model = selectedImageUri, contentDescription = null,
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
                            OutlinedButton(onClick = {
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
                            }, shape = MaterialTheme.shapes.medium) { Text("KAMERA") }
                            OutlinedButton(onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
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

    if (metricToDelete != null) {
        AlertDialog(
            onDismissRequest = { metricToDelete = null },
            title = { Text("EINTRAG LÖSCHEN?", fontWeight = FontWeight.Black) },
            text = { Text("Möchtest du diesen Eintrag wirklich löschen?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBodyMetric(metricToDelete!!); metricToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("LÖSCHEN") }
            },
            dismissButton = {
                TextButton(onClick = {
                    metricToDelete = null
                }) { Text("ABBRECHEN") }
            }
        )
    }
}

fun copyUriToInternalStorage(context: Context, uri: Uri): Uri? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "BODY_PROGRESS_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)

        inputStream.copyTo(outputStream)

        inputStream.close()
        outputStream.close()

        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}