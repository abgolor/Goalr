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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StepCounterService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private lateinit var stepCounterManager: StepCounterManager
    private var lastSavedSteps = 0

    // Binder for activity communication
    private val binder = StepCounterBinder()

    inner class StepCounterBinder : Binder() {
        fun getService(): StepCounterService = this@StepCounterService
    }

    override fun onCreate() {
        super.onCreate()

        stepCounterManager = StepCounterManager(applicationContext)

        startForegroundService()
        initializeStepTracking()
    }

    private fun startForegroundService() {
        val channelId = "step_counter_channel"
        val channelName = "Step Counter Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your daily steps in the background"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = createStepNotification(0)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createStepNotification(steps: Int): Notification {
        val channelId = "step_counter_channel"
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Goalr Step Tracker")
            .setContentText("Steps today: $steps")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun initializeStepTracking() {
        serviceScope.launch {
            // Load initial steps from database
            val initialSteps = loadInitialSteps()

            // Start step counter manager with initial steps
            stepCounterManager.startTracking(initialSteps)

            // Listen for step updates
            stepCounterManager.stepsToday
                .collect { steps ->
                    handleStepUpdate(steps)
                }
        }
    }

    private suspend fun loadInitialSteps(): Int {
        try {
            val dao = GoalrDatabase.getDatabase(applicationContext).goalrDao()
            val today = getToday()
            val progress = withContext(Dispatchers.IO) {
                dao.getDailyProgress(today)
            }
            lastSavedSteps = progress?.steps ?: 0

            android.util.Log.d("StepService", "Loaded initial steps: $lastSavedSteps for $today")
            return lastSavedSteps
        } catch (e: Exception) {
            android.util.Log.e("StepService", "Error loading initial steps", e)
            lastSavedSteps = 0
            return 0
        }
    }

    private fun handleStepUpdate(steps: Int) {
        android.util.Log.d("StepService", "Step update received: $steps")

        // Update notification
        updateNotification(steps)

        // Emit steps to shared tracker for UI updates
        StepTracker.emitSteps(steps)

        // Save to database periodically (every 10 steps or every 5 minutes)
        if (shouldSaveSteps(steps)) {
            serviceScope.launch {
                saveStepsToDatabase(steps)
            }
        }
    }

    private fun shouldSaveSteps(currentSteps: Int): Boolean {
        val stepDifference = currentSteps - lastSavedSteps
        val timeDifference = System.currentTimeMillis() - getLastSaveTime()

        return stepDifference >= 10 || timeDifference >= 300_000 // 5 minutes
    }

    private fun updateNotification(steps: Int) {
        val notification = createStepNotification(steps)
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    private suspend fun saveStepsToDatabase(steps: Int) {
        try {
            withContext(Dispatchers.IO) {
                val dao = GoalrDatabase.getDatabase(applicationContext).goalrDao()
                val today = getToday()
                val user = dao.getUser()
                val dailyGoal = user?.dailyGoal ?: 4000

                dao.saveDailyProgress(
                    DailyProgressEntity(
                        date = today,
                        steps = steps,
                        goalMet = steps >= dailyGoal
                    )
                )

                lastSavedSteps = steps
                setLastSaveTime(System.currentTimeMillis())

                android.util.Log.d("StepService", "Saved $steps steps to database")
            }
        } catch (e: Exception) {
            android.util.Log.e("StepService", "Error saving steps to database", e)
        }
    }

    // Simple timestamp tracking for save intervals
    private var lastSaveTime = System.currentTimeMillis()

    private fun getLastSaveTime(): Long = lastSaveTime
    private fun setLastSaveTime(time: Long) { lastSaveTime = time }

    private fun getToday(): String = dateFormatter.format(Date())

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.i("StepService", "Service started with command")
        return START_STICKY // Restart service if killed
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.i("StepService", "Service destroyed")

        // Save final step count before stopping
        serviceScope.launch {
            val currentSteps = stepCounterManager.getCurrentSteps()
            saveStepsToDatabase(currentSteps)
        }

        stepCounterManager.stopTracking()
        serviceScope.cancel()
    }

    // Public methods for activity interaction
    fun getCurrentSteps(): Int = stepCounterManager.getCurrentSteps()

    fun getDetectionInfo(): String = stepCounterManager.getDetectionInfo()

    companion object {
        private const val NOTIFICATION_ID = 1

        const val ACTION_START_SERVICE = "com.ajaytechsolutions.goalr.START_STEP_SERVICE"
        const val ACTION_STOP_SERVICE = "com.ajaytechsolutions.goalr.STOP_STEP_SERVICE"
    }
}