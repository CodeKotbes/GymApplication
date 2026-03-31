package com.example.gymapplication.gymUI.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.viewModelScope
import com.example.gymapplication.data.WorkoutLog
import com.example.gymapplication.data.WorkoutSession
import com.example.gymapplication.gymUI.workout.WorkoutService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

internal fun GymViewModel.startTimer(session: WorkoutSession) {
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

internal fun GymViewModel.triggerVibration(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(500)
    }
}

internal fun GymViewModel.startRestTimerLoop(context: Context?) {
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

fun GymViewModel.toggleWorkoutPause(context: Context) {
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
            currentSession.copy(isPaused = true, lastPausedTimeMillis = now)
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

fun GymViewModel.startWorkout(context: Context, planId: Int?, name: String, restTime: Int) {
    _currentRestTime.value = restTime
    _currentExerciseIndex.value = 0
    _activeSessionNotes.value = emptyMap()
    skipRest()

    viewModelScope.launch(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val newSession = WorkoutSession(startTimeMillis = startTime, planId = planId, name = name)
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

fun GymViewModel.finishWorkout(context: Context) {
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

        val intent = Intent(context, WorkoutService::class.java).apply { action = "STOP_WORKOUT" }
        context.startService(intent)
    }
}

fun GymViewModel.triggerWorkoutSummary() {
    _triggerSummaryEvent.value = true
}

fun GymViewModel.consumeSummaryEvent() {
    _triggerSummaryEvent.value = false
}

fun GymViewModel.updateExerciseIndex(newIndex: Int) {
    _currentExerciseIndex.value = newIndex
}

fun GymViewModel.startRestTimer(context: Context, seconds: Int) {
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

fun GymViewModel.adjustRestTime(deltaSeconds: Int) {
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

fun GymViewModel.skipRest() {
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

fun GymViewModel.updateActiveSessionNote(equipmentId: Int, note: String?, imageUris: String?) {
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

fun GymViewModel.updatePastSessionNote(
    sessionId: Int,
    equipmentId: Int,
    note: String?,
    imageUris: String?
) {
    viewModelScope.launch(Dispatchers.IO) {
        val logs = dao.getLogsForSessionDirect(sessionId).filter { it.equipmentId == equipmentId }
        logs.forEach { log ->
            dao.updateLog(log.copy(sessionNote = note, sessionNoteImageUris = imageUris))
        }
    }
}

fun GymViewModel.getLastSessionNote(
    equipmentId: Int,
    currentSessionId: Int?
): kotlinx.coroutines.flow.Flow<Pair<String?, String?>?> {
    return dao.getLogsForEquipment(equipmentId).map { logs ->
        val validLogs = logs.filter {
            it.sessionId != currentSessionId && (!it.sessionNote.isNullOrBlank() || !it.sessionNoteImageUris.isNullOrBlank())
        }
        val lastLog = validLogs.maxByOrNull { it.dateMillis }
        if (lastLog != null) Pair(lastLog.sessionNote, lastLog.sessionNoteImageUris) else null
    }
}

fun GymViewModel.saveWorkoutLog(
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
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(
                Calendar.MILLISECOND,
                0
            )
            }.timeInMillis
            val equipmentLogs = dao.getLogsForEquipment(equipmentId).first()
            equipmentLogs.filter { it.dateMillis >= todayStart }.maxOfOrNull { it.setNumber } ?: 0
        }

        val draftNote = _activeSessionNotes.value[equipmentId]
        val noteToSave = draftNote?.first ?: sessionLogs.firstOrNull()?.sessionNote
        val imagesToSave = draftNote?.second ?: sessionLogs.firstOrNull()?.sessionNoteImageUris

        var nextSetNumber = maxSetNumber + 1
        for (i in 1..sets) {
            newLogs.add(
                WorkoutLog(
                    sessionId = currentSessionId, equipmentId = equipmentId, dateMillis = timestamp,
                    setNumber = nextSetNumber++, weight = weight, reps = reps, isCompleted = true,
                    sessionNote = noteToSave, sessionNoteImageUris = imagesToSave
                )
            )
        }
        dao.insertWorkoutLogs(newLogs)
    }
}

fun GymViewModel.updateWorkoutLog(
    log: WorkoutLog,
    newWeight: Float,
    newReps: Int,
    newDateMillis: Long
) {
    viewModelScope.launch {
        val updatedLog = log.copy(weight = newWeight, reps = newReps, dateMillis = newDateMillis)
        dao.updateLog(updatedLog)
    }
}

fun GymViewModel.deleteWorkoutLog(log: WorkoutLog) {
    viewModelScope.launch { dao.deleteLog(log) }
}

fun GymViewModel.getLogsFlow(equipmentId: Int) = dao.getLogsForEquipment(equipmentId)

fun GymViewModel.getLogsForSessionFlow(sessionId: Int) = dao.getLogsForSession(sessionId)

fun GymViewModel.getLatestLogForEquipment(equipmentId: Int): kotlinx.coroutines.flow.Flow<GhostValue?> {
    return dao.getLogsForEquipment(equipmentId).map { logs ->
        logs.maxByOrNull { it.dateMillis }?.let { GhostValue(it.weight, it.reps) }
    }
}

fun GymViewModel.updateWorkoutSessionDate(session: WorkoutSession, newDateMillis: Long) {
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

fun GymViewModel.deleteWorkoutSession(session: WorkoutSession) {
    viewModelScope.launch(Dispatchers.IO) {
        dao.deleteLogsForSession(session.sessionId)
        dao.deleteWorkoutSession(session)
    }
}