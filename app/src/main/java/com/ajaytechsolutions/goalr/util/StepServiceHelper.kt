package com.ajaytechsolutions.goalr.util

import android.content.Context
import android.content.Intent
import android.os.Build
import com.ajaytechsolutions.goalr.services.StepCounterService

/**
 * Helper class for managing the StepCounterService lifecycle
 */
object StepServiceHelper {

    /**
     * Starts the step counter service if not already running
     */
    fun startStepService(context: Context) {
        val intent = Intent(context, StepCounterService::class.java).apply {
            action = StepCounterService.ACTION_START_SERVICE
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            android.util.Log.i("StepServiceHelper", "Step service start requested")
        } catch (e: Exception) {
            android.util.Log.e("StepServiceHelper", "Failed to start step service", e)
        }
    }

    /**
     * Stops the step counter service
     */
    fun stopStepService(context: Context) {
        val intent = Intent(context, StepCounterService::class.java).apply {
            action = StepCounterService.ACTION_STOP_SERVICE
        }
        
        try {
            context.stopService(intent)
            android.util.Log.i("StepServiceHelper", "Step service stop requested")
        } catch (e: Exception) {
            android.util.Log.e("StepServiceHelper", "Failed to stop step service", e)
        }
    }

    /**
     * Checks if the service is running (simplified check)
     * Note: This is a basic implementation. For production, consider using 
     * service binding or other mechanisms for accurate service state detection.
     */
    fun isServiceRunning(context: Context): Boolean {
        // This is a simplified implementation
        // In practice, you might want to use service binding or other methods
        // to accurately check service status
        return true // Placeholder - implement as needed
    }
}