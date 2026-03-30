package com.example.gymapplication.gymUI.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymapplication.gymUI.GymViewModel
import com.example.gymapplication.gymUI.compare.FriendDuelScreen
import com.example.gymapplication.gymUI.compare.FriendsCompareContent
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(viewModel: GymViewModel) {
    val tabs = listOf(
        "Körper" to Icons.Default.Person,
        "Gewichte" to Icons.Default.FitnessCenter,
        "Rekorde" to Icons.Default.Star,
        "Analyse" to Icons.Default.ShowChart,
        "Vergleich" to Icons.Default.Compare
    )

    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    var selectedFriendId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedEquipmentId by rememberSaveable { mutableStateOf<Int?>(null) }
    val equipmentList by viewModel.equipmentList.collectAsState(initial = emptyList())
    val selectedEquipment = equipmentList.find { it.id == selectedEquipmentId }
    var selectedBodyType by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedBodyUnit by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedEfficiencyEqId by rememberSaveable { mutableStateOf<Int?>(null) }
    var expandedMuscleGroups by rememberSaveable { mutableStateOf(emptyList<String>()) }

    if (selectedEquipment != null) {
        BackHandler { selectedEquipmentId = null }
        HistoryDetailScreen(
            equipment = selectedEquipment,
            viewModel = viewModel,
            onBack = { selectedEquipmentId = null })
        return
    }

    if (selectedFriendId != null) {
        val friends by viewModel.friendsList.collectAsState(initial = emptyList())
        val selectedFriend = friends.find { it.userId == selectedFriendId }
        if (selectedFriend != null) {
            BackHandler { selectedFriendId = null }
            FriendDuelScreen(
                friend = selectedFriend,
                viewModel = viewModel,
                onBack = { selectedFriendId = null })
            return
        }
    }

    if (selectedBodyType != null && selectedBodyUnit != null) {
        BackHandler { selectedBodyType = null; selectedBodyUnit = null }
        BodyDetailScreen(
            type = selectedBodyType!!,
            unit = selectedBodyUnit!!,
            viewModel = viewModel,
            onBack = { selectedBodyType = null; selectedBodyUnit = null })
        return
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text(
            "MEIN FORTSCHRITT",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(16.dp))

        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {},
            indicator = { tabPositions ->
                if (tabPositions.isNotEmpty()) {
                    val currentTab = tabPositions[pagerState.currentPage]
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(currentTab),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, (title, icon) ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    icon = {
                        Icon(
                            icon,
                            contentDescription = title,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    text = {
                        Text(
                            title.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> BodyProgressContent(viewModel) { type, unit ->
                    selectedBodyType = type; selectedBodyUnit = unit
                }

                1 -> EquipmentProgressContent(viewModel) { selectedEquipmentId = it.id }
                2 -> PersonalRecordsContent(viewModel)
                3 -> AnalysisContent(
                    viewModel,
                    selectedEfficiencyEqId,
                    { selectedEfficiencyEqId = it },
                    expandedMuscleGroups,
                    { muscle ->
                        expandedMuscleGroups =
                            if (expandedMuscleGroups.contains(muscle)) expandedMuscleGroups - muscle else expandedMuscleGroups + muscle
                    })

                4 -> FriendsCompareContent(
                    viewModel,
                    onFriendClick = { selectedFriendId = it.userId })
            }
        }
    }
}