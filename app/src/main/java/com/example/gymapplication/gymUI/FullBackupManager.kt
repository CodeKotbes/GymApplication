package com.example.gymapplication.gymUI

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FullBackupManager {
    private const val BACKUP_FILE_NAME = "GymApp_FullBackup.gymbackup"
    private const val AUTO_BACKUP_PREFIX = "GymApp_Auto_"

    fun createAndShareBackup(context: Context) {
        val appContext = context.applicationContext
        val backupFile = File(appContext.cacheDir, BACKUP_FILE_NAME)

        try {
            performBackupToStream(appContext, FileOutputStream(backupFile))
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
            appContext.startActivity(
                Intent.createChooser(intent, "Backup sichern...")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createAutoBackup(context: Context, folderUriString: String): Boolean {
        return try {
            val folderUri = Uri.parse(folderUriString)
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return false

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val fileName = "$AUTO_BACKUP_PREFIX$timestamp.gymbackup"

            val newFile = folder.createFile("application/octet-stream", fileName) ?: return false

            context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                performBackupToStream(context, outputStream)
            }

            rotateBackups(folder)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun performBackupToStream(context: Context, outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zos ->
            val dbFile = context.getDatabasePath("gym_database")
            listOf(dbFile, File(dbFile.path + "-shm"), File(dbFile.path + "-wal")).forEach { file ->
                if (file.exists()) addToZip(file, "database/${file.name}", zos)
            }
            context.filesDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".jpg")) {
                    addToZip(file, "images/${file.name}", zos)
                }
            }
        }
    }

    private fun rotateBackups(folder: DocumentFile) {
        val backups = folder.listFiles()
            .filter { it.name?.startsWith(AUTO_BACKUP_PREFIX) == true }
            .sortedByDescending { it.lastModified() }

        if (backups.size > 5) {
            backups.drop(5).forEach { it.delete() }
        }
    }

    private fun addToZip(file: File, zipPath: String, zos: ZipOutputStream) {
        val entry = ZipEntry(zipPath)
        zos.putNextEntry(entry)
        FileInputStream(file).use { fis -> fis.copyTo(zos) }
        zos.closeEntry()
    }

    fun restoreBackup(context: Context, uri: Uri) {
        try {
            com.example.gymapplication.data.GymDatabase.getDatabase(context).close()

            context.deleteDatabase("gym_database")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        // Ignoriere Ordner-Einträge in der Zip
                        if (entry.isDirectory) {
                            zis.closeEntry()
                            entry = zis.nextEntry
                            continue
                        }

                        // Macht den Import abwärtskompatibel zu V13: Ignoriert Ordnerpfade im Zip!
                        val pureFileName = entry.name.substringAfterLast("/")

                        val targetFile = if (pureFileName.startsWith("gym_database")) {
                            // Erkennt gym_database, gym_database-wal, gym_database-shm
                            context.getDatabasePath(pureFileName)
                        } else if (pureFileName.endsWith(".jpg") || pureFileName.endsWith(".png")) {
                            // Erkennt alle Bilder, egal wo sie im Zip lagen
                            File(context.filesDir, pureFileName)
                        } else {
                            null
                        }

                        targetFile?.let { file ->
                            file.parentFile?.mkdirs()
                            if (file.exists()) file.delete()

                            FileOutputStream(file).use { fos -> zis.copyTo(fos) }
                        }

                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            android.widget.Toast.makeText(context, "Backup importiert! App startet neu...", android.widget.Toast.LENGTH_LONG).show()

            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            handler.postDelayed({
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(0)
            }, 1000)

        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Fehler beim Import: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}