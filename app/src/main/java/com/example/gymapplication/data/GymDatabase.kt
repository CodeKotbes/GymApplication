package com.example.gymapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Equipment::class, WorkoutLog::class, WorkoutPlan::class,
        PlanExercise::class, WorkoutSession::class, PlannedWorkout::class,
        BodyMetric::class, Friend::class, FriendExerciseMapping::class, BodyTarget::class
    ],
    version = 18,
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

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN durationInSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE equipment_table ADD COLUMN generalNote TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE equipment_table ADD COLUMN generalNoteImageUris TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE workout_log_table ADD COLUMN sessionNote TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE workout_log_table ADD COLUMN sessionNoteImageUris TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `friends_table` (`userId` TEXT NOT NULL, `name` TEXT NOT NULL, `lastSyncMillis` INTEGER NOT NULL, `snapshotJson` TEXT NOT NULL, PRIMARY KEY(`userId`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `friend_exercise_mapping` (`friendUserId` TEXT NOT NULL, `friendExerciseName` TEXT NOT NULL, `myEquipmentId` INTEGER NOT NULL, PRIMARY KEY(`friendUserId`, `friendExerciseName`))")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE equipment_table ADD COLUMN targetValue REAL DEFAULT NULL")
                db.execSQL("CREATE TABLE IF NOT EXISTS `body_targets` (`type` TEXT NOT NULL, `targetValue` REAL NOT NULL, PRIMARY KEY(`type`))")
            }
        }

        fun getDatabase(context: Context): GymDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "gym_database"
                )
                    .addMigrations(
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}