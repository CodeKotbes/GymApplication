package com.example.gymapplication.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class HistoryItem(
    val logId: Int,
    val equipmentName: String,
    val weight: Float,
    val reps: Int,
    val dateMillis: Long
)

data class EquipmentWithLog(
    val id: Int,
    val name: String,
    val muscleGroup: String,
    val imageUri: String?,
    val latestWeight: Float?,
    val latestReps: Int?,
    val latestSets: Int?
)

data class DetailedMuscleStat(
    val muscleGroup: String,
    val equipmentName: String,
    val totalSets: Int
)

data class DailyVolumeStat(
    val dateStr: String,
    val totalVolume: Float
)

@Dao
interface GymDao {
    @Query("SELECT * FROM equipment_table ORDER BY name ASC")
    fun getAllEquipment(): Flow<List<Equipment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(equipment: Equipment): Long

    @Update
    suspend fun updateEquipment(equipment: Equipment)

    @Delete
    suspend fun deleteEquipment(equipment: Equipment)

    @Query("SELECT * FROM workout_log_table WHERE equipmentId = :equipmentId ORDER BY dateMillis DESC, setNumber DESC")
    fun getLogsForEquipment(equipmentId: Int): Flow<List<WorkoutLog>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLog(log: WorkoutLog)

    @Delete
    suspend fun deleteLog(log: WorkoutLog)

    @Query("SELECT * FROM workout_plan_table")
    fun getAllWorkoutPlans(): Flow<List<WorkoutPlan>>

    @Insert
    suspend fun insertWorkoutPlan(plan: WorkoutPlan): Long

    @Insert
    suspend fun insertPlanExercise(planExercise: PlanExercise)

    @Query(
        """
        SELECT e.* FROM equipment_table e 
        INNER JOIN plan_exercise_table pe ON e.id = pe.equipmentId 
        WHERE pe.planId = :planId
        ORDER BY pe.orderIndex ASC
    """
    )
    fun getEquipmentForPlan(planId: Int): Flow<List<Equipment>>

    @Delete
    suspend fun deleteWorkoutPlan(plan: WorkoutPlan)

    @Query(
        """
        SELECT l.id as logId, e.name as equipmentName, l.weight, l.reps, l.dateMillis 
        FROM workout_log_table l 
        INNER JOIN equipment_table e ON l.equipmentId = e.id 
        ORDER BY l.dateMillis DESC
    """
    )
    fun getFullHistory(): Flow<List<HistoryItem>>

    @Query(
        """
        SELECT e.id, e.name, e.muscleGroup, e.imageUri,
               (SELECT weight FROM workout_log_table WHERE equipmentId = e.id ORDER BY dateMillis DESC, setNumber DESC LIMIT 1) as latestWeight,
               (SELECT reps FROM workout_log_table WHERE equipmentId = e.id ORDER BY dateMillis DESC, setNumber DESC LIMIT 1) as latestReps,
               (SELECT setNumber FROM workout_log_table WHERE equipmentId = e.id ORDER BY dateMillis DESC, setNumber DESC LIMIT 1) as latestSets
        FROM equipment_table e ORDER BY e.name ASC
    """
    )
    fun getEquipmentWithLatestLog(): Flow<List<EquipmentWithLog>>

    @Update
    suspend fun updateLog(log: WorkoutLog)

    @Update
    suspend fun updateWorkoutPlan(plan: WorkoutPlan)

    @Query("DELETE FROM plan_exercise_table WHERE planId = :planId AND equipmentId = :equipmentId")
    suspend fun removeEquipmentFromPlan(planId: Int, equipmentId: Int)

