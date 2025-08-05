package com.ajaytechsolutions.goalr.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ajaytechsolutions.goalr.data.GoalrDatabase
import com.ajaytechsolutions.goalr.data.entities.DailyProgressEntity
import com.ajaytechsolutions.goalr.manager.StepCounterManager
import com.ajaytechsolutions.goalr.util.StepTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class StepWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val stepCounterManager = StepCounterManager(context)
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override suspend fun doWork(): Result {
        android.util.Log.d("StepWorker", "Worker started at ${System.currentTimeMillis()}")

        // Load last saved steps from database
        val dao = GoalrDatabase.getDatabase(applicationContext).goalrDao()
        val today = getToday()
        val progress = withContext(Dispatchers.IO) { dao.getDailyProgress(today) }
        val lastSavedSteps = progress?.steps ?: 0
        android.util.Log.d("StepWorker", "Last saved steps for $today: $lastSavedSteps")

        // Start tracking with initial steps
        stepCounterManager.startTracking(lastSavedSteps)
        android.util.Log.d("StepWorker", "StepCounterManager started: ${stepCounterManager.getSensorInfo()}")

        // Collect step updates for 60 seconds
        try {
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                stepCounterManager.stepsToday
                    .takeWhile { System.currentTimeMillis() - startTime < 60_000L }
                    .collect { steps ->
                        android.util.Log.d("StepWorker", "Received steps: $steps")
                        if (steps > lastSavedSteps) {
                            saveStepsToDatabase(steps)
                            StepTracker.emitSteps(steps)
                        }
                    }
            }
        } catch (e: Exception) {
            android.util.Log.e("StepWorker", "Error collecting steps: ${e.message}")
        } finally {
            stepCounterManager.stopTracking()
            android.util.Log.d("StepWorker", "StepCounterManager stopped")
        }

        // Reschedule the worker
        StepWorkerScheduler.schedule(applicationContext)
        android.util.Log.d("StepWorker", "Worker rescheduled")

        return Result.success()
    }

    private suspend fun saveStepsToDatabase(steps: Int) {
        withContext(Dispatchers.IO) {
            android.util.Log.d("StepWorker", "Saving steps to DB: $steps")
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
        }
    }

    private fun getToday(): String = dateFormatter.format(Date())
}