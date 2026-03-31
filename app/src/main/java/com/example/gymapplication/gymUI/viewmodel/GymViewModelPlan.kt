package com.example.gymapplication.gymUI.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.example.gymapplication.data.Equipment
import com.example.gymapplication.data.PlanExercise
import com.example.gymapplication.data.PlannedWorkout
import com.example.gymapplication.data.WorkoutPlan
import com.example.gymapplication.gymUI.plan.PlanImporter
import com.example.gymapplication.gymUI.workout.WorkoutAlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun GymViewModel.createWorkoutPlan(name: String) {
    viewModelScope.launch { dao.insertWorkoutPlan(WorkoutPlan(name = name)) }
}

fun GymViewModel.deleteWorkoutPlan(plan: WorkoutPlan) {
    viewModelScope.launch { dao.deleteWorkoutPlan(plan) }
}

fun GymViewModel.updateWorkoutPlanName(plan: WorkoutPlan, newName: String) {
    viewModelScope.launch {
        dao.updateWorkoutPlan(plan.copy(name = newName))
    }
}

fun GymViewModel.addMultipleEquipmentToPlan(planId: Int, equipmentIds: List<Int>) {
    viewModelScope.launch {
        equipmentIds.forEach { eqId ->
            dao.insertPlanExercise(PlanExercise(planId = planId, equipmentId = eqId))
        }
    }
}

fun GymViewModel.removeEquipmentFromPlan(planId: Int, equipmentId: Int) {
    viewModelScope.launch { dao.removeEquipmentFromPlan(planId, equipmentId) }
}

fun GymViewModel.getEquipmentWithLogsForPlanFlow(planId: Int) =
    dao.getEquipmentWithLogsForPlan(planId)

fun GymViewModel.reorderEquipmentInPlan(
    planId: Int,
    newList: List<com.example.gymapplication.data.EquipmentWithLog>
) {
    viewModelScope.launch(Dispatchers.IO) {
        newList.forEachIndexed { index, equipment ->
            dao.updatePlanExerciseOrder(planId, equipment.id, index)
        }
    }
}

fun GymViewModel.importPlan(context: Context, uri: Uri) {
    viewModelScope.launch {
        try {
            val (planData, imageMap) = PlanImporter.extractPlanFromUri(context, uri)
            if (planData != null) {
                val planId = dao.insertWorkoutPlan(WorkoutPlan(name = planData.planName)).toInt()
                planData.exercises.forEach { exercise ->
                    val mainImagePath = exercise.imageFileName?.let { imageMap[it] }
                    val noteImagePaths =
                        exercise.generalNoteImageFileNames?.split("|")?.mapNotNull { fileName ->
                            imageMap[fileName]
                        }?.joinToString("|")?.takeIf { it.isNotBlank() }

                    val newEquipment = Equipment(
                        name = exercise.name, muscleGroup = exercise.muscleGroup,
                        imageUri = mainImagePath, generalNote = exercise.generalNote,
                        generalNoteImageUris = noteImagePaths
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
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Fehler beim Import: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

fun GymViewModel.scheduleWorkout(
    context: Context,
    planId: Int,
    planName: String,
    dateMillis: Long
) {
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

internal fun GymViewModel.setWorkoutAlarm(
    context: Context,
    planName: String,
    exactTimeMillis: Long
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent =
        Intent(context, WorkoutAlarmReceiver::class.java).apply { putExtra("PLAN_NAME", planName) }

    val triggerTime =
        if (exactTimeMillis <= System.currentTimeMillis()) System.currentTimeMillis() + 2000 else exactTimeMillis
    val requestCode = (exactTimeMillis / 1000).toInt()
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    } else {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }
}

fun GymViewModel.deletePlannedWorkout(context: Context, planned: PlannedWorkout) {
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

fun GymViewModel.saveEquipment(name: String, muscleGroup: String, imageUri: String?) {
    viewModelScope.launch {
        dao.insertEquipment(Equipment(name = name, muscleGroup = muscleGroup, imageUri = imageUri))
    }
}

fun GymViewModel.updateEquipmentDetails(
    equipment: Equipment,
    newName: String,
    newMuscle: String,
    newImageUri: String?
) {
    viewModelScope.launch {
        dao.updateEquipment(
            equipment.copy(
                name = newName,
                muscleGroup = newMuscle,
                imageUri = newImageUri
            )
        )
    }
}

fun GymViewModel.updateEquipmentNote(equipment: Equipment, note: String?, imageUris: String?) {
    viewModelScope.launch(Dispatchers.IO) {
        dao.updateEquipment(equipment.copy(generalNote = note, generalNoteImageUris = imageUris))
    }
}

fun GymViewModel.deleteEquipment(equipment: Equipment) {
    viewModelScope.launch { dao.deleteEquipment(equipment) }
}