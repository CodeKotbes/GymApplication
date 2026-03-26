package com.example.gymapplication.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "equipment_table")
data class Equipment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val muscleGroup: String,
    val imageUri: String?
)

@Entity(tableName = "workout_plan_table")
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Entity(
    tableName = "plan_exercise_table",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Equipment::class,
            parentColumns = ["id"],
            childColumns = ["equipmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId"), Index("equipmentId")]
)
data class PlanExercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val planId: Int,
    val equipmentId: Int,
    val orderIndex: Int = 0
)

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val sessionId: Int = 0,
    val startTimeMillis: Long,
    val endTimeMillis: Long? = null,
    val planId: Int? = null,
    val name: String,
    val restTimeSeconds: Int = 120,
    val isPaused: Boolean = false,
    val lastPausedTimeMillis: Long? = null,
    val accumulatedPauseTimeMillis: Long = 0L,
    val restEndTimeMillis: Long? = null
)

@Entity(
    tableName = "workout_log_table",
    foreignKeys = [
        ForeignKey(
            entity = Equipment::class,
            parentColumns = ["id"],
            childColumns = ["equipmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("equipmentId"), Index("sessionId")]
)
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int? = null,
    val equipmentId: Int,
    val dateMillis: Long,
    val setNumber: Int,
    val weight: Float,
    val reps: Int,
    val isCompleted: Boolean = false
)

@Entity(tableName = "planned_workouts")
data class PlannedWorkout(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val planId: Int,
    val planName: String,
    val dateMillis: Long
)

@Entity(tableName = "body_metrics")
data class BodyMetric(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,
    val value: Float,
    val dateMillis: Long,
    val imageUri: String? = null
)