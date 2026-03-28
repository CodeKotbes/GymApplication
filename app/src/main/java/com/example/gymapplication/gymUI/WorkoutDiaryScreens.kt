package com.example.gymapplication.gymUI

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.gymapplication.data.WorkoutSession
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiaryZoomDialog(imageUri: String, onClose: () -> Unit) {
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDiaryScreen(viewModel: GymViewModel, onBack: () -> Unit) {
    Locale.setDefault(Locale.GERMANY)

    val finishedSessions by viewModel.finishedSessions.collectAsState(initial = emptyList())
    var selectedSessionId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedSession = finishedSessions.find { it.sessionId == selectedSessionId }
    var sessionToDeleteId by rememberSaveable { mutableStateOf<Int?>(null) }
    val sessionToDelete = finishedSessions.find { it.sessionId == sessionToDeleteId }
    var sessionToEditDateId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
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
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
                sessionToEditDateId = null
            },
            shape = MaterialTheme.shapes.large,
            colors = customDatePickerColors,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { newMillis ->
                        sessionToEditDateId?.let { id ->
                            val sessionToEdit = finishedSessions.find { it.sessionId == id }
                            sessionToEdit?.let {
                                viewModel.updateWorkoutSessionDate(it, newMillis)
                            }
                        }
                    }
                    showDatePicker = false
                    sessionToEditDateId = null
                }) {
                    Text(
                        "ÄNDERN",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    sessionToEditDateId = null
                }) { Text("ABBRECHEN", color = MaterialTheme.colorScheme.primary) }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = customDatePickerColors
            )
        }
    }

    if (selectedSession != null) {
        WorkoutDiaryDetailScreen(
            session = selectedSession,
            viewModel = viewModel,
            onBack = { selectedSessionId = null }
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
                        .clickable { selectedSessionId = session.sessionId },
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

                            val h = session.durationInSeconds / 3600
                            val m = (session.durationInSeconds % 3600) / 60
                            val s = session.durationInSeconds % 60
                            val durationText = if (session.durationInSeconds > 0) {
                                if (h > 0) String.format(
                                    Locale.getDefault(),
                                    " •  %02d:%02d:%02d Std.",
                                    h,
                                    m,
                                    s
                                )
                                else String.format(
                                    Locale.getDefault(),
                                    " •  %02d:%02d Min.",
                                    m,
                                    s
                                )
                            } else ""

                            Text(
                                "$date$durationText",
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
                                onDismissRequest = { showMenu = false },
                                shape = MaterialTheme.shapes.medium,
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Datum ändern", fontWeight = FontWeight.Bold) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = "Datum",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        sessionToEditDateId = session.sessionId
                                        datePickerState.selectedDateMillis =
                                            session.startTimeMillis
                                        showDatePicker = true
                                    }
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Workout löschen",
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
                                    onClick = {
                                        showMenu = false; sessionToDeleteId = session.sessionId
                                    }
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
                onDismissRequest = { sessionToDeleteId = null },
                title = { Text("LÖSCHEN BESTÄTIGEN", fontWeight = FontWeight.Black) },
                text = { Text("Soll dieses Workout wirklich gelöscht werden?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteWorkoutSession(sessionToDelete!!)
                            sessionToDeleteId = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("LÖSCHEN") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        sessionToDeleteId = null
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

    var fullscreenImageUri by rememberSaveable { mutableStateOf<String?>(null) }

    if (fullscreenImageUri != null) {
        DiaryZoomDialog(imageUri = fullscreenImageUri!!) { fullscreenImageUri = null }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding(),
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
                        val eqName = allEquipment.find { it.id == equipmentId }?.name ?: "Übung"
                        Text(
                            eqName.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(8.dp))

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

                        Spacer(modifier = Modifier.height(16.dp))

                        val firstLog = sessionLogs.firstOrNull()
                        if (firstLog != null) {
                            PastSessionNoteCard(
                                sessionId = session.sessionId,
                                equipmentId = equipmentId,
                                originalText = firstLog.sessionNote ?: "",
                                originalImagesString = firstLog.sessionNoteImageUris,
                                viewModel = viewModel,
                                onImageClick = { fullscreenImageUri = it }
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(120.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PastSessionNoteCard(
    sessionId: Int,
    equipmentId: Int,
    originalText: String,
    originalImagesString: String?,
    viewModel: GymViewModel,
    onImageClick: (String) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val originalImages = originalImagesString?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
    var isEditing by rememberSaveable(sessionId, equipmentId) { mutableStateOf(false) }
    var editText by rememberSaveable(sessionId, equipmentId) { mutableStateOf(originalText) }
    var editImages by rememberSaveable(sessionId, equipmentId) { mutableStateOf(originalImages) }
    var imageToDelete by remember { mutableStateOf<String?>(null) }
    var showDeleteNoteConfirm by remember { mutableStateOf(false) }
    var tempCameraUriString by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(originalText, originalImages) {
        if (!isEditing) {
            editText = originalText
            editImages = originalImages
        }
    }

    val photoPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                editImages = editImages + uris.map { it.toString() }
            }
        }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && tempCameraUriString != null) {
                editImages = editImages + tempCameraUriString!!
            }
        }

    if (imageToDelete != null) {
        AlertDialog(
            onDismissRequest = { imageToDelete = null },
            title = { Text("BILD LÖSCHEN?", fontWeight = FontWeight.Black) },
            text = { Text("Soll dieses Bild aus der Notiz entfernt werden?") },
            confirmButton = {
                Button(
                    onClick = {
                        editImages = editImages.filter { it != imageToDelete }
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
                        isEditing = false
                        editText = ""
                        editImages = emptyList()
                        viewModel.updatePastSessionNote(sessionId, equipmentId, null, null)
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.5f
            )
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "NOTIZEN FÜR DIESES TRAINING",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (isEditing) {
                Column {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = { Text("Was hast du an diesem Tag beachtet?") }
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
                                    val photoFile = File(
                                        context.filesDir,
                                        "gym_note_${System.currentTimeMillis()}.jpg"
                                    )
                                    val photoUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        photoFile
                                    )
                                    tempCameraUriString = photoUri.toString()
                                    cameraLauncher.launch(photoUri)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Fehler: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
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
                                isEditing = false
                                editText = originalText
                                editImages = originalImages
                            }) { Text("ABBR.") }

                            Button(onClick = {
                                keyboardController?.hide()
                                val finalNote = editText.takeIf { it.isNotBlank() }
                                val finalImages =
                                    editImages.joinToString("|").takeIf { it.isNotBlank() }
                                viewModel.updatePastSessionNote(
                                    sessionId,
                                    equipmentId,
                                    finalNote,
                                    finalImages
                                )
                                isEditing = false
                            }) { Text("SPEICHERN") }
                        }
                    }
                }
            } else {
                if (originalText.isBlank() && originalImages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable { isEditing = true }, contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Tippe hier, um eine Notiz nachträglich hinzuzufügen.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
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
                                    Text(originalText, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
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
                                        onClick = { showMenu = false; isEditing = true }
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
    }
}