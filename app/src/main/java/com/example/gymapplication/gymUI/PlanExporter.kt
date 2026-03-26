package com.example.gymapplication.gymUI

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.gymapplication.data.WorkoutPlan
import com.google.gson.Gson
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import android.net.Uri
import com.example.gymapplication.data.EquipmentWithLog
import java.util.zip.ZipInputStream

data class PlanExportData(
    val planName: String,
    val exercises: List<ExerciseExportData>
)

data class ExerciseExportData(
    val name: String,
    val muscleGroup: String,
    val imageFileName: String?,
    val orderIndex: Int
)

object PlanExporter {
    fun exportAndSharePlan(
        context: Context,
        plan: WorkoutPlan,
        equipmentList: List<EquipmentWithLog>
    ) {
        val exportExercises = equipmentList.mapIndexed { index, equipment ->
            val fileName = equipment.imageUri?.let { File(it).name }
            ExerciseExportData(
                name = equipment.name,
                muscleGroup = equipment.muscleGroup,
                imageFileName = fileName,
                orderIndex = index
            )
        }
        val exportData = PlanExportData(plan.name, exportExercises)
        val jsonString = Gson().toJson(exportData)

        val exportDir = File(context.cacheDir, "images").apply { mkdirs() }
        val zipFile = File(exportDir, "${plan.name.replace(" ", "_")}.gymplan")

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            val jsonEntry = ZipEntry("plan.json")
            zos.putNextEntry(jsonEntry)
            zos.write(jsonString.toByteArray())
            zos.closeEntry()

            equipmentList.forEach { equipment ->
                equipment.imageUri?.let { imagePath ->
                    val imageFile = File(imagePath)
                    if (imageFile.exists()) {
                        val imageEntry = ZipEntry(imageFile.name)
                        zos.putNextEntry(imageEntry)
                        FileInputStream(imageFile).use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Trainingsplan teilen..."))
    }
}

object PlanImporter {
    fun extractPlanFromUri(context: Context, uri: Uri): Pair<PlanExportData?, Map<String, String>> {
        var planData: PlanExportData? = null
        val imageFilePaths = mutableMapOf<String, String>()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "plan.json") {
                        val json = zis.bufferedReader().readText()
                        planData = Gson().fromJson(json, PlanExportData::class.java)
                    } else {
                        val newFile = File(
                            context.filesDir,
                            "imported_${System.currentTimeMillis()}_${entry.name}"
                        )
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        imageFilePaths[entry.name] = newFile.absolutePath
                    }
                    entry = zis.nextEntry
                }
            }
        }
        return Pair(planData, imageFilePaths)
    }
}