package com.example.gymapplication.gymUI.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymapplication.gymUI.GymViewModel
import com.example.gymapplication.gymUI.analysis.GraphDataPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2

@Composable
fun FriendExerciseDetailScreen(
    friendEx: ParsedFriendExercise,
    myEqId: Int,
    myName: String,
    friendName: String,
    viewModel: GymViewModel,
    onBack: () -> Unit
) {
    val myLogs by viewModel.getLogsFlow(myEqId).collectAsState(initial = emptyList())
    var showFullscreenGraph by remember { mutableStateOf(false) }

    val myGraphData = remember(myLogs) {
        myLogs.groupBy {
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            ).format(Date(it.dateMillis))
        }
            .mapNotNull { (_, dayLogs) ->
                dayLogs.maxByOrNull { it.weight }?.let {
                    GraphDataPoint(
                        it.weight,
                        it.dateMillis
                    )
                }
            }
            .sortedBy { it.dateMillis }
    }

    val tableData = remember(myGraphData, friendEx.history) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())

        val allDates = (myGraphData.map {
            try {
                dateFormat.format(Date(it.dateMillis))
            } catch (e: Exception) {
                ""
            }
        } +
                friendEx.history.map {
                    try {
                        dateFormat.format(Date(it.dateMillis))
                    } catch (e: Exception) {
                        ""
                    }
                })
            .filter { it.isNotBlank() }
            .distinct()
            .sortedDescending()

        allDates.map { dateStr ->
            val myVal = myGraphData.find {
                try {
                    dateFormat.format(Date(it.dateMillis)) == dateStr
                } catch (e: Exception) {
                    false
                }
            }?.value
            val friendVal = friendEx.history.find {
                try {
                    dateFormat.format(Date(it.dateMillis)) == dateStr
                } catch (e: Exception) {
                    false
                }
            }?.value
            val displayDate = try {
                displayFormat.format(dateFormat.parse(dateStr) ?: Date())
            } catch (e: Exception) {
                dateStr
            }
            Triple(displayDate, myVal, friendVal)
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Zurück"
                )
            }
            Text(
                friendEx.name.uppercase(),
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
                Text(
                    "VERLAUF",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFullscreenGraph = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CombinedCompareGraph(
                            myData = myGraphData,
                            friendData = friendEx.history,
                            myName = myName,
                            friendName = friendName,
                            unit = "kg",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            isFullView = false
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "ALLE EINTRÄGE",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Datum",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        myName.uppercase(),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        friendName.uppercase(),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFF97316)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }

            items(tableData) { (dateStr, myWeight, friendWeight) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            dateStr,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        val myStr = if (myWeight != null) {
                            if (myWeight % 1.0f == 0.0f) "${myWeight.toInt()} kg" else "${
                                String.format(
                                    Locale.getDefault(),
                                    "%.1f",
                                    myWeight
                                )
                            } kg"
                        } else "-"
                        Text(
                            myStr,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )

                        val friendStr = if (friendWeight != null) {
                            if (friendWeight % 1.0f == 0.0f) "${friendWeight.toInt()} kg" else "${
                                String.format(
                                    Locale.getDefault(),
                                    "%.1f",
                                    friendWeight
                                )
                            } kg"
                        } else "-"
                        Text(
                            friendStr,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                            color = Color(0xFFF97316),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showFullscreenGraph) {
        Dialog(
            onDismissRequest = { showFullscreenGraph = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = true
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "VERLAUF: ${friendEx.name.uppercase()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = {
                            showFullscreenGraph = false
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
                            friendData = friendEx.history,
                            myName = myName,
                            friendName = friendName,
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