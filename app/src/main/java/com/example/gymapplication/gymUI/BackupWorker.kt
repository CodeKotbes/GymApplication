package com.example.gymapplication.gymUI

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class BackupWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val sharedPrefs =
            applicationContext.getSharedPreferences("gym_settings", Context.MODE_PRIVATE)
        val folderUri = sharedPrefs.getString("auto_backup_folder_uri", null)
        val isEnabled = sharedPrefs.getBoolean("auto_backup_enabled", false)

        return if (isEnabled && folderUri != null) {
            val success = FullBackupManager.createAutoBackup(applicationContext, folderUri)
            if (success) Result.success() else Result.retry()
        } else {
            Result.failure()
        }
    }
}