package com.example.gymapplication.gymUI

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: GymViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activeSession by viewModel.activeSession.collectAsState()
    val workoutDuration by viewModel.workoutDuration.collectAsState()

    if (activeSession == null) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }

    val equipmentInPlan by if (activeSession!!.planId != null) {
        viewModel.getEquipmentWithLogsForPlanFlow(activeSession!!.planId!!)
            .collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    val currentIndex by viewModel.currentExerciseIndex.collectAsState()

    val isResting by viewModel.isResting.collectAsState()
    val restSecondsLeft by viewModel.restSecondsLeft.collectAsState()
    val defaultRestTime by viewModel.currentRestTime.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(activeSession!!.name.uppercase(), fontWeight = FontWeight.Black)
                        val minutes = workoutDuration / 60
                        val seconds = workoutDuration % 60
                        Text(
                            text = String.format(
                                Locale.getDefault(),
                                "%02d:%02d",
                                minutes,
                                seconds
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Minimieren")
                    }
                },
                actions = {
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
                            viewModel.finishWorkout(context)
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("BEENDEN", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->

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
                        Text("PAUSE ÜBERSPRINGEN", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }

            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                ) {

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentEquipment.imageUri != null) {
                                AsyncImage(
                                    model = currentEquipment.imageUri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(MaterialTheme.shapes.medium),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            Column {
                                Text(
                                    text = "ÜBUNG ${currentIndex + 1} VON ${equipmentInPlan.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentEquipment.name.uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        key(currentIndex) {
                            var weightInput by remember {
                                mutableStateOf(
                                    currentEquipment.latestWeight?.toString() ?: ""
                                )
                            }
                            var repsInput by remember {
                                mutableStateOf(
                                    currentEquipment.latestReps?.toString() ?: ""
                                )
                            }
                            var isWarmup by remember { mutableStateOf(false) }

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
                                                text = "ZULETZT: ${ghost.weight} kg",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.4f
                                                ),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${ghost.reps} REPS",
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
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            enabled = !isWarmup
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Button(
                                        onClick = {
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
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 32.dp),
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

                        OutlinedButton(
                            onClick = {
                                if (currentIndex < equipmentInPlan.size - 1) viewModel.updateExerciseIndex(
                                    currentIndex + 1
                                )
                            },
                            enabled = currentIndex < equipmentInPlan.size - 1,
                            modifier = Modifier
                                .weight(1f)
                                .height(55.dp)
                        ) {
                            Text("NÄCHSTE")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}