package com.ajaytechsolutions.goalr.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ajaytechsolutions.goalr.data.GoalrDatabase
import com.ajaytechsolutions.goalr.manager.StepCounterManager
import com.ajaytechsolutions.goalr.services.StepCounterService
import com.ajaytechsolutions.goalr.util.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("ServiceAutoStarter", "Received broadcast: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                startStepServiceIfPermissionsGranted(context)
            }
        }
    }

    private fun startStepServiceIfPermissionsGranted(context: Context) {
        // Only start service if we have the necessary permissions
        if (PermissionManager.areAllCriticalPermissionsGranted(context)) {
            try {
                val serviceIntent = Intent(context, StepCounterService::class.java).apply {
                    action = StepCounterService.ACTION_START_SERVICE
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                android.util.Log.i("ServiceAutoStarter", "Step counter service started after boot")
            } catch (e: Exception) {
                android.util.Log.e("ServiceAutoStarter", "Failed to start service after boot", e)
            }
        } else {
            android.util.Log.w("ServiceAutoStarter", "Cannot start service - missing permissions")
            PermissionManager.logPermissionStatus(context)
        }
    }

}
