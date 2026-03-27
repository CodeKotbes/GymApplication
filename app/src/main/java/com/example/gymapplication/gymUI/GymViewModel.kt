package com.example.gymapplication.gymUI

import android.app.AlarmManager
import android.app.PendingIntent
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymapplication.data.Equipment
import com.example.gymapplication.data.GymDao
import com.example.gymapplication.data.WorkoutLog
import com.example.gymapplication.data.WorkoutSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.gymapplication.data.BodyMetric
import com.example.gymapplication.data.PlanExercise
import com.example.gymapplication.data.PlannedWorkout
import com.example.gymapplication.data.WorkoutPlan
import java.util.Calendar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class PRItem(
    val equipmentName: String,
    val maxWeight: Float,
    val repsAtMaxWeight: Int,
    val dateOfMaxWeight: Long,
    val theoretical1RM: Float
)

class GymViewModel(private val dao: GymDao) : ViewModel() {

    val equipmentList = dao.getAllEquipment()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fullHistory = dao.getFullHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private val _activeSession = MutableStateFlow<WorkoutSession?>(null)
    val activeSession = _activeSession.asStateFlow()

    private val _currentRestTime = MutableStateFlow(120)
    val currentRestTime = _currentRestTime.asStateFlow()

    private val _workoutDuration = MutableStateFlow(0L)
    val workoutDuration = _workoutDuration.asStateFlow()

    private var timerJob: Job? = null

    private val _isResting = MutableStateFlow(false)
    val isResting = _isResting.asStateFlow()

    private val _restSecondsLeft = MutableStateFlow(0)
    val restSecondsLeft = _restSecondsLeft.asStateFlow()

    private var restTimerJob: Job? = null

    private val _currentExerciseIndex = MutableStateFlow(0)
    val currentExerciseIndex = _currentExerciseIndex.asStateFlow()

    private val _activeSessionNotes = MutableStateFlow<Map<Int, Pair<String?, String?>>>(emptyMap())
    val activeSessionNotes = _activeSessionNotes.asStateFlow()

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

    private fun startTimer(session: WorkoutSession) {
        timerJob?.cancel()

        if (session.isPaused) {
            val pauseStart = session.lastPausedTimeMillis ?: System.currentTimeMillis()
            val elapsedMillis =
                pauseStart - session.startTimeMillis - session.accumulatedPauseTimeMillis
            _workoutDuration.value = maxOf(0L, elapsedMillis / 1000L)
        } else {
            timerJob = viewModelScope.launch(Dispatchers.Default) {
                while (true) {
                    val elapsedMillis =
                        System.currentTimeMillis() - session.startTimeMillis - session.accumulatedPauseTimeMillis
                    _workoutDuration.value = maxOf(0L, elapsedMillis / 1000L)
                    delay(1000L)
                }
            }
        }
    }

