package com.example.gymapplication.gymUI

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.gymapplication.data.Equipment
import com.example.gymapplication.data.WorkoutPlan
import java.util.Locale

@Composable
fun PlanDetailScreen(
    plan: WorkoutPlan,
    viewModel: GymViewModel,
    navController: NavController,
    onBack: () -> Unit
) {
    val equipmentInPlan by viewModel.getEquipmentWithLogsForPlanFlow(plan.id)
        .collectAsState(initial = emptyList())
    val allEquipment by viewModel.equipmentWithLatestLogs.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showMultiSelectDialog by rememberSaveable { mutableStateOf(false) }
    var equipmentForHistory by remember { mutableStateOf<Equipment?>(null) }
    var showStartDialog by rememberSaveable { mutableStateOf(false) }
    var fullscreenImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var showOrderDialogFor by rememberSaveable { mutableStateOf<Int?>(null) }
    var newOrderInput by rememberSaveable { mutableStateOf("") }
    val activeSession by viewModel.activeSession.collectAsState()

    if (fullscreenImageUri != null) {
        PlanZoomDialog(imageUri = fullscreenImageUri!!) { fullscreenImageUri = null }
    }

    if (equipmentForHistory != null) {
        HistoryDetailScreen(
            equipment = equipmentForHistory!!,
            viewModel = viewModel,
            onBack = { equipmentForHistory = null }
        )
    } else {
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
                            plan.name.uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                    }
                    IconButton(onClick = {
                        PlanExporter.exportAndSharePlan(
                            context,
                            plan,
                            equipmentInPlan
                        )
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Teilen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (activeSession != null) {
                    Button(
                        onClick = { navController.navigate("active_workout") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(65.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(
                            "ZUM AKTIVEN WORKOUT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else {
                    Button(
                        onClick = { showStartDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(65.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            "WORKOUT STARTEN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showMultiSelectDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = MaterialTheme.shapes.medium
                ) { Text("GERÄTE HINZUFÜGEN", fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    "ÜBUNGEN IM PLAN",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
            }

            itemsIndexed(equipmentInPlan) { index, equipment ->
                var showEqMenu by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            equipmentForHistory = Equipment(
                                equipment.id,
                                equipment.name,
                                equipment.muscleGroup,
                                equipment.imageUri
                            )
                        },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    showOrderDialogFor = index; newOrderInput =
                                    (index + 1).toString()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        if (equipment.imageUri != null) {
                            AsyncImage(
                                model = equipment.imageUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(65.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { fullscreenImageUri = equipment.imageUri },
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                equipment.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                equipment.muscleGroup.uppercase(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            if (equipment.latestWeight != null) {
                                Text(
                                    "Zuletzt: ${equipment.latestSets}×${equipment.latestReps} (${equipment.latestWeight} kg)",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Box {
                            IconButton(onClick = {
                                showEqMenu = true
                            }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                            DropdownMenu(
                                expanded = showEqMenu,
                                onDismissRequest = { showEqMenu = false }) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Aus Plan entfernen",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showEqMenu = false; viewModel.removeEquipmentFromPlan(
                                        plan.id,
                                        equipment.id
                                    )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(120.dp)) }
        }

        if (showOrderDialogFor != null) {
            AlertDialog(
                onDismissRequest = { showOrderDialogFor = null },
                title = { Text("POSITION ÄNDERN", fontWeight = FontWeight.Black) },
                text = {
                    OutlinedTextField(
                        value = newOrderInput,
                        onValueChange = { newOrderInput = it },
                        label = { Text("Neue Position (1 - ${equipmentInPlan.size})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            val fromIndex = showOrderDialogFor!!
                            var toIndex = (newOrderInput.toIntOrNull() ?: (fromIndex + 1)) - 1
                            toIndex = toIndex.coerceIn(0, equipmentInPlan.size - 1)
                            if (fromIndex != toIndex) {
                                val mutableList = equipmentInPlan.toMutableList()
                                val item = mutableList.removeAt(fromIndex)
                                mutableList.add(toIndex, item)
                                viewModel.reorderEquipmentInPlan(plan.id, mutableList)
                            }
                            showOrderDialogFor = null
                        },
                        shape = MaterialTheme.shapes.medium
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showOrderDialogFor = null
                    }) { Text("ABBRECHEN") }
                }
            )
        }

        if (showMultiSelectDialog) {
            val existingIds = equipmentInPlan.map { it.id }.toSet()
            val availableEquipment = allEquipment.filter { it.id !in existingIds }
            var selectedEquipmentIds by remember { mutableStateOf(setOf<Int>()) }
            AlertDialog(
                onDismissRequest = { showMultiSelectDialog = false },
                title = { Text("GERÄTE AUSWÄHLEN", fontWeight = FontWeight.Black) },
                text = {
                    if (availableEquipment.isEmpty()) {
                        Text("Du hast bereits alle deine Geräte in diesem Plan.")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                            items(availableEquipment) { eq ->
                                val isSelected = selectedEquipmentIds.contains(eq.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedEquipmentIds =
                                                if (isSelected) selectedEquipmentIds - eq.id else selectedEquipmentIds + eq.id
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedEquipmentIds =
                                                if (checked) selectedEquipmentIds + eq.id else selectedEquipmentIds - eq.id
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            eq.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            eq.muscleGroup.uppercase(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
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
                            keyboardController?.hide(); if (selectedEquipmentIds.isNotEmpty()) {
                            viewModel.addMultipleEquipmentToPlan(
                                plan.id,
                                selectedEquipmentIds.toList()
                            )
                        }; showMultiSelectDialog = false
                        },
                        shape = MaterialTheme.shapes.medium
                    ) { Text("HINZUFÜGEN (${selectedEquipmentIds.size})") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showMultiSelectDialog = false
                    }) { Text("ABBRECHEN") }
                }
            )
        }

        if (showStartDialog) {
            var selectedRestTime by rememberSaveable { mutableIntStateOf(120) }
            AlertDialog(
                onDismissRequest = { showStartDialog = false },
                title = { Text("WORKOUT SETUP", fontWeight = FontWeight.Black) },
                text = {
                    Column {
                        Text(
                            "Plan: ${plan.name.uppercase()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Pausenzeit zwischen Sätzen:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        val m = selectedRestTime / 60
                        val s = selectedRestTime % 60
                        Text(
                            String.format(Locale.getDefault(), "%02d:%02d Min.", m, s),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Slider(
                            value = selectedRestTime.toFloat(),
                            onValueChange = { selectedRestTime = it.toInt() },
                            valueRange = 30f..180f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            keyboardController?.hide(); viewModel.startWorkout(
                            context,
                            plan.id,
                            plan.name,
                            selectedRestTime
                        ); showStartDialog = false; navController.navigate("active_workout")
                        },
                        shape = MaterialTheme.shapes.medium
                    ) { Text("START", fontWeight = FontWeight.Black) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showStartDialog = false
                    }) { Text("ABBRECHEN") }
                }
            )
        }
    }
}

@Composable
fun PlanZoomDialog(imageUri: String, onClose: () -> Unit) {
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
            ) { Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.White) }
        }
    }
}