package com.example.gymapplication.gymUI

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymapplication.data.WorkoutSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutDiaryScreen(viewModel: GymViewModel, onBack: () -> Unit) {
    val finishedSessions by viewModel.finishedSessions.collectAsState(initial = emptyList())
    var selectedSession by remember { mutableStateOf<WorkoutSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<WorkoutSession?>(null) }

    val context = LocalContext.current
    var sessionToEditDate by remember { mutableStateOf<WorkoutSession?>(null) }
    val calendar = remember { java.util.Calendar.getInstance() }

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = java.util.Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            sessionToEditDate?.let {
                viewModel.updateWorkoutSessionDate(it, cal.timeInMillis)
            }
            sessionToEditDate = null
        },
        calendar.get(java.util.Calendar.YEAR),
        calendar.get(java.util.Calendar.MONTH),
        calendar.get(java.util.Calendar.DAY_OF_MONTH)
    )

    datePickerDialog.setOnDismissListener { sessionToEditDate = null }

    if (selectedSession != null) {
        WorkoutDiaryDetailScreen(
            session = selectedSession!!,
            viewModel = viewModel,
            onBack = { selectedSession = null }
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                    Text(
                        "TRAININGSTAGEBUCH",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "MEINE WORKOUTS",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
            }

            items(finishedSessions) { session ->
                var showMenu by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedSession = session },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                session.name.uppercase(),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            val date = SimpleDateFormat(
                                "dd. MMM yyyy • HH:mm",
                                Locale.getDefault()
                            ).format(Date(session.startTimeMillis))
                            Text(
                                date,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    text = { Text("Datum ändern") },
                                    onClick = {
                                        showMenu = false
                                        sessionToEditDate = session
                                        calendar.timeInMillis = session.startTimeMillis
                                        datePickerDialog.updateDate(
                                            calendar.get(java.util.Calendar.YEAR),
                                            calendar.get(java.util.Calendar.MONTH),
                                            calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                        )
                                        datePickerDialog.show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Workout löschen",
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
            item { Spacer(modifier = Modifier.height(120.dp)) }
        }

        if (sessionToDelete != null) {
            AlertDialog(
                onDismissRequest = { sessionToDelete = null },
                title = { Text("LÖSCHEN BESTÄTIGEN", fontWeight = FontWeight.Black) },
                text = { Text("Soll dieses Workout wirklich gelöscht werden?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteWorkoutSession(sessionToDelete!!); sessionToDelete =
                            null
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
    }
}

@Composable
fun WorkoutDiaryDetailScreen(session: WorkoutSession, viewModel: GymViewModel, onBack: () -> Unit) {
    val logs by viewModel.getLogsForSessionFlow(session.sessionId)
        .collectAsState(initial = emptyList())
    val allEquipment by viewModel.equipmentList.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Zurück"
                    )
                }
                Text(
                    "DETAILS: ${session.name.uppercase()}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        val groupedLogs = logs.groupBy { it.equipmentId }
        groupedLogs.forEach { (equipmentId, sessionLogs) ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val eqName = allEquipment.find { it.id == equipmentId }?.name ?: "Gerät"
                        Text(
                            eqName.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                        sessionLogs.forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Satz ${log.setNumber}", fontWeight = FontWeight.Bold)
                                Text("${log.weight} kg × ${log.reps}")
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(120.dp)) }
    }
}