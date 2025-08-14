package com.ajaytechsolutions.goalr.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ajaytechsolutions.goalr.data.GoalrDatabase
import com.ajaytechsolutions.goalr.data.entities.DailyProgressEntity
import com.ajaytechsolutions.goalr.util.StepTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Simplified StepWorker that can serve as a backup to the foreground service
 * or handle periodic database synchronization tasks.
 *
 * This worker is now optional since the foreground service handles primary step tracking.
 */
class StepWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override suspend fun doWork(): Result {
        android.util.Log.d("StepWorker", "Backup worker started at ${System.currentTimeMillis()}")

        try {
            // This can serve as a backup sync mechanism
            // The foreground service should be handling primary step tracking
            performBackupSync()

            return Result.success()
        } catch (e: Exception) {
            android.util.Log.e("StepWorker", "Worker failed: ${e.message}")
            return Result.retry()
        }
    }

    /**
     * Performs backup synchronization tasks
     * This could include data validation, cleanup, or failsafe operations
     */
    private suspend fun performBackupSync() {
        val dao = GoalrDatabase.getDatabase(applicationContext).goalrDao()
        val today = getToday()

        // Check if we have recent data
        val progress = withContext(Dispatchers.IO) { dao.getDailyProgress(today) }

        if (progress != null) {
            android.util.Log.d("StepWorker", "Backup sync found ${progress.steps} steps for $today")

            // Emit current steps to ensure UI is in sync
            StepTracker.emitSteps(progress.steps)
        } else {
            android.util.Log.d("StepWorker", "No progress data found for $today")

            // Initialize with 0 steps if no data exists
            StepTracker.emitSteps(0)
        }
    }

    private fun getToday(): String = dateFormatter.format(Date())
}