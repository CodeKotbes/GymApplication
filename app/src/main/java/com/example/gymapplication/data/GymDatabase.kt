package com.example.gymapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Equipment::class, WorkoutLog::class, WorkoutPlan::class, PlanExercise::class, WorkoutSession::class, PlannedWorkout::class, BodyMetric::class],
    version = 16, // WICHTIG: Version auf 16 erhöht!
    exportSchema = false
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun gymDao(): GymDao

    companion object {
        @Volatile
        private var INSTANCE: GymDatabase? = null

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plan_exercise_table ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN isPaused INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN lastPausedTimeMillis INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN accumulatedPauseTimeMillis INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN restEndTimeMillis INTEGER DEFAULT NULL")
            }
        }

        // NEU: Migration von 15 auf 16 (Feld für die Workout-Dauer hinzugefügt)
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Hängt die neue Spalte an die bestehende Tabelle an. Default 0, da bestehende Workouts 0 Sekunden Dauer haben.
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN durationInSeconds INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): GymDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "gym_database"
                )
                    // NEU: MIGRATION_15_16 am Ende der Liste hinzugefügt
                    .addMigrations(MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}