package com.example.gymapplication.gymUI.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymapplication.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GhostValue(val weight: Float, val reps: Int)

class GymViewModel(internal val dao: GymDao) : ViewModel() {

    val equipmentList = dao.getAllEquipment()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fullHistory = dao.getFullHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class GlobalStats(
        val maxStrengthScore: Float,
        val volume30d: Float,
        val frequency30d: Int,
        val progressionScore: Float
    )

    data class PRItem(
        val equipmentName: String,
        val maxWeight: Float,
        val repsAtMaxWeight: Int,
        val dateOfMaxWeight: Long,
        val theoretical1RM: Float
    )

    val personalRecords = fullHistory.map { historyItems ->
        historyItems.groupBy { it.equipmentName }.mapNotNull { (name, logs) ->
            val maxLog = logs.maxByOrNull { it.weight } ?: return@mapNotNull null
            val max1RM = logs.maxOfOrNull { it.weight * (1f + it.reps / 30f) } ?: 0f

            PRItem(
                equipmentName = name,
                maxWeight = maxLog.weight,
                repsAtMaxWeight = maxLog.reps,
                dateOfMaxWeight = maxLog.dateMillis,
                theoretical1RM = max1RM
            )
        }.sortedByDescending { it.maxWeight }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val equipmentWithLatestLogs = dao.getEquipmentWithLatestLog()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workoutPlans = dao.getAllWorkoutPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val plannedWorkouts = dao.getAllPlannedWorkouts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val detailedMuscleStats = dao.getDetailedMuscleStats(
        System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyVolumeStats = dao.getDailyVolumeStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val finishedSessions = dao.getAllFinishedSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bodyTargets = dao.getAllBodyTargets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendsList = dao.getAllFriends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myGlobalStats = combine(dao.getAllFinishedSessions(), personalRecords) { _, prs ->
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
        val sixtyDaysAgo = now - (60L * 24 * 60 * 60 * 1000)

        val freq30 = dao.getWorkoutsCountDirect(thirtyDaysAgo)
        val vol30 = dao.getTotalVolumeDirect(thirtyDaysAgo) ?: 0f
        val volPreviousMonth = dao.getVolumeBetweenDirect(sixtyDaysAgo, thirtyDaysAgo) ?: 0f
        val progression =
            if (volPreviousMonth > 0) ((vol30 - volPreviousMonth) / volPreviousMonth) * 100 else 0f
        val top3Strength = prs.take(3).sumOf { it.maxWeight.toDouble() }.toFloat()

        GlobalStats(top3Strength, vol30, freq30, progression)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GlobalStats(0f, 0f, 0, 0f))

    internal val _activeSession = MutableStateFlow<WorkoutSession?>(null)
    val activeSession = _activeSession.asStateFlow()

    internal val _currentRestTime = MutableStateFlow(120)
    val currentRestTime = _currentRestTime.asStateFlow()

    internal val _workoutDuration = MutableStateFlow(0L)
    val workoutDuration = _workoutDuration.asStateFlow()

    internal val _isResting = MutableStateFlow(false)
    val isResting = _isResting.asStateFlow()

    internal val _restSecondsLeft = MutableStateFlow(0)
    val restSecondsLeft = _restSecondsLeft.asStateFlow()

    internal val _currentExerciseIndex = MutableStateFlow(0)
    val currentExerciseIndex = _currentExerciseIndex.asStateFlow()

    internal val _activeSessionNotes =
        MutableStateFlow<Map<Int, Pair<String?, String?>>>(emptyMap())
    val activeSessionNotes = _activeSessionNotes.asStateFlow()

    internal val _triggerSummaryEvent = MutableStateFlow(false)
    val triggerSummaryEvent = _triggerSummaryEvent.asStateFlow()

    internal val _weightGoal = MutableStateFlow("Abnehmen")
    val weightGoal = _weightGoal.asStateFlow()

    internal var timerJob: Job? = null
    internal var restTimerJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val ongoingSession = dao.getActiveWorkoutSession()
            if (ongoingSession != null) {
                _activeSession.value = ongoingSession
                startTimer(ongoingSession)

                if (ongoingSession.restEndTimeMillis != null) {
                    _isResting.value = true
                    startRestTimerLoop(null)
                }

                val sessionLogs = dao.getLogsForSessionDirect(ongoingSession.sessionId)
                val noteMap = mutableMapOf<Int, Pair<String?, String?>>()
                sessionLogs.forEach { log ->
                    if (!log.sessionNote.isNullOrBlank() || !log.sessionNoteImageUris.isNullOrBlank()) {
                        noteMap[log.equipmentId] = Pair(log.sessionNote, log.sessionNoteImageUris)
                    }
                }
                _activeSessionNotes.value = noteMap
            }
        }
    }
}

class GymViewModelFactory(private val dao: GymDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GymViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GymViewModel(dao) as T
        }
        throw IllegalArgumentException("Unbekanntes ViewModel")
    }
}