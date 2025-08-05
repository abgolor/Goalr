package com.ajaytechsolutions.goalr.app

import android.app.Application
import com.ajaytechsolutions.goalr.worker.StepWorkerScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GoalrApp : Application(){
    override fun onCreate() {
        super.onCreate()
        StepWorkerScheduler.schedule(this)
    }
}