    private fun triggerVibration(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    private fun startRestTimerLoop(context: Context?) {
        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val session = _activeSession.value
                if (session == null || session.restEndTimeMillis == null) {
                    _isResting.value = false
                    _restSecondsLeft.value = 0
                    break
                }

                if (session.isPaused) {
                    val pausedAt = session.lastPausedTimeMillis ?: System.currentTimeMillis()
                    val remaining = (session.restEndTimeMillis - pausedAt) / 1000L
                    _restSecondsLeft.value = maxOf(0, remaining.toInt())
                    delay(200L)
                    continue
                }

                val remaining = (session.restEndTimeMillis - System.currentTimeMillis()) / 1000L
                if (remaining <= 0) {
                    _restSecondsLeft.value = 0
                    _isResting.value = false

                    context?.let { triggerVibration(it) }

                    withContext(Dispatchers.IO) {
                        val current = _activeSession.value
                        if (current != null) {
                            val updated = current.copy(restEndTimeMillis = null)
                            dao.updateWorkoutSession(updated)
                            _activeSession.value = updated
                        }
                    }
                    break
                }

                _restSecondsLeft.value = remaining.toInt()
                delay(200L)
            }
        }
    }

    fun toggleWorkoutPause(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSession = _activeSession.value ?: return@launch
            val now = System.currentTimeMillis()

            val updatedSession = if (currentSession.isPaused) {
                val pauseDuration = now - (currentSession.lastPausedTimeMillis ?: now)
                val newAccumulated = currentSession.accumulatedPauseTimeMillis + pauseDuration
                val newRestEnd = currentSession.restEndTimeMillis?.let { it + pauseDuration }

                currentSession.copy(
                    isPaused = false,
                    lastPausedTimeMillis = null,
                    accumulatedPauseTimeMillis = newAccumulated,
                    restEndTimeMillis = newRestEnd
                )
            } else {
                currentSession.copy(
                    isPaused = true,
                    lastPausedTimeMillis = now
                )
            }

            dao.updateWorkoutSession(updatedSession)
            _activeSession.value = updatedSession

            startTimer(updatedSession)

            if (updatedSession.restEndTimeMillis != null) {
                startRestTimerLoop(context)
            }

            val intent = Intent(context, WorkoutService::class.java).apply {
                putExtra("WORKOUT_NAME", updatedSession.name)
                putExtra("START_TIME", updatedSession.startTimeMillis)
                putExtra("ACCUMULATED_PAUSE", updatedSession.accumulatedPauseTimeMillis)
                putExtra("IS_PAUSED", updatedSession.isPaused)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun startWorkout(context: Context, planId: Int?, name: String, restTime: Int) {
        _currentRestTime.value = restTime
        _currentExerciseIndex.value = 0
        _activeSessionNotes.value = emptyMap()
        skipRest()

        viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val newSession = WorkoutSession(
                startTimeMillis = startTime,
                planId = planId,
                name = name
            )
            val sessionId = dao.insertWorkoutSession(newSession).toInt()

            val sessionWithId = newSession.copy(sessionId = sessionId)
            _activeSession.value = sessionWithId

            startTimer(sessionWithId)

            val intent = Intent(context, WorkoutService::class.java).apply {
                putExtra("WORKOUT_NAME", name)
                putExtra("START_TIME", startTime)
                putExtra("ACCUMULATED_PAUSE", 0L)
                putExtra("IS_PAUSED", false)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun finishWorkout(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSession = _activeSession.value

            if (currentSession != null) {
                val finishedSession = currentSession.copy(
                    endTimeMillis = System.currentTimeMillis(),
                    durationInSeconds = _workoutDuration.value.toInt()
                )
                dao.updateWorkoutSession(finishedSession)

                val planId = currentSession.planId
                if (planId != null) {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val startOfDay = cal.timeInMillis
                    val endOfDay = startOfDay + (24 * 60 * 60 * 1000)

                    dao.deletePlannedWorkoutForToday(planId, startOfDay, endOfDay)
                }
            }

            _activeSession.value = null
            timerJob?.cancel()
            _workoutDuration.value = 0L
            _currentExerciseIndex.value = 0
            _activeSessionNotes.value = emptyMap()
            skipRest()

            val intent = Intent(context, WorkoutService::class.java).apply {
                action = "STOP_WORKOUT"
            }
            context.startService(intent)
        }
    }

    val plannedWorkouts = dao.getAllPlannedWorkouts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val detailedMuscleStats = dao.getDetailedMuscleStats(
        System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyVolumeStats = dao.getDailyVolumeStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun scheduleWorkout(planId: Int, planName: String, dateMillis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertPlannedWorkout(
                PlannedWorkout(
                    planId = planId,
                    planName = planName,
                    dateMillis = dateMillis
                )
            )
        }
    }

    fun scheduleBackup(context: Context, enabled: Boolean, frequency: String) {
        val workManager = androidx.work.WorkManager.getInstance(context)

        if (!enabled) {
            workManager.cancelUniqueWork("auto_backup")
            return
        }

        val interval = if (frequency == "Täglich") 1L else 7L

        val backupRequest = androidx.work.PeriodicWorkRequestBuilder<BackupWorker>(
            interval, java.util.concurrent.TimeUnit.DAYS
        ).setConstraints(
            androidx.work.Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .build()
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "auto_backup",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            backupRequest
        )
    }

    private val _triggerSummaryEvent = MutableStateFlow(false)
    val triggerSummaryEvent = _triggerSummaryEvent.asStateFlow()

    fun triggerWorkoutSummary() {
        _triggerSummaryEvent.value = true
    }

    fun consumeSummaryEvent() {
        _triggerSummaryEvent.value = false
    }

    fun updateBackupSchedule(context: Context) {
        val sharedPrefs = context.getSharedPreferences("gym_settings", Context.MODE_PRIVATE)
        val enabled = sharedPrefs.getBoolean("auto_backup_enabled", false)
        val frequency = sharedPrefs.getString("auto_backup_frequency", "Täglich")
        val folderUri = sharedPrefs.getString("auto_backup_folder_uri", null)
        val hour = sharedPrefs.getInt("auto_backup_hour", 2) // Standard 02:00 Uhr
        val minute = sharedPrefs.getInt("auto_backup_minute", 0)

        val workManager = androidx.work.WorkManager.getInstance(context)

        if (!enabled || folderUri == null) {
            workManager.cancelUniqueWork("gym_auto_backup")
            return
        }

        val calendar = Calendar.getInstance()
        val nowMillis = calendar.timeInMillis
        val targetCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (targetCalendar.timeInMillis <= nowMillis) {
            targetCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = targetCalendar.timeInMillis - nowMillis
        val interval = if (frequency == "Täglich") 1L else 7L

        val request = androidx.work.PeriodicWorkRequestBuilder<BackupWorker>(
            interval, java.util.concurrent.TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "gym_auto_backup",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleWorkout(context: Context, planId: Int, planName: String, dateMillis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertPlannedWorkout(
                PlannedWorkout(
                    planId = planId,
                    planName = planName,
                    dateMillis = dateMillis
                )
            )
            setWorkoutAlarm(context, planName, dateMillis)
        }
    }

    private fun setWorkoutAlarm(context: Context, planName: String, dateMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, WorkoutAlarmReceiver::class.java).apply {
            putExtra("PLAN_NAME", planName)
        }

        val requestCode = (dateMillis / 1000).toInt()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun deletePlannedWorkout(plannedWorkout: PlannedWorkout) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deletePlannedWorkout(plannedWorkout)
        }
    }

    fun deletePlannedWorkout(context: Context, planned: PlannedWorkout) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deletePlannedWorkout(planned)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WorkoutAlarmReceiver::class.java)

            val requestCode = (planned.dateMillis / 1000).toInt()

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )

            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }

    fun deleteWorkoutSession(session: WorkoutSession) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteLogsForSession(session.sessionId)
            dao.deleteWorkoutSession(session)
        }
    }

    val finishedSessions = dao.getAllFinishedSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getLogsForSessionFlow(sessionId: Int) = dao.getLogsForSession(sessionId)

    fun updateWorkoutSessionDate(session: WorkoutSession, newDateMillis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val timeOffset = newDateMillis - session.startTimeMillis

            val updatedSession = session.copy(
                startTimeMillis = newDateMillis,
                endTimeMillis = session.endTimeMillis?.plus(timeOffset)
            )
            dao.updateWorkoutSession(updatedSession)

            val logs = dao.getLogsForSessionDirect(session.sessionId)
            logs.forEach { log ->
                val updatedLog = log.copy(dateMillis = log.dateMillis + timeOffset)
                dao.updateLog(updatedLog)
            }
        }
    }

    fun addBodyMetric(
        type: String,
        value: Float,
        imageUri: String?,
        customDateMillis: Long? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertBodyMetric(
                BodyMetric(
                    type = type,
                    value = value,
                    dateMillis = customDateMillis ?: System.currentTimeMillis(),
                    imageUri = imageUri
                )
            )
        }
    }

    fun getBodyMetrics(type: String) = dao.getMetricsByType(type)

    fun updateBodyMetric(
        metric: BodyMetric,
        newValue: Float,
        imageUri: String?,
        newDateMillis: Long? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateBodyMetric(
                metric.copy(
                    value = newValue,
                    imageUri = imageUri,
                    dateMillis = newDateMillis ?: metric.dateMillis
                )
            )
        }
    }

    fun deleteBodyMetric(metric: BodyMetric) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteBodyMetric(metric)
        }
    }

    fun saveEquipment(name: String, muscleGroup: String, imageUri: String?) {
        viewModelScope.launch {
            val newEquipment = Equipment(
                name = name,
                muscleGroup = muscleGroup,
                imageUri = imageUri
            )
            dao.insertEquipment(newEquipment)
        }
    }

    fun updateEquipmentDetails(
        equipment: Equipment,
        newName: String,
        newMuscle: String,
        newImageUri: String?
    ) {
        viewModelScope.launch {
            val updatedEquipment =
                equipment.copy(name = newName, muscleGroup = newMuscle, imageUri = newImageUri)
            dao.updateEquipment(updatedEquipment)
        }
    }

    fun updateEquipmentNote(equipment: Equipment, note: String?, imageUris: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateEquipment(equipment.copy(generalNote = note, generalNoteImageUris = imageUris))
        }
    }

    fun deleteEquipment(equipment: Equipment) {
        viewModelScope.launch {
            dao.deleteEquipment(equipment)
        }
    }

    fun updateExerciseIndex(newIndex: Int) {
        _currentExerciseIndex.value = newIndex
    }

    fun startRestTimer(context: Context, seconds: Int) {
        val session = _activeSession.value ?: return
        val now = System.currentTimeMillis()
        val endTime = now + (seconds * 1000L)

        viewModelScope.launch(Dispatchers.IO) {
            val updated = session.copy(restEndTimeMillis = endTime)
            dao.updateWorkoutSession(updated)
            _activeSession.value = updated

            _isResting.value = true
            startRestTimerLoop(context)
        }
    }

    fun adjustRestTime(deltaSeconds: Int) {
        val session = _activeSession.value ?: return
        val currentRestEnd = session.restEndTimeMillis ?: return

        val newRestEnd = currentRestEnd + (deltaSeconds * 1000L)
        val compareTime = if (session.isPaused) (session.lastPausedTimeMillis
            ?: System.currentTimeMillis()) else System.currentTimeMillis()

        if (newRestEnd <= compareTime) {
            skipRest()
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val updated = session.copy(restEndTimeMillis = newRestEnd)
                dao.updateWorkoutSession(updated)
                _activeSession.value = updated
            }
        }
    }

    fun skipRest() {
        restTimerJob?.cancel()
        _isResting.value = false
        _restSecondsLeft.value = 0

        viewModelScope.launch(Dispatchers.IO) {
            val session = _activeSession.value
            if (session?.restEndTimeMillis != null) {
                val updated = session.copy(restEndTimeMillis = null)
                dao.updateWorkoutSession(updated)
                _activeSession.value = updated
            }
        }
    }

    fun updateActiveSessionNote(equipmentId: Int, note: String?, imageUris: String?) {
        val currentNotes = _activeSessionNotes.value.toMutableMap()
        currentNotes[equipmentId] = Pair(note, imageUris)
        _activeSessionNotes.value = currentNotes

        viewModelScope.launch(Dispatchers.IO) {
            val sessionId = _activeSession.value?.sessionId ?: return@launch
            val logs = dao.getLogsForSessionDirect(sessionId).filter { it.equipmentId == equipmentId }
            logs.forEach { log ->
                dao.updateLog(log.copy(sessionNote = note, sessionNoteImageUris = imageUris))
            }
        }
    }

    fun updatePastSessionNote(sessionId: Int, equipmentId: Int, note: String?, imageUris: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val logs = dao.getLogsForSessionDirect(sessionId).filter { it.equipmentId == equipmentId }
            logs.forEach { log ->
                dao.updateLog(log.copy(sessionNote = note, sessionNoteImageUris = imageUris))
            }
        }
    }

    fun getLastSessionNote(equipmentId: Int, currentSessionId: Int?): kotlinx.coroutines.flow.Flow<Pair<String?, String?>?> {
        return dao.getLogsForEquipment(equipmentId).map { logs ->
            val validLogs = logs.filter {
                it.sessionId != currentSessionId &&
                        (!it.sessionNote.isNullOrBlank() || !it.sessionNoteImageUris.isNullOrBlank())
            }
            val lastLog = validLogs.maxByOrNull { it.dateMillis }
            if (lastLog != null) Pair(lastLog.sessionNote, lastLog.sessionNoteImageUris) else null
        }
    }

    fun saveWorkoutLog(
        equipmentId: Int,
        weight: Float,
        reps: Int,
        sets: Int,
        customDateMillis: Long? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val timestamp = customDateMillis ?: System.currentTimeMillis()
            val newLogs = mutableListOf<WorkoutLog>()
            val currentSessionId = _activeSession.value?.sessionId

            val sessionLogs = if (currentSessionId != null) {
                dao.getLogsForSessionDirect(currentSessionId).filter { it.equipmentId == equipmentId }
            } else emptyList()

            val maxSetNumber = if (currentSessionId != null) {
                sessionLogs.maxOfOrNull { it.setNumber } ?: 0
            } else {
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val equipmentLogs = dao.getLogsForEquipment(equipmentId).first()
                equipmentLogs
                    .filter { it.dateMillis >= todayStart }
                    .maxOfOrNull { it.setNumber } ?: 0
            }

            val draftNote = _activeSessionNotes.value[equipmentId]
            val noteToSave = draftNote?.first ?: sessionLogs.firstOrNull()?.sessionNote
            val imagesToSave = draftNote?.second ?: sessionLogs.firstOrNull()?.sessionNoteImageUris

            var nextSetNumber = maxSetNumber + 1

            for (i in 1..sets) {
                newLogs.add(
                    WorkoutLog(
                        sessionId = currentSessionId,
                        equipmentId = equipmentId,
                        dateMillis = timestamp,
                        setNumber = nextSetNumber++,
                        weight = weight,
                        reps = reps,
                        isCompleted = true,
                        sessionNote = noteToSave,
                        sessionNoteImageUris = imagesToSave
                    )
                )
            }

            dao.insertWorkoutLogs(newLogs)
        }
    }

    fun getLogsFlow(equipmentId: Int) = dao.getLogsForEquipment(equipmentId)

    fun updateWorkoutLog(
        log: WorkoutLog,
        newWeight: Float,
        newReps: Int,
        newDateMillis: Long
    ) {
        viewModelScope.launch {
            val updatedLog =
                log.copy(weight = newWeight, reps = newReps, dateMillis = newDateMillis)
            dao.updateLog(updatedLog)
        }
    }

    fun deleteWorkoutLog(log: WorkoutLog) {
        viewModelScope.launch {
            dao.deleteLog(log)
        }
    }

    fun deleteLog(log: WorkoutLog) {
        viewModelScope.launch {
            dao.deleteLog(log)
        }
    }

    fun createWorkoutPlan(name: String) {
        viewModelScope.launch {
            val newPlan = WorkoutPlan(name = name)
            dao.insertWorkoutPlan(newPlan)
        }
    }

    fun updateWorkoutPlanName(plan: WorkoutPlan, newName: String) {
        viewModelScope.launch {
            dao.updateWorkoutPlan(plan.copy(name = newName))
        }
    }

    fun deleteWorkoutPlan(plan: WorkoutPlan) {
        viewModelScope.launch {
            dao.deleteWorkoutPlan(plan)
        }
    }

    fun addEquipmentToPlan(planId: Int, equipmentId: Int) {
        viewModelScope.launch {
            val planExercise = PlanExercise(
                planId = planId,
                equipmentId = equipmentId
            )
            dao.insertPlanExercise(planExercise)
        }
    }

    fun addMultipleEquipmentToPlan(planId: Int, equipmentIds: List<Int>) {
        viewModelScope.launch {
            equipmentIds.forEach { eqId ->
                dao.insertPlanExercise(
                    PlanExercise(
                        planId = planId,
                        equipmentId = eqId
                    )
                )
            }
        }
    }

    fun removeEquipmentFromPlan(planId: Int, equipmentId: Int) {
        viewModelScope.launch {
            dao.removeEquipmentFromPlan(planId, equipmentId)
        }
    }

    fun getEquipmentForPlanFlow(planId: Int) = dao.getEquipmentForPlan(planId)

    fun getEquipmentWithLogsForPlanFlow(planId: Int): kotlinx.coroutines.flow.Flow<List<com.example.gymapplication.data.EquipmentWithLog>> {
        return dao.getEquipmentWithLogsForPlan(planId)
    }

    fun reorderEquipmentInPlan(
        planId: Int,
        newList: List<com.example.gymapplication.data.EquipmentWithLog>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            newList.forEachIndexed { index, equipment ->
                dao.updatePlanExerciseOrder(planId, equipment.id, index)
            }
        }
    }

    fun importPlan(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val (planData, imageMap) = PlanImporter.extractPlanFromUri(context, uri)

                if (planData != null) {
                    val planId =
                        dao.insertWorkoutPlan(WorkoutPlan(name = planData.planName)).toInt()

                    planData.exercises.forEach { exercise ->
                        val imagePath = exercise.imageFileName?.let { imageMap[it] }

                        val newEquipment = Equipment(
                            name = exercise.name,
                            muscleGroup = exercise.muscleGroup,
                            imageUri = imagePath
                        )
                        val equipmentId = dao.insertEquipment(newEquipment).toInt()

                        dao.insertPlanExercise(
                            PlanExercise(
                                planId = planId,
                                equipmentId = equipmentId,
                                orderIndex = exercise.orderIndex
                            )
                        )
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Plan '${planData.planName}' importiert!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createFullBackup(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.gymapplication.gymUI.FullBackupManager.createAndShareBackup(context)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Backup wird vorbereitet...", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Fehler beim Backup: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun restoreFullBackup(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.gymapplication.gymUI.FullBackupManager.restoreBackup(context, uri)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Daten wiederhergestellt! Bitte schließe die App komplett und starte sie neu.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Fehler beim Importieren: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private val _weightGoal = MutableStateFlow("Abnehmen")
    val weightGoal = _weightGoal.asStateFlow()

    fun setWeightGoal(goal: String) {
        _weightGoal.value = goal
    }

    fun getEquipmentTrend(equipmentId: Int) = dao.getLogsForEquipment(equipmentId).map { logs ->
        val sorted = logs.sortedBy { it.dateMillis }
        if (sorted.size >= 2) {
            sorted.last().weight - sorted[sorted.size - 2].weight
        } else null
    }

    fun getBodyMetricTrend(type: String) = dao.getMetricsByType(type).map { metrics ->
        val sorted = metrics.sortedBy { it.dateMillis }
        if (sorted.size >= 2) {
            sorted.last().value - sorted[sorted.size - 2].value
        } else null
    }

    data class GhostValue(val weight: Float, val reps: Int)

    fun getLatestLogForEquipment(equipmentId: Int): kotlinx.coroutines.flow.Flow<GhostValue?> {
        return dao.getLogsForEquipment(equipmentId).map { logs ->
            logs.maxByOrNull { it.dateMillis }?.let {
                GhostValue(it.weight, it.reps)
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