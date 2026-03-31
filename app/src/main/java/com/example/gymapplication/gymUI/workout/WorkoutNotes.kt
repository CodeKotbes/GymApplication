package com.example.gymapplication.gymUI.workout

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.gymapplication.gymUI.viewmodel.GymViewModel
import com.example.gymapplication.gymUI.viewmodel.getLastSessionNote
import com.example.gymapplication.gymUI.viewmodel.updateActiveSessionNote
import com.example.gymapplication.gymUI.viewmodel.updateEquipmentNote

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
                        "NOTIZEN & INFOS",
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
            text = { Text("Soll dieses Bild aus der Notiz entfernt werden?") },
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
                placeholder = { Text("Deine Notizen (Tipp: Nutze - für Stichpunkte)") }
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