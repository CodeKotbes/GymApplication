package com.example.gymapplication.gymUI

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gymapplication.data.Equipment
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EquipmentScreen(viewModel: GymViewModel) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val equipmentList by viewModel.equipmentWithLatestLogs.collectAsState()

    var selectedEquipmentId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedEquipment = equipmentList.find { it.id == selectedEquipmentId }

    var fullscreenImageUri by rememberSaveable { mutableStateOf<String?>(null) }

    var eqToEditId by rememberSaveable { mutableStateOf<Int?>(null) }
    val eqToEdit = equipmentList.find { it.id == eqToEditId }

    // NEU: ID für Löschbestätigung
    var eqToDeleteId by rememberSaveable { mutableStateOf<Int?>(null) }
    val eqToDelete = equipmentList.find { it.id == eqToDeleteId }

    var editName by rememberSaveable { mutableStateOf("") }
    var editMuscle by rememberSaveable { mutableStateOf("") }
    var editImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val editImageUri = editImageUriString?.let { Uri.parse(it) }

    var editTempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val editGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) editImageUriString = uri.toString()
    }
    val editCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) editImageUriString = editTempCameraUri?.toString()
    }

    if (fullscreenImageUri != null) {
        ZoomableImageDialog(imageUri = fullscreenImageUri!!) { fullscreenImageUri = null }
    }

    if (selectedEquipment != null) {
        BackHandler { selectedEquipmentId = null }
        val actualEquipment = Equipment(
            id = selectedEquipment.id,
            name = selectedEquipment.name,
            muscleGroup = selectedEquipment.muscleGroup,
            imageUri = selectedEquipment.imageUri
        )
        EquipmentDetailScreen(
            equipment = actualEquipment,
            viewModel = viewModel,
            onBack = { selectedEquipmentId = null }
        )
    } else {
        var step by rememberSaveable { mutableIntStateOf(1) }
        var equipmentName by rememberSaveable { mutableStateOf("") }
        var muscleGroup by rememberSaveable { mutableStateOf("") }
        var imageUriString by rememberSaveable { mutableStateOf<String?>(null) }
        val imageUri = imageUriString?.let { Uri.parse(it) }
        var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
        var errorMessage by rememberSaveable { mutableStateOf("") }

        val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) imageUriString = uri.toString()
        }
        val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) imageUriString = tempCameraUri?.toString()
        }

        fun saveImageToInternalStorage(uri: Uri): String {
            val file = File(context.filesDir, "gym_img_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            return file.absolutePath
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("MEINE GERÄTE", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("GERÄT HINZUFÜGEN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (errorMessage.isNotEmpty()) {
                            Text(text = errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        AnimatedContent(targetState = step, label = "wizard") { currentStep ->
                            when (currentStep) {
                                1 -> {
                                    Column {
                                        OutlinedTextField(value = equipmentName, onValueChange = { equipmentName = it; errorMessage = "" }, label = { Text("Gerät") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                                        Button(onClick = { if (equipmentName.isBlank()) errorMessage = "Gerät fehlt!" else step = 2 }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(55.dp), shape = MaterialTheme.shapes.medium) { Text("WEITER") }
                                    }
                                }
                                2 -> {
                                    Column {
                                        OutlinedTextField(value = muscleGroup, onValueChange = { muscleGroup = it; errorMessage = "" }, label = { Text("Muskel") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                                        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = { step = 1 }, modifier = Modifier.weight(1f).height(55.dp), shape = MaterialTheme.shapes.medium) { Text("ZURÜCK") }
                                            Button(onClick = { if (muscleGroup.isBlank()) errorMessage = "Muskel fehlt!" else step = 3 }, modifier = Modifier.weight(1f).height(55.dp), shape = MaterialTheme.shapes.medium) { Text("WEITER") }
                                        }
                                    }
                                }
                                3 -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                        if (imageUri != null) {
                                            AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.size(120.dp).clip(MaterialTheme.shapes.medium).clickable { fullscreenImageUri = imageUri.toString() }, contentScale = ContentScale.Crop)
                                        } else {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, shape = MaterialTheme.shapes.medium) { Text("GALERIE") }
                                                OutlinedButton(onClick = { val uri = context.createTempPictureUri(); tempCameraUri = uri; cameraLauncher.launch(uri) }, shape = MaterialTheme.shapes.medium) { Text("KAMERA") }
                                            }
                                        }
                                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = { step = 2 }, modifier = Modifier.weight(1f).height(55.dp), shape = MaterialTheme.shapes.medium) { Text("ZURÜCK") }
                                            Button(
                                                onClick = {
                                                    keyboardController?.hide()
                                                    val permanentPath = imageUri?.let { saveImageToInternalStorage(it) }
                                                    viewModel.saveEquipment(equipmentName, muscleGroup, permanentPath)
                                                    equipmentName = ""; muscleGroup = ""; imageUriString = null; step = 1
                                                },
                                                modifier = Modifier.weight(1f).height(55.dp), shape = MaterialTheme.shapes.medium
                                            ) { Text("SPEICHERN") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            items(equipmentList) { equipment ->
                var showMenu by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { selectedEquipmentId = equipment.id },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (equipment.imageUri != null) {
                            AsyncImage(model = equipment.imageUri, contentDescription = null, modifier = Modifier.size(65.dp).clip(MaterialTheme.shapes.medium).clickable { fullscreenImageUri = equipment.imageUri }, contentScale = ContentScale.Crop)
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = equipment.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = equipment.muscleGroup.uppercase(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            if (equipment.latestWeight != null) { Text(text = "Zuletzt: ${equipment.latestSets}×${equipment.latestReps} (${equipment.latestWeight} kg)", style = MaterialTheme.typography.labelMedium) }
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Bearbeiten") },
                                    onClick = { showMenu = false; eqToEditId = equipment.id; editName = equipment.name; editMuscle = equipment.muscleGroup; editImageUriString = equipment.imageUri }
                                )
                                // NEU: Lösch-ID setzen!
                                DropdownMenuItem(
                                    text = { Text("Löschen", color = MaterialTheme.colorScheme.error) },
                                    onClick = { showMenu = false; eqToDeleteId = equipment.id }
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(120.dp)) }
        }

        // NEU: Dialog für Bearbeitung
        if (eqToEdit != null) {
            AlertDialog(
                onDismissRequest = { eqToEditId = null },
                title = { Text("GERÄT ANPASSEN", fontWeight = FontWeight.Black) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Gerät") }, shape = MaterialTheme.shapes.medium, singleLine = true)
                        OutlinedTextField(value = editMuscle, onValueChange = { editMuscle = it }, label = { Text("Muskel") }, shape = MaterialTheme.shapes.medium, singleLine = true)
                        if (editImageUri != null) {
                            AsyncImage(model = editImageUri, contentDescription = null, modifier = Modifier.size(100.dp).clip(MaterialTheme.shapes.medium).align(Alignment.CenterHorizontally).clickable { fullscreenImageUri = editImageUri.toString() }, contentScale = ContentScale.Crop)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { editGalleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium) { Text("GALERIE") }
                            OutlinedButton(onClick = { val uri = context.createTempPictureUri(); editTempCameraUri = uri; editCameraLauncher.launch(uri) }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium) { Text("KAMERA") }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        keyboardController?.hide()
                        val finalPath = if (editImageUri?.scheme == "content") saveImageToInternalStorage(editImageUri) else editImageUriString
                        viewModel.updateEquipmentDetails(Equipment(eqToEdit.id, eqToEdit.name, eqToEdit.muscleGroup, eqToEdit.imageUri), editName, editMuscle, finalPath)
                        eqToEditId = null
                    }, shape = MaterialTheme.shapes.medium) { Text("SPEICHERN") }
                },
                dismissButton = { TextButton(onClick = { eqToEditId = null }) { Text("ABBRECHEN") } }
            )
        }

        // NEU: Dialog für Löschung
        if (eqToDelete != null) {
            AlertDialog(
                onDismissRequest = { eqToDeleteId = null },
                title = { Text("GERÄT LÖSCHEN?", fontWeight = FontWeight.Black) },
                text = { Text("Möchtest du das Gerät '${eqToDelete.name}' wirklich löschen? Alle Aufzeichnungen zu diesem Gerät werden ebenfalls entfernt.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteEquipment(Equipment(eqToDelete.id, eqToDelete.name, eqToDelete.muscleGroup, eqToDelete.imageUri))
                            eqToDeleteId = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("LÖSCHEN") }
                },
                dismissButton = { TextButton(onClick = { eqToDeleteId = null }) { Text("ABBRECHEN") } }
            )
        }
    }
}