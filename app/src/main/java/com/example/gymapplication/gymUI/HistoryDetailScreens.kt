package com.example.gymapplication.gymUI

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.gymapplication.data.BodyMetric
import com.example.gymapplication.data.Equipment
import com.example.gymapplication.data.WorkoutLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Composable
fun ImageCompareDialog(
    metric1: BodyMetric,
    metric2: BodyMetric,
    unit: String,
    type: String,
    onClose: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val sorted = listOf(metric1, metric2).sortedBy { it.dateMillis }
    val m1 = sorted[0]
    val m2 = sorted[1]
    val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())

    val diffInMillis = m2.dateMillis - m1.dateMillis
    val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
    val timePassedText = if (diffInDays > 30) "${diffInDays / 30} Monate" else "$diffInDays Tage"

    val diffValue = m2.value - m1.value
    val prefix = if (diffValue > 0) "+" else ""
    val diffColor = if (type.contains("Gewicht", ignoreCase = true)) {
        if (diffValue > 0) Color(0xFFF44336) else Color(0xFF4CAF50)
    } else {
        if (diffValue > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
    }
    var scale1 by remember { mutableFloatStateOf(1f) }
    var offset1 by remember { mutableStateOf(Offset.Zero) }
    var scale2 by remember { mutableFloatStateOf(1f) }
    var offset2 by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "VORHER / NACHHER",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Abstand: $timePassedText",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Schließen"
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = { scale1 = 1f; offset1 = Offset.Zero },
                                        onTap = { m1.imageUri?.let { onImageClick(it) } }
                                    )
                                }
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale1 = (scale1 * zoom).coerceIn(1f, 5f)
                                        if (scale1 == 1f) {
                                            offset1 = Offset.Zero
                                        } else {
                                            offset1 += pan
                                        }
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = m1.imageUri,
                                contentDescription = "Vorher",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale1,
                                        scaleY = scale1,
                                        translationX = offset1.x,
                                        translationY = offset1.y
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "START",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    dateFormat.format(Date(m1.dateMillis)),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${m1.value} $unit",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = { scale2 = 1f; offset2 = Offset.Zero },
                                        onTap = { m2.imageUri?.let { onImageClick(it) } }
                                    )
                                }
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale2 = (scale2 * zoom).coerceIn(1f, 5f)
                                        if (scale2 == 1f) {
                                            offset2 = Offset.Zero
                                        } else {
                                            offset2 += pan
                                        }
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = m2.imageUri,
                                contentDescription = "Nachher",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale2,
                                        scaleY = scale2,
                                        translationX = offset2.x,
                                        translationY = offset2.y
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "AKTUELL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    dateFormat.format(Date(m2.dateMillis)),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${m2.value} $unit",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
                }

                if (diffValue != 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = diffColor.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "VERÄNDERUNG:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "$prefix${
                                        String.format(
                                            Locale.getDefault(),
                                            "%.1f",
                                            diffValue
                                        )
                                    } $unit",
                                    color = diffColor,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ProgressDashboardCard(
    title: String,
    unit: String,
    startValue: Float?,
    currentValue: Float?,
    previousValue: Float?,
    targetValue: Float?,
    onSetTargetClick: () -> Unit,
    isLowerBetter: Boolean,
    availableEntries: List<Pair<Long, Float>> = emptyList(),
    onStartSelected: (Long) -> Unit = {}
) {
    var startDropdownExpanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                if (availableEntries.isNotEmpty()) startDropdownExpanded = true
                            }
                            .padding(horizontal = 4.dp)
                    ) {
                        Text("START", style = MaterialTheme.typography.labelSmall)
                        Text(
                            if (startValue != null) "$startValue $unit" else "-",
                            fontWeight = FontWeight.Black
                        )
                    }

                    DropdownMenu(
                        expanded = startDropdownExpanded,
                        onDismissRequest = { startDropdownExpanded = false },
                        shape = MaterialTheme.shapes.medium,
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        availableEntries.forEachIndexed { index, entry ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            dateFormat.format(Date(entry.first)),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            "${entry.second} $unit",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = "Datum",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    onStartSelected(entry.first); startDropdownExpanded = false
                                }
                            )
                            if (index < availableEntries.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("AKTUELL", style = MaterialTheme.typography.labelSmall)
                    Text(
                        if (currentValue != null) "$currentValue $unit" else "-",
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSetTargetClick() }) {
                    Text("ZIEL", style = MaterialTheme.typography.labelSmall)
                    Text(
                        if (targetValue != null) "$targetValue $unit" else "Setzen",
                        fontWeight = FontWeight.Black
                    )
                }
            }

            if (targetValue != null && startValue != null && currentValue != null && startValue != targetValue) {
                Spacer(modifier = Modifier.height(16.dp))
                val totalDiff = targetValue - startValue
                val currentDiff = currentValue - startValue
                val rawProgress = currentDiff / totalDiff
                val progress = rawProgress.coerceIn(0f, 1f)

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(MaterialTheme.shapes.medium),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                Text(
                    "${(progress * 100).toInt()}% erreicht",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }

            if (currentValue != null && previousValue != null) {
                val diff = currentValue - previousValue
                if (abs(diff) >= 0.1f) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))

                    val isGood = if (isLowerBetter) diff < 0 else diff > 0
                    val color = if (isGood) Color(0xFF4CAF50) else Color(0xFFF44336)
                    val prefix = if (diff > 0) "+" else ""

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Veränderung zum letzten Mal: ",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "$prefix${String.format(Locale.getDefault(), "%.1f", diff)} $unit",
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyDetailScreen(type: String, unit: String, viewModel: GymViewModel, onBack: () -> Unit) {
    Locale.setDefault(Locale.GERMANY)
    val metrics by viewModel.getBodyMetrics(type).collectAsState(initial = emptyList())
    val currentGoal by viewModel.weightGoal.collectAsState()
    val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val sharedPrefs = context.getSharedPreferences("gym_targets", Context.MODE_PRIVATE)
    var targetValue by remember { mutableStateOf<Float?>(null) }
    var selectedStartMillis by remember { mutableStateOf<Long?>(null) }
    var showTargetDialog by rememberSaveable { mutableStateOf(false) }
    var targetInput by rememberSaveable { mutableStateOf("") }
    var isCompareMode by rememberSaveable { mutableStateOf(false) }
    var selectedForCompare by remember { mutableStateOf<List<BodyMetric>>(emptyList()) }
    var showCompareDialog by remember { mutableStateOf(false) }

    LaunchedEffect(type) {
        val savedTarget = sharedPrefs.getFloat("target_body_$type", -1f)
        if (savedTarget != -1f) targetValue = savedTarget

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
                    }) { Text("ÜBERNEHMEN", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("ABBRECHEN", color = MaterialTheme.colorScheme.primary) }
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
                                            selectedImageUri = metric.imageUri?.let { Uri.parse(it) }
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
                    if (floatVal != null) {
                        sharedPrefs.edit().putFloat("target_body_$type", floatVal).apply()
                        targetValue = floatVal
                    } else if (targetInput.isBlank()) {
                        sharedPrefs.edit().remove("target_body_$type").apply()
                        targetValue = null
                    }
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

@Composable
fun HistoryDetailScreen(equipment: Equipment, viewModel: GymViewModel, onBack: () -> Unit) {
    val logsFlow = remember(equipment.id) { viewModel.getLogsFlow(equipment.id) }
    val logs by logsFlow.collectAsState(initial = emptyList())
    val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("gym_targets", Context.MODE_PRIVATE)
    var targetValue by remember { mutableStateOf<Float?>(null) }
    var selectedStartMillis by remember { mutableStateOf<Long?>(null) }
    var showTargetDialog by rememberSaveable { mutableStateOf(false) }
    var targetInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(equipment.id) {
        val savedTarget = sharedPrefs.getFloat("target_eq_${equipment.id}", -1f)
        if (savedTarget != -1f) targetValue = savedTarget

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
                    if (floatVal != null) {
                        sharedPrefs.edit().putFloat("target_eq_${equipment.id}", floatVal).apply()
                        targetValue = floatVal
                    } else if (targetInput.isBlank()) {
                        sharedPrefs.edit().remove("target_eq_${equipment.id}").apply()
                        targetValue = null
                    }
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