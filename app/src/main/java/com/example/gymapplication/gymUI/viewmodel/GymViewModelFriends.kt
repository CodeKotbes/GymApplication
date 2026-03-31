package com.example.gymapplication.gymUI.viewmodel

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.example.gymapplication.data.Friend
import com.example.gymapplication.data.FriendExerciseMapping
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

internal fun GymViewModel.getMyUserId(context: Context): String {
    val prefs = context.getSharedPreferences("gym_settings", Context.MODE_PRIVATE)
    var uId = prefs.getString("my_user_id", null)
    if (uId == null) {
        uId = UUID.randomUUID().toString()
        prefs.edit().putString("my_user_id", uId).apply()
    }
    return uId
}

fun GymViewModel.generateFullExportJson(
    context: Context,
    myName: String,
    onResult: (String) -> Unit
) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            val myId = getMyUserId(context)
            val root = JSONObject()
            root.put("v", 4)
            root.put("uId", myId)
            root.put("name", myName.ifBlank { "Gym Bro" })
            root.put("ts", System.currentTimeMillis())

            val now = System.currentTimeMillis()
            val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
            val sixtyDaysAgo = now - (60L * 24 * 60 * 60 * 1000)

            val freq30 = dao.getWorkoutsCountDirect(thirtyDaysAgo)
            val vol30 = dao.getTotalVolumeDirect(thirtyDaysAgo) ?: 0f
            val volPrev = dao.getVolumeBetweenDirect(sixtyDaysAgo, thirtyDaysAgo) ?: 0f
            val prog = if (volPrev > 0) ((vol30 - volPrev) / volPrev) * 100 else 0f

            val currentPRs = personalRecords.first()
            val top3Strength = currentPRs.take(3).sumOf { it.maxWeight.toDouble() }.toFloat()

            val statsObj = JSONObject()
            statsObj.put("maxS", top3Strength.toDouble())
            statsObj.put("vol", vol30.toDouble())
            statsObj.put("freq", freq30)
            statsObj.put("prog", prog.toDouble())
            root.put("stats", statsObj)

            val volumeStats = dailyVolumeStats.first()
            val historyArray = JSONArray()
            volumeStats.forEach { stat ->
                try {
                    val date =
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(stat.dateStr)
                    if (date != null && date.time >= thirtyDaysAgo) {
                        val pointObj = JSONObject()
                        pointObj.put("d", date.time)
                        pointObj.put("v", stat.totalVolume.toDouble())
                        historyArray.put(pointObj)
                    }
                } catch (e: Exception) {
                }
            }
            root.put("volHistory", historyArray)

            val dataArray = JSONArray()
            val currentEquipment = equipmentWithLatestLogs.first()

            currentPRs.forEach { pr ->
                val eq = currentEquipment.find { it.name == pr.equipmentName }
                val item = JSONObject()
                item.put("n", pr.equipmentName)
                item.put("m", eq?.muscleGroup ?: "Unbekannt")
                item.put("prW", pr.maxWeight.toDouble())
                item.put("prR", pr.repsAtMaxWeight)
                item.put("prD", pr.dateOfMaxWeight)

                val exHistoryArray = JSONArray()
                if (eq != null) {
                    val logs = dao.getLogsForEquipment(eq.id).first()
                    val dailyMax = logs.groupBy {
                        SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                        ).format(Date(it.dateMillis))
                    }.mapNotNull { (_, dayLogs) ->
                        val maxLog = dayLogs.maxByOrNull { it.weight }
                        if (maxLog != null) {
                            val logObj = JSONObject()
                            logObj.put("d", maxLog.dateMillis)
                            logObj.put("w", maxLog.weight.toDouble())
                            logObj
                        } else null
                    }
                    dailyMax.forEach { exHistoryArray.put(it) }
                }
                item.put("h", exHistoryArray)
                dataArray.put(item)
            }
            root.put("data", dataArray)

            val muscleStatsList = detailedMuscleStats.first()
            val muscleArray = JSONArray()
            muscleStatsList.forEach { stat ->
                val obj = JSONObject()
                obj.put("m", stat.muscleGroup)
                obj.put("e", stat.equipmentName)
                obj.put("s", stat.totalSets)
                muscleArray.put(obj)
            }
            root.put("muscleStatsArr", muscleArray)

            withContext(Dispatchers.Main) { onResult(root.toString()) }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) { onResult("") }
        }
    }
}

fun GymViewModel.importFriendFromFile(context: Context, uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader().use { it?.readText() } ?: ""
            val root = JSONObject(jsonString)
            val baseUId = root.getString("uId")
            val name = root.getString("name")
            val ts = root.getLong("ts")

            if (baseUId == getMyUserId(context)) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Das ist deine eigene Datei!", Toast.LENGTH_SHORT)
                        .show()
                }
                return@launch
            }

            val uniqueUserId = "${baseUId}_$ts"
            val friend = Friend(
                userId = uniqueUserId,
                name = name,
                lastSyncMillis = ts,
                snapshotJson = jsonString
            )
            dao.insertFriend(friend)

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "$name erfolgreich importiert!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Fehler beim Lesen der Datei.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

fun GymViewModel.deleteFriend(friend: Friend) {
    viewModelScope.launch(Dispatchers.IO) { dao.deleteFriend(friend) }
}

fun GymViewModel.getFriendMappingsFlow(friendId: String) = dao.getMappingsForFriend(friendId)

fun GymViewModel.saveFriendMapping(friendId: String, friendExName: String, myEqId: Int) {
    viewModelScope.launch(Dispatchers.IO) {
        dao.insertFriendMapping(
            FriendExerciseMapping(
                friendUserId = friendId,
                friendExerciseName = friendExName,
                myEquipmentId = myEqId
            )
        )
    }
}

fun GymViewModel.deleteFriendMapping(friendUserId: String, friendExerciseName: String) {
    viewModelScope.launch(Dispatchers.IO) {
        dao.deleteFriendMapping(friendUserId, friendExerciseName)
    }
}