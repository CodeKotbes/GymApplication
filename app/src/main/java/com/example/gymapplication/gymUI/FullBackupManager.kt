package com.example.gymapplication.gymUI

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FullBackupManager {
    private const val BACKUP_FILE_NAME = "GymApp_FullBackup.gymbackup"

    fun createAndShareBackup(context: Context) {
        val appContext = context.applicationContext
        val backupFile = File(appContext.cacheDir, BACKUP_FILE_NAME)
        val dbFile = appContext.getDatabasePath("gym_database")

        if (backupFile.exists()) backupFile.delete()

        try {
            ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
                val dbFiles = listOf(
                    dbFile,
                    File(dbFile.path + "-shm"),
                    File(dbFile.path + "-wal")
                )

                dbFiles.forEach { file ->
                    if (file.exists()) {
                        addToZip(file, "database/${file.name}", zos)
                    }
                }

                val imagesDir = appContext.filesDir
                imagesDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.endsWith(".jpg")) {
                        addToZip(file, "images/${file.name}", zos)
                    }
                }
            }

            val uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                backupFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Vollständiges Backup sichern...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun restoreBackup(context: Context, uri: Uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val targetFile = if (entry.name.startsWith("database/")) {
                            val fileName = entry.name.removePrefix("database/")
                            context.getDatabasePath(fileName)
                        } else if (entry.name.startsWith("images/")) {
                            val fileName = entry.name.removePrefix("images/")
                            File(context.filesDir, fileName)
                        } else null

                        targetFile?.let {
                            it.parentFile?.mkdirs()
                            FileOutputStream(it).use { fos -> zis.copyTo(fos) }
                        }
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addToZip(file: File, zipPath: String, zos: ZipOutputStream) {
        val entry = ZipEntry(zipPath)
        zos.putNextEntry(entry)
        FileInputStream(file).use { fis -> fis.copyTo(zos) }
        zos.closeEntry()
    }
}