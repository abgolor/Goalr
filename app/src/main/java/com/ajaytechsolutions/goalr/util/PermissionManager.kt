package com.ajaytechsolutions.goalr.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Comprehensive permission management utility for step tracking app
 * Handles all permission states and provides helper methods for checking and requesting permissions
 */
object PermissionManager {

    /**
     * Checks if activity recognition permission is granted
     */
    fun isActivityRecognitionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required for older versions
        }
    }

    /**
     * Checks if notification permission is granted
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required for older versions
        }
    }

    /**
     * Checks if app is exempt from battery optimization
     */
    fun isBatteryOptimizationDisabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // Not applicable for older versions
        }
    }

    /**
     * Checks if all critical permissions are granted for foreground service
     */
    fun areAllCriticalPermissionsGranted(context: Context): Boolean {
        return isActivityRecognitionGranted(context) && 
               isNotificationPermissionGranted(context)
    }

    /**
     * Checks if all permissions (including battery optimization) are granted
     */
    fun areAllPermissionsOptimal(context: Context): Boolean {
        return areAllCriticalPermissionsGranted(context) && 
               isBatteryOptimizationDisabled(context)
    }

    /**
     * Opens battery optimization settings for the app
     */
    fun requestBatteryOptimizationExemption(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            try {
                if (context is Activity) {
                    context.startActivity(intent)
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("PermissionManager", "Failed to open battery optimization request", e)
                openBatteryOptimizationSettings(context)
            }
        }
    }

    /**
     * Opens general battery optimization settings
     */
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("PermissionManager", "Failed to open battery settings", e)
            openAppSettings(context)
        }
    }

    /**
     * Opens app-specific settings page
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Gets user-friendly permission status message
     */
    fun getPermissionStatusMessage(context: Context): String {
        val activityGranted = isActivityRecognitionGranted(context)
        val notificationGranted = isNotificationPermissionGranted(context)
        val batteryOptimized = isBatteryOptimizationDisabled(context)

        return when {
            !activityGranted -> "Activity recognition permission required"
            !notificationGranted -> "Notification permission required for background service"
            !batteryOptimized -> "Battery optimization should be disabled for best performance"
            else -> "All permissions granted - optimal setup"
        }
    }

    /**
     * Gets the next required permission to request
     */
    fun getNextPermissionToRequest(context: Context): String? {
        return when {
            !isActivityRecognitionGranted(context) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Manifest.permission.ACTIVITY_RECOGNITION
                } else null
            }
            !isNotificationPermissionGranted(context) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else null
            }
            else -> null
        }
    }

    /**
     * Checks if we should show rationale for activity permission
     */
    fun shouldShowActivityRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.shouldShowRequestPermissionRationale(Manifest.permission.ACTIVITY_RECOGNITION)
        } else false
    }

    /**
     * Checks if we should show rationale for notification permission
     */
    fun shouldShowNotificationRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        } else false
    }

    /**
     * Creates comprehensive permission state data class
     */
    fun createPermissionStates(context: Context): com.ajaytechsolutions.goalr.PermissionStates {
        return com.ajaytechsolutions.goalr.PermissionStates(
            activityRecognitionGranted = isActivityRecognitionGranted(context),
            notificationGranted = isNotificationPermissionGranted(context),
            batteryOptimizationDisabled = isBatteryOptimizationDisabled(context),
            activityRecognitionAsked = isActivityRecognitionGranted(context),
            notificationAsked = isNotificationPermissionGranted(context),
            batteryOptimizationAsked = isBatteryOptimizationDisabled(context)
        )
    }

    /**
     * Logs current permission status for debugging
     */
    fun logPermissionStatus(context: Context) {
        android.util.Log.d("PermissionManager", """
            Permission Status:
            - Activity Recognition: ${isActivityRecognitionGranted(context)}
            - Notifications: ${isNotificationPermissionGranted(context)}
            - Battery Optimization Disabled: ${isBatteryOptimizationDisabled(context)}
            - All Critical Granted: ${areAllCriticalPermissionsGranted(context)}
            - All Optimal: ${areAllPermissionsOptimal(context)}
        """.trimIndent())
    }
}