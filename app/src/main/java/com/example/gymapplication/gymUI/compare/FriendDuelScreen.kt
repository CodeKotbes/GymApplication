package com.example.gymapplication.gymUI.compare

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymapplication.data.Friend
import com.example.gymapplication.gymUI.analysis.GraphDataPoint
import com.example.gymapplication.gymUI.analysis.PremiumDonutChart
import com.example.gymapplication.gymUI.viewmodel.GymViewModel
import com.example.gymapplication.gymUI.viewmodel.deleteFriendMapping
import com.example.gymapplication.gymUI.viewmodel.getFriendMappingsFlow
import com.example.gymapplication.gymUI.viewmodel.saveFriendMapping
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun FriendDuelScreen(friend: Friend, viewModel: GymViewModel, onBack: () -> Unit) {
    var selectedCompareExercise by remember { mutableStateOf<Pair<ParsedFriendExercise, Int>?>(null) }

    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("gym_settings", Context.MODE_PRIVATE)
    val myName = sharedPrefs.getString("my_name", "DU") ?: "DU"

    if (selectedCompareExercise != null) {
        BackHandler { selectedCompareExercise = null }
        val fEx = selectedCompareExercise!!.first
        val myEqId = selectedCompareExercise!!.second

        FriendExerciseDetailScreen(
            friendEx = fEx,
            myEqId = myEqId,
            myName = myName,
            friendName = friend.name,
            viewModel = viewModel,
            onBack = { selectedCompareExercise = null }
        )
        return
    }

    val myPRs by viewModel.personalRecords.collectAsState(initial = emptyList())
    val myEquipment by viewModel.equipmentList.collectAsState(initial = emptyList())
    val baseUserId = friend.userId.substringBefore("_")
    val mappings by viewModel.getFriendMappingsFlow(baseUserId)
        .collectAsState(initial = emptyList())

    val myStats by viewModel.myGlobalStats.collectAsState()
    val myVolumeStats by viewModel.dailyVolumeStats.collectAsState(initial = emptyList())
    val myDetailedStats by viewModel.detailedMuscleStats.collectAsState(initial = emptyList())

    val friendGlobalStats = remember(friend.snapshotJson) {
        try {
            val root = JSONObject(friend.snapshotJson)
            val statsObj = root.optJSONObject("stats")
            if (statsObj != null) {
                mapOf(
                    "maxS" to statsObj.optDouble("maxS", 0.0).toFloat(),
                    "vol" to statsObj.optDouble("vol", 0.0).toFloat(),
                    "freq" to statsObj.optInt("freq", 0).toFloat(),
                    "prog" to statsObj.optDouble("prog", 0.0).toFloat()
                )
            } else mapOf("maxS" to 0f, "vol" to 0f, "freq" to 0f, "prog" to 0f)
        } catch (e: Exception) {
            mapOf("maxS" to 0f, "vol" to 0f, "freq" to 0f, "prog" to 0f)
        }
    }

    val friendVolumeHistory = remember(friend.snapshotJson) {
        val list = mutableListOf<GraphDataPoint>()
        try {
            val root = JSONObject(friend.snapshotJson)
            val historyArray = root.optJSONArray("volHistory")
            if (historyArray != null) {
                for (i in 0 until historyArray.length()) {
                    val obj = historyArray.getJSONObject(i)
                    list.add(GraphDataPoint(obj.getDouble("v").toFloat(), obj.getLong("d")))
                }
            }
        } catch (e: Exception) {
        }
        list.sortedBy { it.dateMillis }
    }

    val friendDetailedStats = remember(friend.snapshotJson) {
        val list = mutableListOf<com.example.gymapplication.data.DetailedMuscleStat>()
        try {
            val root = JSONObject(friend.snapshotJson)
            val mArray = root.optJSONArray("muscleStatsArr")
            if (mArray != null) {
                for (i in 0 until mArray.length()) {
                    val obj = mArray.getJSONObject(i)
                    list.add(
                        com.example.gymapplication.data.DetailedMuscleStat(
                            obj.getString("m"),
                            obj.getString("e"),
                            obj.getInt("s")
                        )
                    )
                }
            } else {
                val mObj = root.optJSONObject("muscleStats")
                if (mObj != null) {
                    val iter = mObj.keys()
                    while (iter.hasNext()) {
                        val key = iter.next()
                        list.add(
                            com.example.gymapplication.data.DetailedMuscleStat(
                                key,
                                "Alle Übungen",
                                mObj.getInt(key)
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
        }
        list
    }

    val friendExercises = remember(friend.snapshotJson) {
        val list = mutableListOf<ParsedFriendExercise>()
        try {
            val root = JSONObject(friend.snapshotJson)
            val dataArray = root.getJSONArray("data")
            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                val histList = mutableListOf<GraphDataPoint>()
                val histArray = item.optJSONArray("h")
                if (histArray != null) {
                    for (j in 0 until histArray.length()) {
                        val hObj = histArray.getJSONObject(j)
                        histList.add(
                            GraphDataPoint(
                                hObj.getDouble("w").toFloat(),
                                hObj.getLong("d")
                            )
                        )
                    }
                }
                list.add(
                    ParsedFriendExercise(
                        item.getString("n"),
                        item.getString("m"),
                        item.getDouble("prW").toFloat(),
                        item.getInt("prR"),
                        histList.sortedBy { it.dateMillis })
                )
            }
        } catch (e: Exception) {
        }
        list
    }

    var exerciseToMap by remember { mutableStateOf<ParsedFriendExercise?>(null) }
    var showVolumeFullscreen by remember { mutableStateOf(false) }

    var balanceTabIsMe by remember { mutableStateOf(true) }
    var expandedMuscleGroups by remember { mutableStateOf(emptyList<String>()) }

    val pieColors = listOf(
        Color(0xFF6366F1), Color(0xFF10B981), Color(0xFFF59E0B),
        Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF06B6D4),
        Color(0xFFF43F5E), Color(0xFF14B8A6), Color(0xFFF97316)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Zurück"
                )
            }
            Text(
                "VERGLEICH: ${friend.name.uppercase()}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FriendGlobalComparisonHeader(
                    myName = myName,
                    myStats = myStats,
                    friendName = friend.name,
                    friendStats = friendGlobalStats
                )
            }

            val myGraphData = myVolumeStats.mapNotNull { stat ->
                try {
                    val date =
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(stat.dateStr)
                    val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                    if (date != null && date.time >= thirtyDaysAgo) GraphDataPoint(
                        stat.totalVolume,
                        date.time
                    ) else null
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.dateMillis }

            if (myGraphData.isNotEmpty() || friendVolumeHistory.isNotEmpty()) {
                item {
                    Text(
                        "VOLUMEN VERLAUF (30 TAGE)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showVolumeFullscreen = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            CombinedCompareGraph(
                                myData = myGraphData,
                                friendData = friendVolumeHistory,
                                myName = myName,
                                friendName = friend.name,
                                unit = "kg",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                isFullView = false
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (myDetailedStats.isNotEmpty() || friendDetailedStats.isNotEmpty()) {
                item {
                    Text(
                        "MUSKEL-BALANCE",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { balanceTabIsMe = true }
                                .background(if (balanceTabIsMe) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .padding(12.dp), contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "DEINE BALANCE",
                                color = if (balanceTabIsMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { balanceTabIsMe = false }
                                .background(if (!balanceTabIsMe) Color(0xFFF97316) else Color.Transparent)
                                .padding(12.dp), contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${friend.name.uppercase()}'S BALANCE",
                                color = if (!balanceTabIsMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val activeStats = if (balanceTabIsMe) myDetailedStats else friendDetailedStats
                    val muscleGroupTotals = activeStats.groupBy { it.muscleGroup }
                        .mapValues { entry -> entry.value.sumOf { it.totalSets } }.toList()
                        .sortedByDescending { it.second }
                    val totalSetsOverall = muscleGroupTotals.sumOf { it.second }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (totalSetsOverall > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PremiumDonutChart(
                                        data = muscleGroupTotals.map { it.second.toFloat() },
                                        colors = pieColors,
                                        modifier = Modifier.size(160.dp),
                                        strokeWidth = 45f,
                                        highlightedIndices = muscleGroupTotals.indices.filter {
                                            expandedMuscleGroups.contains(
                                                muscleGroupTotals[it].first
                                            )
                                        },
                                        outlineColor = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))

                                muscleGroupTotals.forEachIndexed { index, (muscle, total) ->
                                    val isExpanded = expandedMuscleGroups.contains(muscle)
                                    val percentage = (total.toFloat() / totalSetsOverall) * 100
                                    val groupColor = pieColors[index % pieColors.size]

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(MaterialTheme.shapes.small)
                                                .clickable {
                                                    expandedMuscleGroups =
                                                        if (isExpanded) expandedMuscleGroups - muscle else expandedMuscleGroups + muscle
                                                }
                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(groupColor)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                muscle.uppercase(),
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                "${percentage.toInt()} %",
                                                fontWeight = FontWeight.Black,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
                                            var selectedExercise by remember {
                                                mutableStateOf<String?>(
                                                    null
                                                )
                                            }
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        start = 12.dp,
                                                        end = 12.dp,
                                                        top = 16.dp,
                                                        bottom = 24.dp
                                                    )
                                                    .background(
                                                        MaterialTheme.colorScheme.surfaceVariant.copy(
                                                            alpha = 0.3f
                                                        ),
                                                        MaterialTheme.shapes.medium
                                                    )
                                                    .padding(16.dp)
                                            ) {
                                                Text(
                                                    "ÜBUNGS-AUFSCHLÜSSELUNG",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))

                                                val exercises =
                                                    activeStats.filter { it.muscleGroup == muscle }
                                                        .sortedByDescending { it.totalSets }
                                                val highlightedExIndices =
                                                    exercises.indices.filter { exercises[it].equipmentName == selectedExercise }

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(120.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    PremiumDonutChart(
                                                        data = exercises.map { it.totalSets.toFloat() },
                                                        colors = exercises.indices.map { exIndex -> pieColors[(index + exIndex + 2) % pieColors.size] },
                                                        modifier = Modifier.size(100.dp),
                                                        strokeWidth = 30f,
                                                        highlightedIndices = highlightedExIndices,
                                                        outlineColor = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(16.dp))
                                                exercises.forEachIndexed { exIndex, ex ->
                                                    val exPercentage =
                                                        (ex.totalSets.toFloat() / total) * 100
                                                    val exColor =
                                                        pieColors[(index + exIndex + 2) % pieColors.size]
                                                    val isExSelected =
                                                        selectedExercise == ex.equipmentName

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(MaterialTheme.shapes.small)
                                                            .clickable {
                                                                selectedExercise =
                                                                    if (isExSelected) null else ex.equipmentName
                                                            }
                                                            .padding(
                                                                vertical = 8.dp,
                                                                horizontal = 4.dp
                                                            ),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(8.dp)
                                                                    .clip(CircleShape)
                                                                    .background(exColor)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                ex.equipmentName,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = if (isExSelected) FontWeight.Black else FontWeight.Bold,
                                                                color = if (isExSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                        Text(
                                                            "${exPercentage.toInt()} %",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = if (isExSelected) FontWeight.Black else FontWeight.Bold,
                                                            color = if (isExSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (index < muscleGroupTotals.size - 1) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = 0.05f
                                            )
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    "Keine Daten für die letzten 30 Tage.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Text(
                    text = "EINZELNE ÜBUNGEN",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tippe auf eine verknüpfte Übung für Details & Verlauf",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(friendExercises) { fEx ->
                val mapping = mappings.find { it.friendExerciseName == fEx.name }

                if (mapping != null) {
                    val myEq = myEquipment.find { it.id == mapping.myEquipmentId }
                    val myPr = myPRs.find { it.equipmentName == myEq?.name }
                    val myWeight = myPr?.maxWeight ?: 0f
                    val myReps = myPr?.repsAtMaxWeight ?: 0

                    var showMenu by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { mod ->
                                if (myEq != null) mod.clickable {
                                    selectedCompareExercise = Pair(fEx, myEq.id)
                                } else mod
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    (myEq?.name ?: fEx.name).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.weight(1f)
                                )

                                Box {
                                    IconButton(
                                        onClick = { showMenu = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "Optionen"
                                        )
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
                                                    "Neu verknüpfen",
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Bearbeiten",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                exerciseToMap = fEx
                                            }
                                        )

                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "Verknüpfung aufheben",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.LinkOff,
                                                    contentDescription = "Aufheben",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                viewModel.deleteFriendMapping(baseUserId, fEx.name)
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            val myColor = MaterialTheme.colorScheme.primary
                            val friendColor = Color(0xFFF97316)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "DU",
                                    color = myColor,
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    friend.name.uppercase(),
                                    color = friendColor,
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            val totalWeight = myWeight + fEx.prWeight
                            val myFraction = if (totalWeight > 0f) myWeight / totalWeight else 0.5f
                            val animatedFraction by animateFloatAsState(
                                targetValue = myFraction,
                                animationSpec = tween(1200),
                                label = "bar"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .clip(CircleShape)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(animatedFraction.coerceAtLeast(0.05f))
                                        .background(myColor),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (myWeight > 0) Text(
                                        " ${myWeight.roundToInt()} kg",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.surface)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight((1f - animatedFraction).coerceAtLeast(0.05f))
                                        .background(friendColor),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (fEx.prWeight > 0) Text(
                                        "${fEx.prWeight.roundToInt()} kg ",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    if (myWeight > 0) "${myWeight} kg x $myReps Wdh." else "Keine Daten",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${fEx.prWeight} kg x ${fEx.prReps} Wdh.",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.3f
                            )
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                Text(
                                    fEx.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Muskel: ${fEx.muscle}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { exerciseToMap = fEx },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = CircleShape
                            ) {
                                Text(
                                    "VERKNÜPFEN",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (exerciseToMap != null) {
        AlertDialog(
            onDismissRequest = { exerciseToMap = null },
            title = { Text("ÜBUNG VERKNÜPFEN", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text(
                        "Dein Freund nutzt '${exerciseToMap!!.name}'. Welcher deiner Übungen entspricht das?",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val sortedEquipment =
                        myEquipment.sortedWith(compareByDescending<com.example.gymapplication.data.Equipment> {
                            it.name.equals(
                                exerciseToMap!!.name,
                                ignoreCase = true
                            )
                        }.thenByDescending {
                            it.muscleGroup.equals(
                                exerciseToMap!!.muscle,
                                ignoreCase = true
                            )
                        }.thenBy { it.name })

                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(sortedEquipment) { eq ->
                            val isMatch = eq.name.equals(exerciseToMap!!.name, ignoreCase = true)
                            val isSameMuscle =
                                eq.muscleGroup.equals(exerciseToMap!!.muscle, ignoreCase = true)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.saveFriendMapping(
                                            baseUserId,
                                            exerciseToMap!!.name,
                                            eq.id
                                        )
                                        exerciseToMap = null
                                    },
                                colors = CardDefaults.cardColors(containerColor = if (isMatch) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            eq.name,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMatch) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isMatch) {
                                            Spacer(modifier = Modifier.width(8.dp)); Badge(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ) { Text("MATCH", color = Color.White) }
                                        }
                                    }
                                    Text(
                                        eq.muscleGroup,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSameMuscle && !isMatch) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { exerciseToMap = null }) { Text("ABBRECHEN") } }
        )
    }

    if (showVolumeFullscreen) {
        val myGraphData = myVolumeStats.mapNotNull { stat ->
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(stat.dateStr)
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                if (date != null && date.time >= thirtyDaysAgo) GraphDataPoint(
                    stat.totalVolume,
                    date.time
                ) else null
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.dateMillis }

        Dialog(
            onDismissRequest = { showVolumeFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = true
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "GESAMTVOLUMEN",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = {
                            showVolumeFullscreen = false
                        }) { Icon(Icons.Default.Close, contentDescription = "Schließen") }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = MaterialTheme.shapes.large
                            )
                            .padding(8.dp)
                    ) {
                        CombinedCompareGraph(
                            myData = myGraphData,
                            friendData = friendVolumeHistory,
                            myName = myName,
                            friendName = friend.name,
                            unit = "kg",
                            modifier = Modifier.fillMaxSize(),
                            isFullView = true
                        )
                    }
                    Text(
                        "Nutze zwei Finger zum Zoomen • Tippe auf Punkte für Details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 16.dp, bottom = 8.dp)
                    )
                }
            }
        }
    }
}