    @Query(
        """
        SELECT e.id, e.name, e.muscleGroup, e.imageUri,
               (SELECT weight FROM workout_log_table WHERE equipmentId = e.id ORDER BY dateMillis DESC, setNumber DESC LIMIT 1) as latestWeight,
               (SELECT reps FROM workout_log_table WHERE equipmentId = e.id ORDER BY dateMillis DESC, setNumber DESC LIMIT 1) as latestReps,
               (SELECT setNumber FROM workout_log_table WHERE equipmentId = e.id ORDER BY dateMillis DESC, setNumber DESC LIMIT 1) as latestSets
        FROM equipment_table e 
        INNER JOIN plan_exercise_table pe ON e.id = pe.equipmentId 
        WHERE pe.planId = :planId
        ORDER BY pe.orderIndex ASC
    """
    )
    fun getEquipmentWithLogsForPlan(planId: Int): Flow<List<EquipmentWithLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLogs(logs: List<WorkoutLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(session: WorkoutSession): Long

    @Update
    suspend fun updateWorkoutSession(session: WorkoutSession)

    @Query("SELECT * FROM workout_sessions WHERE endTimeMillis IS NULL LIMIT 1")
    suspend fun getActiveWorkoutSession(): WorkoutSession?

    @Query("SELECT * FROM workout_sessions WHERE endTimeMillis IS NOT NULL ORDER BY startTimeMillis DESC")
    fun getAllFinishedSessions(): Flow<List<WorkoutSession>>

    @Query(
        """
    SELECT l.*, e.name as equipmentName 
    FROM workout_log_table l 
    INNER JOIN equipment_table e ON l.equipmentId = e.id 
    WHERE l.sessionId = :sessionId 
    ORDER BY l.dateMillis ASC, l.setNumber ASC
"""
    )
    fun getLogsForSession(sessionId: Int): Flow<List<WorkoutLog>>

    @Delete
    suspend fun deleteWorkoutSession(session: WorkoutSession)

    @Query("DELETE FROM workout_log_table WHERE sessionId = :sessionId")
    suspend fun deleteLogsForSession(sessionId: Int)

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertPlannedWorkout(plannedWorkout: PlannedWorkout)

    @Delete
    suspend fun deletePlannedWorkout(plannedWorkout: PlannedWorkout)

    @Query("SELECT * FROM planned_workouts ORDER BY dateMillis ASC")
    fun getAllPlannedWorkouts(): kotlinx.coroutines.flow.Flow<List<PlannedWorkout>>


    @Query(
        """
    DELETE FROM planned_workouts 
    WHERE planId = :planId 
    AND dateMillis >= :startOfDay 
    AND dateMillis < :endOfDay
"""
    )
    suspend fun deletePlannedWorkoutForToday(planId: Int, startOfDay: Long, endOfDay: Long)

    @Insert
    suspend fun insertBodyMetric(metric: BodyMetric)

    @Query("SELECT * FROM body_metrics WHERE type = :type ORDER BY dateMillis ASC")
    fun getMetricsByType(type: String): kotlinx.coroutines.flow.Flow<List<BodyMetric>>

    @Delete
    suspend fun deleteBodyMetric(metric: BodyMetric)

    @Update
    suspend fun updateBodyMetric(metric: BodyMetric)

    @Query("SELECT * FROM workout_log_table WHERE sessionId = :sessionId")
    suspend fun getLogsForSessionDirect(sessionId: Int): List<WorkoutLog>

    @Query("UPDATE plan_exercise_table SET orderIndex = :newIndex WHERE planId = :planId AND equipmentId = :equipmentId")
    suspend fun updatePlanExerciseOrder(planId: Int, equipmentId: Int, newIndex: Int)

    @Query(
        """
        SELECT e.muscleGroup, e.name as equipmentName, COUNT(l.id) as totalSets
        FROM workout_log_table l
        INNER JOIN equipment_table e ON l.equipmentId = e.id
        WHERE l.dateMillis >= :sinceMillis
        GROUP BY e.muscleGroup, e.id
        ORDER BY totalSets DESC
    """
    )
    fun getDetailedMuscleStats(sinceMillis: Long): kotlinx.coroutines.flow.Flow<List<DetailedMuscleStat>>

    @Query(
        """
        SELECT strftime('%Y-%m-%d', dateMillis / 1000, 'unixepoch', 'localtime') as dateStr, 
               SUM(weight * reps) as totalVolume
        FROM workout_log_table
        GROUP BY dateStr
        ORDER BY dateStr ASC
    """
    )
    fun getDailyVolumeStats(): Flow<List<DailyVolumeStat>>
}