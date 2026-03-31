package com.example.gymapplication.gymUI.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymapplication.data.PlannedWorkout
import com.example.gymapplication.data.WorkoutPlan
import com.example.gymapplication.data.WorkoutSession
import com.example.gymapplication.gymUI.workout.WorkoutDiaryDetailScreen
import com.example.gymapplication.gymUI.plan.PlanDetailScreen
import com.example.gymapplication.gymUI.viewmodel.GymViewModel
import com.example.gymapplication.gymUI.viewmodel.deletePlannedWorkout
import com.example.gymapplication.gymUI.viewmodel.deleteWorkoutSession
import com.example.gymapplication.gymUI.viewmodel.scheduleWorkout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: GymViewModel, navController: NavController) {
    val finishedSessions by viewModel.finishedSessions.collectAsState(initial = emptyList())
    val plannedWorkouts by viewModel.plannedWorkouts.collectAsState(initial = emptyList())
    val allPlans by viewModel.workoutPlans.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val locale = Locale.GERMANY
    var selectedMillis by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    val selectedDateStr = SimpleDateFormat("EEEE, dd. MMMM", locale).format(Date(selectedMillis))
    var showPlanDialog by rememberSaveable { mutableStateOf(false) }
    var sessionDetailsId by rememberSaveable { mutableStateOf<Int?>(null) }
    val sessionDetails = finishedSessions.find { it.sessionId == sessionDetailsId }
    var planDetailsId by rememberSaveable { mutableStateOf<Int?>(null) }
    val planDetails = allPlans.find { it.id == planDetailsId }
    var fullscreenImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var sessionToDelete by remember { mutableStateOf<WorkoutSession?>(null) }
    var plannedToDelete by remember { mutableStateOf<PlannedWorkout?>(null) }

    if (fullscreenImageUri != null) {
        CalendarZoomDialog(imageUri = fullscreenImageUri!!) { fullscreenImageUri = null }
    }

    if (sessionDetails != null) {
        BackHandler { sessionDetailsId = null }
        WorkoutDiaryDetailScreen(
            session = sessionDetails,
            viewModel = viewModel,
            onBack = { sessionDetailsId = null })
        return
    }
    if (planDetails != null) {
        BackHandler { planDetailsId = null }
        PlanDetailScreen(
            plan = planDetails,
            viewModel = viewModel,
            navController = navController,
            onBack = { planDetailsId = null })
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
                val dateFormatForDots = SimpleDateFormat("yyyyMMdd", locale)
                val finishedDatesSet =
                    finishedSessions.map { dateFormatForDots.format(Date(it.startTimeMillis)) }
                        .toSet()
                val plannedDatesSet =
                    plannedWorkouts.map { dateFormatForDots.format(Date(it.dateMillis)) }.toSet()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .clip(MaterialTheme.shapes.large),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    GymCalendar(
                        selectedDateMillis = selectedMillis,
                        onDateSelected = { selectedMillis = it },
                        finishedDates = finishedDatesSet,
                        plannedDates = plannedDatesSet
                    )
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
                        onClick = { keyboardController?.hide(); showPlanDialog = true },
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
                        .clickable { sessionDetailsId = session.sessionId },
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

                            val h = session.durationInSeconds / 3600
                            val m = (session.durationInSeconds % 3600) / 60
                            val s = session.durationInSeconds % 60

                            val durationText = if (session.durationInSeconds > 0) {
                                if (h > 0) {
                                    String.format(
                                        Locale.getDefault(),
                                        " •  %02d:%02d:%02d Std.",
                                        h,
                                        m,
                                        s
                                    )
                                } else {
                                    String.format(Locale.getDefault(), " •  %02d:%02d Min.", m, s)
                                }
                            } else ""

                            Text(
                                "ABSOLVIERT$durationText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null)
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
                                    onClick = { showMenu = false; sessionToDelete = session }
                                )
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
                        .clickable { planDetailsId = planned.planId },
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
                            val timeStr =
                                SimpleDateFormat("HH:mm", locale).format(Date(planned.dateMillis))
                            Text(
                                "GEPLANT",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null)
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
                                    onClick = { showMenu = false; plannedToDelete = planned }
                                )
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
        var showTimePickerForPlan by remember { mutableStateOf(false) }
        val timePickerState = rememberTimePickerState(
            initialHour = 9,
            initialMinute = 0,
            is24Hour = true
        )

        if (showTimePickerForPlan) {
            AlertDialog(
                onDismissRequest = { showTimePickerForPlan = false },
                title = { Text("UHRZEIT WÄHLEN", fontWeight = FontWeight.Black) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TimePicker(state = timePickerState)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTimePickerForPlan = false }) {
                        Text("ÜBERNEHMEN", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        AlertDialog(
            onDismissRequest = { showPlanDialog = false },
            title = { Text("PLAN AUSWÄHLEN", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text(
                        "ERINNERUNG UM:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showTimePickerForPlan = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(
                                Locale.getDefault(),
                                "%02d:%02d UHR",
                                timePickerState.hour,
                                timePickerState.minute
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    if (allPlans.isEmpty()) {
                        Text("Du hast noch keine Pläne erstellt.")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxHeight(0.5f)) {
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyboardController?.hide()

                        selectedPlanToSchedule?.let { plan ->
                            val finalCalendar = Calendar.getInstance(locale).apply {
                                timeInMillis = selectedMillis
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE, timePickerState.minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            viewModel.scheduleWorkout(
                                context = context,
                                planId = plan.id,
                                planName = plan.name,
                                dateMillis = finalCalendar.timeInMillis
                            )
                        }
                        showPlanDialog = false
                    },
                    shape = MaterialTheme.shapes.medium,
                    enabled = selectedPlanToSchedule != null
                ) { Text("HINZUFÜGEN") }
            },
            dismissButton = {
                TextButton(onClick = { showPlanDialog = false }) { Text("ABBRECHEN") }
            }
        )
    }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("WORKOUT LÖSCHEN?", fontWeight = FontWeight.Black) },
            text = { Text("Soll dieses abgeschlossene Workout wirklich gelöscht werden?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWorkoutSession(sessionToDelete!!); sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("LÖSCHEN") }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) { Text("ABBRECHEN") }
            }
        )
    }

    if (plannedToDelete != null) {
        AlertDialog(
            onDismissRequest = { plannedToDelete = null },
            title = { Text("GEPLANTES WORKOUT LÖSCHEN?", fontWeight = FontWeight.Black) },
            text = { Text("Möchtest du dieses geplante Workout aus dem Kalender entfernen?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlannedWorkout(
                            context,
                            plannedToDelete!!
                        ); plannedToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("LÖSCHEN") }
            },
            dismissButton = {
                TextButton(onClick = { plannedToDelete = null }) { Text("ABBRECHEN") }
            }
        )
    }
}