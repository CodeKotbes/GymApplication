package com.example.gymapplication.gymUI

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.gymapplication.data.WorkoutPlan
import com.example.gymapplication.data.WorkoutSession
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarZoomDialog(imageUri: String, onClose: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale == 1f) {
                            offset = Offset.Zero
                        } else {
                            offset += pan
                        }
                    }
                }
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: GymViewModel, navController: NavController) {
    val finishedSessions by viewModel.finishedSessions.collectAsState(initial = emptyList())
    val plannedWorkouts by viewModel.plannedWorkouts.collectAsState(initial = emptyList())
    val allPlans by viewModel.workoutPlans.collectAsState(initial = emptyList())

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val locale = Locale.GERMANY
    val configuration = LocalConfiguration.current
    configuration.setLocale(locale)

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    val selectedMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
    val selectedDateStr = SimpleDateFormat("EEEE, dd. MMMM", locale).format(Date(selectedMillis))

    var showPlanDialog by remember { mutableStateOf(false) }
    var sessionDetails by remember { mutableStateOf<WorkoutSession?>(null) }
    var planDetails by remember { mutableStateOf<WorkoutPlan?>(null) }
    var fullscreenImageUri by remember { mutableStateOf<String?>(null) }

    if (fullscreenImageUri != null) {
        CalendarZoomDialog(imageUri = fullscreenImageUri!!) { fullscreenImageUri = null }
    }

    if (sessionDetails != null) {
        BackHandler { sessionDetails = null }

        WorkoutDiaryDetailScreen(
            session = sessionDetails!!,
            viewModel = viewModel,
            onBack = { sessionDetails = null })
        return
    }
    if (planDetails != null) {
        BackHandler { planDetails = null }
        PlanDetailScreen(
            plan = planDetails!!,
            viewModel = viewModel,
            navController = navController,
            onBack = { planDetails = null })
        return
    }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "TRAININGS-KALENDER",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                CompositionLocalProvider(androidx.compose.ui.platform.LocalConfiguration provides configuration) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .clip(MaterialTheme.shapes.large),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        DatePicker(
                            state = datePickerState,
                            title = null,
                            headline = null,
                            showModeToggle = false,
                            colors = DatePickerDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.primary,
                                headlineContentColor = MaterialTheme.colorScheme.primary,
                                selectedDayContainerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(425.dp)
                                .padding(bottom = 8.dp)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedDateStr.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            showPlanDialog = true
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("PLANEN", fontWeight = FontWeight.Bold)
                    }
                }
            }

            val selectedCalendar =
                Calendar.getInstance(locale).apply { timeInMillis = selectedMillis }
            val selYear = selectedCalendar.get(Calendar.YEAR)
            val selDay = selectedCalendar.get(Calendar.DAY_OF_YEAR)

            val sessionsToday = finishedSessions.filter { session ->
                val cal =
                    Calendar.getInstance(locale).apply { timeInMillis = session.startTimeMillis }
                cal.get(Calendar.YEAR) == selYear && cal.get(Calendar.DAY_OF_YEAR) == selDay
            }

            val plannedToday = plannedWorkouts.filter { planned ->
                val cal = Calendar.getInstance(locale).apply { timeInMillis = planned.dateMillis }
                cal.get(Calendar.YEAR) == selYear && cal.get(Calendar.DAY_OF_YEAR) == selDay
            }

            if (sessionsToday.isEmpty() && plannedToday.isEmpty()) {
                item {
                    Text(
                        "Keine Einträge für diesen Tag.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            items(sessionsToday) { session ->
                var showMenu by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { sessionDetails = session },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                session.name.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "ABSOLVIERT",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
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
                                    text = {
                                        Text(
                                            "Löschen",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showMenu = false; viewModel.deleteWorkoutSession(session)
                                    })
                            }
                        }
                    }
                }
            }

            items(plannedToday) { planned ->
                var showMenu by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { planDetails = allPlans.find { it.id == planned.planId } },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                planned.planName.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "GEPLANT",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
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
                                    text = {
                                        Text(
                                            "Löschen",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showMenu = false; viewModel.deletePlannedWorkout(
                                        context,
                                        planned
                                    )
                                    })
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    if (showPlanDialog) {
        var selectedPlanToSchedule by remember { mutableStateOf<WorkoutPlan?>(null) }
        AlertDialog(
            onDismissRequest = { showPlanDialog = false },
            title = { Text("PLAN AUSWÄHLEN", fontWeight = FontWeight.Black) },
            text = {
                if (allPlans.isEmpty()) {
                    Text("Du hast noch keine Pläne erstellt.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                        items(allPlans) { plan ->
                            val isSelected = selectedPlanToSchedule == plan
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPlanToSchedule = plan }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { selectedPlanToSchedule = plan },
                                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    plan.name.uppercase(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        selectedPlanToSchedule?.let {
                            viewModel.scheduleWorkout(
                                context,
                                it.id,
                                it.name,
                                selectedMillis
                            )
                        }
                        showPlanDialog = false
                    },
                    shape = MaterialTheme.shapes.medium,
                    enabled = selectedPlanToSchedule != null
                ) { Text("HINZUFÜGEN") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPlanDialog = false
                }) { Text("ABBRECHEN") }
            }
        )
    }
}