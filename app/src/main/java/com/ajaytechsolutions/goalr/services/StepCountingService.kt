package com.ajaytechsolutions.goalr.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ajaytechsolutions.goalr.R
import com.ajaytechsolutions.goalr.data.GoalrDatabase
import com.ajaytechsolutions.goalr.data.entities.DailyProgressEntity
import com.ajaytechsolutions.goalr.manager.StepCounterManager
import com.ajaytechsolutions.goalr.util.StepTracker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class StepCounterService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    override fun onCreate() {
        super.onCreate()
        startForegroundService()

        // Listen to step updates from Worker
        serviceScope.launch {
            StepTracker.stepUpdates.collect { steps ->
                updateNotification(steps)
            }
        }
    }

    private fun startForegroundService() {
        val channelId = "step_counter_channel"
        val channelName = "Step Counter Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Goalr is tracking steps")
            .setContentText("Current Steps: 0")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun updateNotification(steps: Int) {
        val channelId = "step_counter_channel"
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Goalr is tracking steps")
            .setContentText("Current Steps: $steps")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        getSystemService(NotificationManager::class.java).notify(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
