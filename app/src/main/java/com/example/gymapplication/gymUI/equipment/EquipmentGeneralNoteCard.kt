package com.example.gymapplication.gymUI.equipment

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.gymapplication.data.Equipment
import com.example.gymapplication.gymUI.viewmodel.GymViewModel
import com.example.gymapplication.gymUI.viewmodel.updateEquipmentNote
import com.example.gymapplication.gymUI.workout.LinkifiedText
import java.io.File
import kotlin.collections.plus

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EquipmentGeneralNoteCard(
    equipment: Equipment,
    viewModel: GymViewModel,
    onImageClick: (String) -> Unit
) {
    val context = LocalContext.current
    val originalText = equipment.generalNote ?: ""
    val originalImages =
        equipment.generalNoteImageUris?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
    var showBottomSheet by rememberSaveable(equipment.id) { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editText by rememberSaveable(equipment.id) { mutableStateOf(originalText) }
    var editImages by rememberSaveable(equipment.id) { mutableStateOf(originalImages) }
    var imageToDelete by remember { mutableStateOf<String?>(null) }
    var showDeleteNoteConfirm by remember { mutableStateOf(false) }
    var tempCameraUriString by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showBottomSheet) {
        if (showBottomSheet) {
            editText = equipment.generalNote ?: ""
            editImages = equipment.generalNoteImageUris?.split("|")?.filter { it.isNotBlank() }
                ?: emptyList()
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
            text = { Text("Soll dieses Bild aus der Notiz gelöscht werden?") },
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
                        viewModel.updateEquipmentNote(equipment, null, null)
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ALLGEMEINE NOTIZEN",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

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
                            onClick = { showMenu = false; showBottomSheet = true }
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
            Spacer(modifier = Modifier.height(16.dp))

            if (originalText.isBlank() && originalImages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clickable { showBottomSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Tippe hier, um eine Notiz hinzuzufügen.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column {
                    if (originalText.isNotBlank()) {
                        LinkifiedText(
                            text = originalText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(12.dp))
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

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding() + 16.dp
                    )
            ) {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
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
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
                        TextButton(onClick = { showBottomSheet = false }) { Text("ABBRECHEN") }

                        Button(onClick = {
                            val finalNote = editText.takeIf { it.isNotBlank() }
                            val finalImages =
                                editImages.joinToString("|").takeIf { it.isNotBlank() }
                            viewModel.updateEquipmentNote(equipment, finalNote, finalImages)
                            showBottomSheet = false
                        }) { Text("SPEICHERN") }
                    }
                }
            }
        }
    }
}