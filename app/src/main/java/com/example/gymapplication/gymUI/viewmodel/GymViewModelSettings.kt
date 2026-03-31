package com.example.gymapplication.gymUI.viewmodel

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.gymapplication.data.BodyMetric
import com.example.gymapplication.data.BodyTarget
import com.example.gymapplication.gymUI.backup.BackupWorker
import com.example.gymapplication.gymUI.backup.FullBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

fun GymViewModel.updateBackupSchedule(context: Context) {
    val sharedPrefs = context.getSharedPreferences("gym_settings", Context.MODE_PRIVATE)
    val enabled = sharedPrefs.getBoolean("auto_backup_enabled", false)
    val frequency = sharedPrefs.getString("auto_backup_frequency", "Täglich")
    val folderUri = sharedPrefs.getString("auto_backup_folder_uri", null)
    val hour = sharedPrefs.getInt("auto_backup_hour", 2)
    val minute = sharedPrefs.getInt("auto_backup_minute", 0)

    val workManager = WorkManager.getInstance(context)

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

    val request = PeriodicWorkRequestBuilder<BackupWorker>(interval, TimeUnit.DAYS)
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build())
        .build()

    workManager.enqueueUniquePeriodicWork(
        "gym_auto_backup",
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

fun GymViewModel.createFullBackup(context: Context) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            FullBackupManager.createAndShareBackup(context)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Backup wird vorbereitet...", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
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

fun GymViewModel.restoreFullBackup(context: Context, uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            FullBackupManager.restoreBackup(context, uri)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Daten wiederhergestellt! Bitte schließe die App komplett und starte sie neu.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
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

fun GymViewModel.loadWeightGoal(context: Context) {
    val sharedPrefs = context.getSharedPreferences("gym_settings", Context.MODE_PRIVATE)
    _weightGoal.value = sharedPrefs.getString("saved_weight_goal", "Abnehmen") ?: "Abnehmen"
}

fun GymViewModel.setWeightGoal(context: Context, goal: String) {
    _weightGoal.value = goal
    context.getSharedPreferences("gym_settings", Context.MODE_PRIVATE)
        .edit().putString("saved_weight_goal", goal).apply()
}

fun GymViewModel.addBodyMetric(
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

fun GymViewModel.getBodyMetrics(type: String) = dao.getMetricsByType(type)

fun GymViewModel.updateBodyMetric(
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

fun GymViewModel.deleteBodyMetric(metric: BodyMetric) {
    viewModelScope.launch(Dispatchers.IO) { dao.deleteBodyMetric(metric) }
}

fun GymViewModel.saveBodyTarget(type: String, value: Float?) {
    viewModelScope.launch(Dispatchers.IO) {
        if (value == null) dao.deleteBodyTarget(type)
        else dao.insertBodyTarget(BodyTarget(type, value))
    }
}

fun GymViewModel.saveEquipmentTarget(id: Int, value: Float?) {
    viewModelScope.launch(Dispatchers.IO) { dao.updateEquipmentTarget(id, value) }
}

fun GymViewModel.getEquipmentTrend(equipmentId: Int) =
    dao.getLogsForEquipment(equipmentId).map { logs ->
        if (logs.size >= 2) logs[0].weight - logs[1].weight else null
    }

fun GymViewModel.getBodyMetricTrend(type: String) = dao.getMetricsByType(type).map { metrics ->
    if (metrics.size >= 2) metrics.last().value - metrics[metrics.size - 2].value else null
}