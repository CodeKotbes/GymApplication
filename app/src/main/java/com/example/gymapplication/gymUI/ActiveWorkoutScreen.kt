package com.example.gymapplication.gymUI

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: GymViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val activeSession by viewModel.activeSession.collectAsState()
    val workoutDuration by viewModel.workoutDuration.collectAsState()

    var showSummary by rememberSaveable { mutableStateOf(false) }
    var finalPlanName by rememberSaveable { mutableStateOf("") }
    var finalDuration by rememberSaveable { mutableLongStateOf(0L) }
    val triggerSummary by viewModel.triggerSummaryEvent.collectAsState()

    LaunchedEffect(triggerSummary) {
        if (triggerSummary && activeSession != null) {
            finalPlanName = activeSession?.name ?: "Freies Workout"
            finalDuration = workoutDuration
            if (!activeSession!!.isPaused) viewModel.toggleWorkoutPause(context)
            showSummary = true
            viewModel.consumeSummaryEvent()
        }
    }

    if (activeSession == null && !showSummary) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }

    val equipmentInPlan by if (activeSession?.planId != null) {
        viewModel.getEquipmentWithLogsForPlanFlow(activeSession!!.planId!!)
            .collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    val sessionLogs by remember(activeSession?.sessionId) {
        if (activeSession != null) viewModel.getLogsForSessionFlow(activeSession!!.sessionId)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    val allEquipment by viewModel.equipmentList.collectAsState()
    val currentIndex by viewModel.currentExerciseIndex.collectAsState()
    val isResting by viewModel.isResting.collectAsState()
    val restSecondsLeft by viewModel.restSecondsLeft.collectAsState()
    val defaultRestTime by viewModel.currentRestTime.collectAsState()
    var fullscreenImageUri by rememberSaveable { mutableStateOf<String?>(null) }

    if (fullscreenImageUri != null) {
        HistoryZoomDialog(imageUri = fullscreenImageUri!!) { fullscreenImageUri = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (showSummary) "WORKOUT BEENDET" else activeSession?.name?.uppercase()
                                ?: finalPlanName.uppercase(),
                            fontWeight = FontWeight.Black
                        )
                        val durationToUse = if (showSummary) finalDuration else workoutDuration
                        val h = durationToUse / 3600
                        val m = (durationToUse % 3600) / 60
                        val s = durationToUse % 60
                        val timeStr =
                            if (h > 0) String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
                            else String.format(Locale.getDefault(), "%02d:%02d", m, s)
                        Text(
                            text = if (showSummary) "Deine Leistung" else timeStr,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    if (!showSummary) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.Close, contentDescription = "Minimieren")
                        }
                    }
                },
                actions = {
                    if (!showSummary && activeSession != null) {
                        IconButton(onClick = { viewModel.toggleWorkoutPause(context) }) {
                            Icon(
                                imageVector = if (activeSession!!.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Button(
                            onClick = {
                                finalPlanName = activeSession?.name ?: "Freies Workout"
                                finalDuration = workoutDuration
                                if (!activeSession!!.isPaused) viewModel.toggleWorkoutPause(context)
                                showSummary = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("BEENDEN", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->

        if (showSummary) {
            val calculatedVolume = sessionLogs.sumOf { (it.weight * it.reps).toDouble() }.toFloat()

            val topSetsList = sessionLogs
                .groupBy { it.equipmentId }
                .mapNotNull { (equipmentId, logs) ->
                    val eqName = allEquipment.find { it.id == equipmentId }?.name ?: "Übung"
                    val bestLog = logs.maxWithOrNull(compareBy({ it.weight }, { it.reps }))
                    bestLog?.let { eqName to "${it.weight} kg x ${it.reps}" }
                }
                .take(5)

            WorkoutSummaryView(
                modifier = Modifier.padding(innerPadding),
                planName = finalPlanName,
                duration = finalDuration,
                totalVolume = calculatedVolume,
                topSets = topSetsList,
                onShare = { bitmap -> ShareCardManager.shareBitmap(context, bitmap) },
                onFinish = {
                    if (activeSession != null) {
                        viewModel.finishWorkout(context)
                    }
                    onNavigateBack()
                }
            )
        } else {
            if (equipmentInPlan.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Lade Übungen...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@Scaffold
            }

            val currentEquipment = equipmentInPlan[currentIndex]
            val ghostValue by viewModel.getLatestLogForEquipment(currentEquipment.id)
                .collectAsState(initial = null)

            AnimatedContent(targetState = isResting, label = "WorkoutView") { resting ->
                if (resting) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "PAUSE",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val m = restSecondsLeft / 60
                        val s = restSecondsLeft % 60
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", m, s),
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.adjustRestTime(-30) },
                                modifier = Modifier.height(55.dp)
                            ) { Text("-30s") }
                            OutlinedButton(
                                onClick = { viewModel.adjustRestTime(30) },
                                modifier = Modifier.height(55.dp)
                            ) { Text("+30s") }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { viewModel.skipRest() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(65.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text(
                                "PAUSE ÜBERSPRINGEN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentEquipment.imageUri != null) {
                                AsyncImage(
                                    model = currentEquipment.imageUri, contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable {
                                            fullscreenImageUri = currentEquipment.imageUri
                                        },
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            Column {
                                Text(
                                    "ÜBUNG ${currentIndex + 1} VON ${equipmentInPlan.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    currentEquipment.name.uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        key(currentIndex) {
                            var weightInput by rememberSaveable(currentIndex) {
                                mutableStateOf(
                                    currentEquipment.latestWeight?.toString() ?: ""
                                )
                            }
                            var repsInput by rememberSaveable(currentIndex) {
                                mutableStateOf(
                                    currentEquipment.latestReps?.toString() ?: ""
                                )
                            }
                            var isWarmup by rememberSaveable(currentIndex) { mutableStateOf(false) }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                    ) {
                                        Checkbox(
                                            checked = isWarmup,
                                            onCheckedChange = { isWarmup = it })
                                        Text(
                                            "Aufwärmsatz",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    ghostValue?.let { ghost ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "ZULETZT: ${ghost.weight} kg",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.4f
                                                ),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "${ghost.reps} REPS",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.4f
                                                ),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = weightInput,
                                            onValueChange = { weightInput = it },
                                            label = { Text("Gewicht (kg)") },
                                            modifier = Modifier.weight(1f),
                                            placeholder = {
                                                Text(
                                                    ghostValue?.weight?.toString() ?: "0.0"
                                                )
                                            },
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Decimal,
                                                imeAction = androidx.compose.ui.text.input.ImeAction.Next
                                            ),
                                            singleLine = true,
                                            enabled = !isWarmup
                                        )
                                        OutlinedTextField(
                                            value = repsInput,
                                            onValueChange = { repsInput = it },
                                            label = { Text("Wiederholungen") },
                                            modifier = Modifier.weight(1f),
                                            placeholder = {
                                                Text(
                                                    ghostValue?.reps?.toString() ?: "0"
                                                )
                                            },
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                            ),
                                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                                onDone = { keyboardController?.hide() }),
                                            singleLine = true,
                                            enabled = !isWarmup
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Button(
                                        onClick = {
                                            keyboardController?.hide()
                                            if (isWarmup) {
                                                viewModel.startRestTimer(context, defaultRestTime)
                                            } else {
                                                val w =
                                                    weightInput.replace(",", ".").toFloatOrNull()
                                                val r = repsInput.toIntOrNull()
                                                if (w != null && r != null) {
                                                    viewModel.saveWorkoutLog(
                                                        currentEquipment.id,
                                                        w,
                                                        r,
                                                        1
                                                    )
                                                    viewModel.startRestTimer(
                                                        context,
                                                        defaultRestTime
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(65.dp),
                                        shape = MaterialTheme.shapes.medium,
                                        enabled = isWarmup || (weightInput.isNotBlank() && repsInput.isNotBlank())
                                    ) {
                                        Text(
                                            if (isWarmup) "WARMUP BEENDEN" else "SATZ BEENDEN",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            WorkoutNoteSection(
                                equipmentId = currentEquipment.id,
                                viewModel = viewModel,
                                activeSessionId = activeSession!!.sessionId,
                                onImageClick = { fullscreenImageUri = it })
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (currentIndex > 0) viewModel.updateExerciseIndex(
                                        currentIndex - 1
                                    )
                                },
                                enabled = currentIndex > 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(55.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ZURÜCK")
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            if (currentIndex < equipmentInPlan.size - 1) {
                                OutlinedButton(
                                    onClick = { viewModel.updateExerciseIndex(currentIndex + 1) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(55.dp)
                                ) {
                                    Text("NÄCHSTE")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        finalPlanName = activeSession?.name ?: "Freies Workout"
                                        finalDuration = workoutDuration
                                        if (!activeSession!!.isPaused) viewModel.toggleWorkoutPause(
                                            context
                                        )
                                        showSummary = true
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(55.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(
                                        "BEENDEN",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(350.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutSummaryView(
    modifier: Modifier = Modifier,
    planName: String,
    duration: Long,
    totalVolume: Float,
    topSets: List<Pair<String, String>>,
    onShare: (android.graphics.Bitmap) -> Unit,
    onFinish: () -> Unit
) {
    val h = duration / 3600
    val m = (duration % 3600) / 60
    val s = duration % 60
    val durationStr =
        if (h > 0) String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s) else String.format(
            Locale.getDefault(),
            "%02d:%02d",
            m,
            s
        )

    var composeView by remember { mutableStateOf<ComposeView?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "DEINE WORKOUT-KARTE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        AndroidView(
            factory = { ctx ->
                ComposeView(ctx).apply {
                    composeView = this
                }
            },
            update = { view ->
                view.setContent {
                    WorkoutShareCard(
                        planName = planName,
                        duration = durationStr,
                        totalVolume = totalVolume,
                        topSets = topSets
                    )
                }
            },
            modifier = Modifier.padding(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                composeView?.let { view ->
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        view.width,
                        view.height,
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(bitmap)
                    view.draw(canvas)
                    onShare(bitmap)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(60.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("WORKOUT TEILEN", fontWeight = FontWeight.Black, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(60.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("ZUM DASHBOARD", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun WorkoutShareCard(
    planName: String,
    duration: String,
    totalVolume: Float,
    topSets: List<Pair<String, String>>
) {
    Column(
        modifier = Modifier
            .width(340.dp)
            .clip(MaterialTheme.shapes.large)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A1A),
                        Color(0xFF0D0D0D)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "WORKOUT SUMMARY",
                color = Color(0xFF2196F3),
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            planName.uppercase(),
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 30.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatItem(Icons.Default.Timer, "DAUER", duration)
            StatItem(Icons.Default.FitnessCenter, "WORKLOAD", "${totalVolume.toInt()} kg")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "TOP ÜBUNGEN",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        topSets.forEach { (exercise, details) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    exercise,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(details, color = Color(0xFF2196F3), fontWeight = FontWeight.Black)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            "GYM TRACKER",
            color = Color.White.copy(alpha = 0.2f),
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                label,
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun WorkoutNoteSection(
    equipmentId: Int,
    viewModel: GymViewModel,
    activeSessionId: Int,
    onImageClick: (String) -> Unit
) {
    var expanded by rememberSaveable(equipmentId) { mutableStateOf(false) }
    var selectedTab by rememberSaveable(equipmentId) { mutableIntStateOf(0) }
    val tabs = listOf("AKTUELL", "LETZTES", "ALLGEMEIN")
    val draftNotes by viewModel.activeSessionNotes.collectAsState()
    val currentDraft = draftNotes[equipmentId]
    val equipmentList by viewModel.equipmentList.collectAsState()
    val equipment = equipmentList.find { it.id == equipmentId }
    val lastNotePair by viewModel.getLastSessionNote(equipmentId, activeSessionId)
        .collectAsState(initial = null)
    val initialCurrentText = currentDraft?.first ?: ""
    val initialCurrentImages =
        currentDraft?.second?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
    var isCurrentEditing by rememberSaveable(equipmentId) { mutableStateOf(false) }
    var currentText by rememberSaveable(equipmentId) { mutableStateOf(initialCurrentText) }
    var currentImages by rememberSaveable(equipmentId) { mutableStateOf(initialCurrentImages) }

    LaunchedEffect(initialCurrentText, initialCurrentImages) {
        if (!isCurrentEditing) {
            currentText = initialCurrentText
            currentImages = initialCurrentImages
        }
    }

    val initialGeneralText = equipment?.generalNote ?: ""
    val initialGeneralImages =
        equipment?.generalNoteImageUris?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
    var isGeneralEditing by rememberSaveable(equipmentId) { mutableStateOf(false) }
    var generalText by rememberSaveable(equipmentId) { mutableStateOf(initialGeneralText) }
    var generalImages by rememberSaveable(equipmentId) { mutableStateOf(initialGeneralImages) }

    LaunchedEffect(initialGeneralText, initialGeneralImages) {
        if (!isGeneralEditing) {
            generalText = initialGeneralText
            generalImages = initialGeneralImages
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.5f
            )
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "NOTIZEN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
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
                            text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> EditableNoteBlock(
                        isEditing = isCurrentEditing,
                        onEditChange = { isCurrentEditing = it },
                        editText = currentText,
                        onTextChange = { currentText = it },
                        editImages = currentImages,
                        onImagesChange = { currentImages = it },
                        originalText = initialCurrentText,
                        originalImages = initialCurrentImages,
                        onSave = { txt, imgs ->
                            viewModel.updateActiveSessionNote(
                                equipmentId,
                                txt,
                                imgs
                            )
                        },
                        onImageClick = onImageClick
                    )

                    1 -> ReadOnlyNoteBlock(
                        noteText = lastNotePair?.first,
                        imageUrisString = lastNotePair?.second,
                        emptyMessage = "Keine Notiz vom letzten Training vorhanden.",
                        onImageClick = onImageClick
                    )

                    2 -> {
                        if (equipment != null) {
                            EditableNoteBlock(
                                isEditing = isGeneralEditing,
                                onEditChange = { isGeneralEditing = it },
                                editText = generalText,
                                onTextChange = { generalText = it },
                                editImages = generalImages,
                                onImagesChange = { generalImages = it },
                                originalText = initialGeneralText,
                                originalImages = initialGeneralImages,
                                onSave = { txt, imgs ->
                                    viewModel.updateEquipmentNote(
                                        equipment,
                                        txt,
                                        imgs
                                    )
                                },
                                onImageClick = onImageClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditableNoteBlock(
    isEditing: Boolean,
    onEditChange: (Boolean) -> Unit,
    editText: String,
    onTextChange: (String) -> Unit,
    editImages: List<String>,
    onImagesChange: (List<String>) -> Unit,
    originalText: String,
    originalImages: List<String>,
    onSave: (String?, String?) -> Unit,
    onImageClick: (String) -> Unit
) {
    val context = LocalContext.current
    var imageToDelete by remember { mutableStateOf<String?>(null) }
    var showDeleteNoteConfirm by remember { mutableStateOf(false) }
    var tempCameraUriString by remember { mutableStateOf<String?>(null) }

    val photoPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                onImagesChange(editImages + uris.map { it.toString() })
            }
        }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && tempCameraUriString != null) {
                onImagesChange(editImages + tempCameraUriString!!)
            }
        }

    if (imageToDelete != null) {
        AlertDialog(
            onDismissRequest = { imageToDelete = null },
            title = { Text("BILD LÖSCHEN?", fontWeight = FontWeight.Black) },
            text = { Text("Soll dieses Bild aus der Notiz gelöscht werden?") },
            confirmButton = {
                Button(
                    onClick = {
                        onImagesChange(editImages.filter { it != imageToDelete })
                        imageToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("LÖSCHEN") }
            },
            dismissButton = { TextButton(onClick = { imageToDelete = null }) { Text("ABBRECHEN") } }
        )
    }

    if (showDeleteNoteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteNoteConfirm = false },
            title = { Text("NOTIZ LÖSCHEN?", fontWeight = FontWeight.Black) },
            text = { Text("Möchtest du diese Notiz komplett leeren?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteNoteConfirm = false
                        onEditChange(false)
                        onTextChange("")
                        onImagesChange(emptyList())
                        onSave(null, null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("LÖSCHEN") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteNoteConfirm = false
                }) { Text("ABBRECHEN") }
            }
        )
    }

    if (isEditing) {
        Column {
            OutlinedTextField(
                value = editText,
                onValueChange = { onTextChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                placeholder = { Text("Deine Notizen") }
            )

            if (editImages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(editImages) { uriStr ->
                        AsyncImage(
                            model = uriStr,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .combinedClickable(
                                    onClick = { onImageClick(uriStr) },
                                    onLongClick = { imageToDelete = uriStr }
                                )
                        )
                    }
                }
                Text(
                    "Lange drücken, um ein Bild zu löschen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Galerie",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        try {
                            val photoFile =
                                File(context.filesDir, "gym_note_${System.currentTimeMillis()}.jpg")
                            val photoUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                photoFile
                            )
                            tempCameraUriString = photoUri.toString()
                            cameraLauncher.launch(photoUri)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Fehler: ${e.message}", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Kamera",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        onEditChange(false)
                        onTextChange(originalText)
                        onImagesChange(originalImages)
                    }) { Text("ABBRECHEN") }

                    Button(onClick = {
                        val finalNote = editText.takeIf { it.isNotBlank() }
                        val finalImages = editImages.joinToString("|").takeIf { it.isNotBlank() }
                        onSave(finalNote, finalImages)
                        onEditChange(false)
                    }) { Text("SPEICHERN") }
                }
            }
        }
    } else {
        if (originalText.isBlank() && originalImages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clickable { onEditChange(true) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Tippe hier, um eine Notiz hinzuzufügen.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        if (originalText.isNotBlank()) {
                            Text(originalText, style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Optionen")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            shape = MaterialTheme.shapes.medium,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            DropdownMenuItem(
                                text = { Text("Bearbeiten", fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Bearbeiten",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = { showMenu = false; onEditChange(true) }
                            )

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
                                onClick = { showMenu = false; showDeleteNoteConfirm = true }
                            )
                        }
                    }
                }

                if (originalImages.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(originalImages) { uriStr ->
                            AsyncImage(
                                model = uriStr,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { onImageClick(uriStr) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReadOnlyNoteBlock(
    noteText: String?,
    imageUrisString: String?,
    emptyMessage: String,
    onImageClick: (String) -> Unit
) {
    if (noteText.isNullOrBlank() && imageUrisString.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                emptyMessage,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            if (!noteText.isNullOrBlank()) {
                Text(noteText, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(12.dp))
            }
            val images = imageUrisString?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
            if (images.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(images) { uriStr ->
                        AsyncImage(
                            model = uriStr,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onImageClick(uriStr) }
                        )
                    }
                }
            }
        }
    }
}