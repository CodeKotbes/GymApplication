package com.example.gymapplication.gymUI

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.gymapplication.data.PlannedWorkout
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

// NEU: Der Samsung-Style Kalender
@Composable
fun GymCalendar(
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    finishedDates: Set<String>,
    plannedDates: Set<String>
) {
    var currentMonthMillis by rememberSaveable { mutableLongStateOf(selectedDateMillis) }
    val locale = Locale.GERMANY

    val currentMonthCal = Calendar.getInstance(locale).apply {
        timeInMillis = currentMonthMillis
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val monthYearFormat = SimpleDateFormat("MMMM yyyy", locale)
    val monthYearString = monthYearFormat.format(currentMonthCal.time)

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)) {
        // Monat & Pfeile Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                currentMonthCal.add(Calendar.MONTH, -1)
                currentMonthMillis = currentMonthCal.timeInMillis
            }) { Icon(Icons.Default.ArrowBack, contentDescription = "Zurück") }

            Text(
                monthYearString.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )

            IconButton(onClick = {
                currentMonthCal.add(Calendar.MONTH, 1)
                currentMonthMillis = currentMonthCal.timeInMillis
            }) { Icon(Icons.Default.ArrowForward, contentDescription = "Vor") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Wochentage (Beginnt garantiert am Montag)
        val weekdays = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Kalender Grid (Die Tage und Punkte)
        val firstDayOfWeek = currentMonthCal.get(Calendar.DAY_OF_WEEK)
        val startOffset = (firstDayOfWeek + 5) % 7 // Berechnet perfekten Montag-Start
        val daysInMonth = currentMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        val dateFormat = SimpleDateFormat("yyyyMMdd", locale)
        val selectedStr = dateFormat.format(Date(selectedDateMillis))
        val todayStr = dateFormat.format(Date())

        for (row in 0 until rows) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - startOffset + 1

                    if (dayNum in 1..daysInMonth) {
                        val cellCal = Calendar.getInstance(locale).apply {
                            timeInMillis = currentMonthMillis
                            set(Calendar.DAY_OF_MONTH, dayNum)
                        }
                        val dateStr = dateFormat.format(cellCal.time)
                        val isSelected = dateStr == selectedStr
                        val isToday = dateStr == todayStr

                        val hasFinished = finishedDates.contains(dateStr)
                        val hasPlanned = plannedDates.contains(dateStr)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onDateSelected(cellCal.timeInMillis) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNum.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isToday || isSelected) FontWeight.Black else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )

                            // Die kleinen Samsung-Punkte für Workouts!
                            if (hasFinished || hasPlanned) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    if (hasFinished) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                    if (hasPlanned) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(
                                                        alpha = 0.7f
                                                    ) else MaterialTheme.colorScheme.secondary
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f))
                    }
                }
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
                // Vorbereitung für unsere kleinen Punkte im Kalender!
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
                    // UNSER NEUER CUSTOM KALENDER EINSATZ:
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
                                        " • ⏱️ %02d:%02d:%02d Std.",
                                        h,
                                        m,
                                        s
                                    )
                                } else {
                                    String.format(Locale.getDefault(), " • ⏱️ %02d:%02d Min.", m, s)
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
                        keyboardController?.hide(); selectedPlanToSchedule?.let {
                        viewModel.scheduleWorkout(
                            context,
                            it.id,
                            it.name,
                            selectedMillis
                        )
                    }; showPlanDialog = false
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
                TextButton(onClick = {
                    sessionToDelete = null
                }) { Text("ABBRECHEN") }
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
                TextButton(onClick = {
                    plannedToDelete = null
                }) { Text("ABBRECHEN") }
            }
        )
    }
}