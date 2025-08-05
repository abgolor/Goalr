package com.ajaytechsolutions.goalr.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object StepWorkerScheduler {
    private const val UNIQUE_WORK_TAG = "StepWorker"

    fun schedule(context: Context) {
        val request = OneTimeWorkRequestBuilder<StepWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_WORK_TAG, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_TAG)
    }
}
