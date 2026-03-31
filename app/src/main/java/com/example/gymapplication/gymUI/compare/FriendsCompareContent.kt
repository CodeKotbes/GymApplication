package com.example.gymapplication.gymUI.compare

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.gymapplication.data.Friend
import com.example.gymapplication.gymUI.analysis.GraphDataPoint
import com.example.gymapplication.gymUI.viewmodel.GymViewModel
import com.example.gymapplication.gymUI.viewmodel.deleteFriend
import com.example.gymapplication.gymUI.viewmodel.generateFullExportJson
import com.example.gymapplication.gymUI.viewmodel.importFriendFromFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun FriendsCompareContent(viewModel: GymViewModel, onFriendClick: (Friend) -> Unit) {
    val friends by viewModel.friendsList.collectAsState()
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }
    var myNameInput by remember { mutableStateOf("") }
    var friendToDelete by remember { mutableStateOf<Friend?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yy - HH:mm", Locale.getDefault()) }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) viewModel.importFriendFromFile(context, uri)
        }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "DEINE FREUNDE",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { importLauncher.launch("application/json") }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Freund hinzufügen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (friends.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.5f
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Compare,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Noch keine Freunde hinzugefügt.", fontWeight = FontWeight.Bold)
                            Text(
                                "Importiere die JSON-Datei eines Freundes, um eure Werte zu vergleichen!",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(friends) { friend ->
                    val isOutdated =
                        (System.currentTimeMillis() - friend.lastSyncMillis) > (14L * 24 * 60 * 60 * 1000)
                    var showMenu by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFriendClick(friend) },
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    friend.name.uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isOutdated) Color(0xFFF59E0B) else Color(
                                                    0xFF10B981
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Sync: ${dateFormat.format(Date(friend.lastSyncMillis))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Optionen")
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
                                        onClick = {
                                            showMenu = false
                                            friendToDelete = friend
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = { showExportDialog = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .fillMaxWidth()
                .height(55.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Text("MEINE DATEN TEILEN", fontWeight = FontWeight.Black)
        }
    }

    if (friendToDelete != null) {
        AlertDialog(
            onDismissRequest = { friendToDelete = null },
            title = { Text("EINTRAG LÖSCHEN?", fontWeight = FontWeight.Black) },
            text = { Text("Möchtest du diesen Synchronisations-Eintrag wirklich löschen?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFriend(friendToDelete!!)
                        friendToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("LÖSCHEN") }
            },
            dismissButton = {
                TextButton(onClick = { friendToDelete = null }) { Text("ABBRECHEN") }
            }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("DATEN EXPORTIEREN", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text(
                        "Gib deinen Namen ein, unter dem dein Freund dich sehen soll.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = myNameInput,
                        onValueChange = { myNameInput = it },
                        label = { Text("Dein Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val nameToUse = myNameInput.ifBlank { "Gym Bro" }
                    context.getSharedPreferences("gym_settings", Context.MODE_PRIVATE).edit()
                        .putString("my_name", nameToUse).apply()

                    viewModel.generateFullExportJson(context, nameToUse) { jsonString ->
                        try {
                            val file = File(
                                context.cacheDir,
                                "gym_export_${System.currentTimeMillis()}.json"
                            )
                            file.writeText(jsonString)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    "Daten teilen mit..."
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    showExportDialog = false
                }) { Text("TEILEN") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportDialog = false
                }) { Text("ABBRECHEN") }
            }
        )
    }
}

data class ParsedFriendExercise(
    val name: String,
    val muscle: String,
    val prWeight: Float,
    val prReps: Int,
    val history: List<GraphDataPoint> = emptyList()
)

@Composable
fun FriendGlobalComparisonHeader(
    myName: String,
    myStats: GymViewModel.GlobalStats,
    friendName: String,
    friendStats: Map<String, Float>
) {
    val myColor = MaterialTheme.colorScheme.primary
    val friendColor = Color(0xFFF97316)

    @Composable
    fun CompareBar(
        title: String,
        myValue: Float,
        friendValue: Float,
        unit: String,
        formatAsInt: Boolean = false,
        isPercentage: Boolean = false
    ) {
        val myAbs = if (isPercentage) kotlin.math.abs(myValue) else myValue
        val friendAbs = if (isPercentage) kotlin.math.abs(friendValue) else friendValue
        val totalAbs = if (myAbs + friendAbs > 0f) myAbs + friendAbs else 1f
        val myFraction = (myAbs / totalAbs).coerceIn(0.05f, 0.95f)
        val animatedFraction by animateFloatAsState(
            targetValue = myFraction,
            animationSpec = tween(1200),
            label = "barAnim_$title"
        )
        val myDisplayValue = if (formatAsInt) myValue.roundToInt().toString() else String.format(
            Locale.getDefault(),
            "%.1f",
            myValue
        )
        val friendDisplayValue = if (formatAsInt) friendValue.roundToInt()
            .toString() else String.format(Locale.getDefault(), "%.1f", friendValue)
        val myPrefix = if (isPercentage && myValue > 0) "+" else ""
        val friendPrefix = if (isPercentage && friendValue > 0) "+" else ""

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$myPrefix$myDisplayValue $unit",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = myColor
                )
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$friendPrefix$friendDisplayValue $unit",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = friendColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(animatedFraction)
                        .background(myColor)
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f - animatedFraction)
                        .background(friendColor)
                )
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = myName.uppercase(),
                    color = myColor,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "VS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = friendName.uppercase(),
                    color = friendColor,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            CompareBar(
                "Max-Kraft Score",
                myStats.maxStrengthScore,
                friendStats["maxS"] ?: 0f,
                "kg",
                true
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            CompareBar("Volumen (30 Tage)", myStats.volume30d, friendStats["vol"] ?: 0f, "kg", true)
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            CompareBar(
                "Workouts (30 Tage)",
                myStats.frequency30d.toFloat(),
                friendStats["freq"] ?: 0f,
                "x",
                true
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            CompareBar(
                "Progression",
                myStats.progressionScore,
                friendStats["prog"] ?: 0f,
                "%",
                false,
                true
            )
        }
    }
}
