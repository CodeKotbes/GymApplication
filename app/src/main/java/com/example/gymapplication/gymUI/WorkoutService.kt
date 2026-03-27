package com.example.gymapplication.gymUI

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.gymapplication.MainActivity
import com.example.gymapplication.data.GymDatabase
import kotlinx.coroutines.*
import java.util.Locale

class WorkoutService : Service() {
    private val channelId = "workout_channel"
    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        timerJob?.cancel()

        serviceScope.launch {
            try {
                val db = GymDatabase.getDatabase(applicationContext)
                val session = db.gymDao().getActiveWorkoutSession()

                if (session != null && !session.isPaused) {
                    val now = System.currentTimeMillis()
                    val updated = session.copy(
                        isPaused = true,
                        lastPausedTimeMillis = now
                    )
                    db.gymDao().updateWorkoutSession(updated)

                    val openAppIntent = Intent(applicationContext, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        applicationContext, 0, openAppIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    val manager = getSystemService(NotificationManager::class.java)
                    manager.notify(1, buildNotification(session.name, "Pausiert", pendingIntent))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_WORKOUT") {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val workoutName = intent?.getStringExtra("WORKOUT_NAME") ?: "Training läuft..."
        val startTimeMillis = intent?.getLongExtra("START_TIME", System.currentTimeMillis())
            ?: System.currentTimeMillis()
        val accumulatedPause = intent?.getLongExtra("ACCUMULATED_PAUSE", 0L) ?: 0L
        val isPaused = intent?.getBooleanExtra("IS_PAUSED", false) ?: false

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        timerJob?.cancel()

        if (isPaused) {
            val notification = buildNotification(workoutName, "Pausiert", pendingIntent)
            startForeground(1, notification)
        } else {
            startForeground(1, buildNotification(workoutName, "00:00", pendingIntent))

            timerJob = serviceScope.launch {
                while (true) {
                    val elapsedSeconds =
                        (System.currentTimeMillis() - startTimeMillis - accumulatedPause) / 1000
                    val minutes = elapsedSeconds / 60
                    val seconds = elapsedSeconds % 60
                    val timeString =
                        String.format(Locale.getDefault(), "Dauer: %02d:%02d", minutes, seconds)

                    val notification = buildNotification(workoutName, timeString, pendingIntent)
                    val manager = getSystemService(NotificationManager::class.java)
                    manager.notify(1, notification)

                    delay(1000L)
                }
            }
        }
        return START_STICKY
    }

    private fun buildNotification(
        title: String,
        text: String,
        pendingIntent: PendingIntent?
    ): Notification {
        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }
        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, "Live Workout", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
    }
}