package com.example.gymapplication.gymUI.workout

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.util.Locale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.gymapplication.gymUI.GymViewModel
import com.example.gymapplication.gymUI.analysis.HistoryZoomDialog
import com.example.gymapplication.gymUI.workout.ShareCardManager
import kotlin.collections.component1
import kotlin.collections.component2

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
    var showFinishDialog by rememberSaveable { mutableStateOf(false) }
    var finalPlanName by rememberSaveable { mutableStateOf("") }
    var finalDuration by rememberSaveable { mutableLongStateOf(0L) }
    val triggerSummary by viewModel.triggerSummaryEvent.collectAsState()
    val personalRecords by viewModel.personalRecords.collectAsState()

    LaunchedEffect(triggerSummary) {
        if (triggerSummary && activeSession != null) {
            showFinishDialog = true
            viewModel.consumeSummaryEvent()
        }
    }

    if (activeSession == null && !showSummary && !showFinishDialog) {
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

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("TRAINING BEENDEN?", fontWeight = FontWeight.Black) },
            text = { Text("Möchtest du dieses Workout wirklich abschließen und zur Auswertung gehen?") },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishDialog = false
                        finalPlanName = activeSession?.name ?: "Freies Workout"
                        finalDuration = workoutDuration
                        if (activeSession?.isPaused == false) viewModel.toggleWorkoutPause(context)
                        showSummary = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("JA, BEENDEN", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("WEITER TRAINIEREN") }
            }
        )
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
                            onClick = { showFinishDialog = true },
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
            val allSetsList = sessionLogs
                .groupBy { it.equipmentId }
                .mapNotNull { (equipmentId, logs) ->
                    val eqName = allEquipment.find { it.id == equipmentId }?.name ?: "Übung"
                    val bestLog = logs.maxWithOrNull(compareBy({ it.weight }, { it.reps }))
                    if (bestLog != null) Triple(
                        eqName,
                        "${bestLog.weight} kg x ${bestLog.reps}",
                        bestLog.weight
                    ) else null
                }
                .sortedByDescending { it.third }
                .map { it.first to it.second }

            WorkoutSummaryView(
                modifier = Modifier.padding(innerPadding),
                planName = finalPlanName,
                duration = finalDuration,
                totalVolume = calculatedVolume,
                topSets = allSetsList,
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
                            val currentPR =
                                personalRecords.find { it.equipmentName == currentEquipment.name }
                            val prWeight = currentPR?.maxWeight ?: 0f
                            val prReps = currentPR?.repsAtMaxWeight ?: 0

                            val inputWeight = weightInput.replace(",", ".").toFloatOrNull() ?: 0f
                            val inputReps = repsInput.toIntOrNull() ?: 0

                            val prMessage: String?
                            val isNewPR: Boolean

                            if (prWeight == 0f && inputWeight > 0 && inputReps > 0) {
                                prMessage = "Dein erster Rekord für diese Übung!"
                                isNewPR = true
                            } else if (inputWeight > 0f) {
                                if (inputWeight > prWeight) {
                                    prMessage =
                                        if (inputReps > 0) "NEUER GEWICHTS-REKORD!" else "Das wäre ein neuer Gewichts-Rekord!"
                                    isNewPR = inputReps > 0
                                } else if (inputWeight == prWeight) {
                                    when {
                                        inputReps == prReps - 1 -> {
                                            prMessage = "Nur noch 1 Rep für den Rekord!"
                                            isNewPR = false
                                        }

                                        inputReps == prReps -> {
                                            prMessage = "Rekord eingestellt!"
                                            isNewPR = false
                                        }

                                        inputReps > prReps -> {
                                            prMessage = "NEUER REP-REKORD!"
                                            isNewPR = true
                                        }

                                        else -> {
                                            prMessage = "Aktueller PR: $prWeight kg x $prReps"
                                            isNewPR = false
                                        }
                                    }
                                } else {
                                    prMessage = null
                                    isNewPR = false
                                }
                            } else {
                                prMessage = null
                                isNewPR = false
                            }

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

                                    AnimatedVisibility(
                                        visible = prMessage != null && !isWarmup,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Text(
                                            text = prMessage ?: "",
                                            color = if (isNewPR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (isNewPR) FontWeight.Black else FontWeight.Bold,
                                            modifier = Modifier
                                                .padding(top = 16.dp, bottom = 4.dp)
                                                .fillMaxWidth(),
                                            textAlign = TextAlign.Center
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
                                                    if (isNewPR) {
                                                        Toast.makeText(
                                                            context,
                                                            "Neuer Rekord gespeichert!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }

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
                                    onClick = { showFinishDialog = true },
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