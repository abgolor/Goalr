package com.ajaytechsolutions.goalr

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ajaytechsolutions.goalr.services.StepCounterService
import com.ajaytechsolutions.goalr.ui.screens.HomeScreen
import com.ajaytechsolutions.goalr.ui.screens.PermissionScreen
import com.ajaytechsolutions.goalr.ui.screens.RegistrationScreen
import com.ajaytechsolutions.goalr.ui.screens.SplashScreen
import com.ajaytechsolutions.goalr.ui.theme.GoalrTheme
import com.ajaytechsolutions.goalr.viewmodel.GoalrViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: GoalrViewModel by viewModels()

    // Track permission states - activity recognition, notification, and battery optimization
    private var _permissionStates = mutableStateOf(PermissionStates())
    val permissionStates: State<PermissionStates> = _permissionStates

    // Service connection for step counter service
    private var stepService: StepCounterService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as StepCounterService.StepCounterBinder
            stepService = binder.getService()
            isServiceBound = true
            android.util.Log.d("MainActivity", "Step service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            stepService = null
            isServiceBound = false
            android.util.Log.d("MainActivity", "Step service disconnected")
        }
    }

    // Permission launchers
    private val requestActivityPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            android.util.Log.d("MainActivity", "Activity permission result: $granted")
            updatePermissionState { currentState ->
                currentState.copy(
                    activityRecognitionGranted = granted,
                    activityRecognitionAsked = true
                )
            }
            checkAndStartService()
        }

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            android.util.Log.d("MainActivity", "Notification permission result: $granted")
            updatePermissionState { currentState ->
                currentState.copy(
                    notificationGranted = granted,
                    notificationAsked = true
                )
            }
            checkAndStartService()
        }

    // Battery optimization result launcher
    private val batteryOptimizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Check battery optimization status when returning
        android.util.Log.d("MainActivity", "Battery optimization result received")
        updatePermissionState { currentState ->
            currentState.copy(
                batteryOptimizationDisabled = isBatteryOptimizationDisabled(),
                batteryOptimizationAsked = true
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize permission states
        initializePermissionStates()

        setContent {
            GoalrTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val userState = viewModel.user.collectAsState()

                    // Always start with splash screen
                    val startDestination = "splash"

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("splash") {
                            SplashScreen {
                                val destination = when {
                                    userState.value == null -> "registration"
                                    !areRequiredPermissionsGranted() -> "permissions"
                                    else -> "home"
                                }
                                navController.navigate(destination) {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        }

                        composable("registration") {
                            RegistrationScreen(
                                onRegistrationComplete = {
                                    // After registration, check permissions
                                    val destination = if (!areRequiredPermissionsGranted()) {
                                        "permissions"
                                    } else {
                                        "home"
                                    }
                                    navController.navigate(destination) {
                                        popUpTo("registration") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("permissions") {
                            PermissionScreen(
                                permissionStates = permissionStates.value,
                                onRequestActivityPermission = { requestActivityPermission() },
                                onRequestNotificationPermission = { requestNotificationPermission() },
                                onRequestBatteryOptimization = { requestBatteryOptimizationExemption() },
                                onOpenAppSettings = { openAppSettings() },
                                onOpenBatterySettings = { openBatteryOptimizationSettings() },
                                onPermissionsGranted = {
                                    navController.navigate("home") {
                                        popUpTo("permissions") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                permissionStates = permissionStates.value,
                                onRequestActivityPermission = { requestActivityPermission() },
                                onRequestNotificationPermission = { requestNotificationPermission() },
                                onRequestBatteryOptimization = { requestBatteryOptimizationExemption() },
                                onOpenAppSettings = { openAppSettings() },
                                onOpenBatterySettings = { openBatteryOptimizationSettings() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Helper function to update permission state and trigger recomposition
    private fun updatePermissionState(update: (PermissionStates) -> PermissionStates) {
        val newState = update(_permissionStates.value)
        android.util.Log.d("MainActivity", "Permission state updated: $newState")
        _permissionStates.value = newState
    }

    private fun initializePermissionStates() {
        val activityGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not needed for older versions
        }

        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not needed for older versions
        }

        val batteryOptimizationDisabled = isBatteryOptimizationDisabled()

        _permissionStates.value = PermissionStates(
            activityRecognitionGranted = activityGranted,
            notificationGranted = notificationGranted,
            batteryOptimizationDisabled = batteryOptimizationDisabled,
            activityRecognitionAsked = activityGranted,
            notificationAsked = notificationGranted,
            batteryOptimizationAsked = batteryOptimizationDisabled
        )

        android.util.Log.d("MainActivity", "Initial permission states: ${_permissionStates.value}")

        // Start service if permissions are already granted
        if (areRequiredPermissionsGranted()) {
            checkAndStartService()
        }
    }

    private fun areRequiredPermissionsGranted(): Boolean {
        val activityGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            _permissionStates.value.activityRecognitionGranted
        } else {
            true
        }

        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            _permissionStates.value.notificationGranted
        } else {
            true
        }

        return activityGranted && notificationGranted
    }

    private fun checkAndStartService() {
        val canStart = areRequiredPermissionsGranted()

        if (canStart) {
            startStepCounterService()

            // Check battery optimization (non-blocking)
            if (!_permissionStates.value.batteryOptimizationDisabled) {
                updatePermissionState { currentState ->
                    currentState.copy(batteryOptimizationDisabled = isBatteryOptimizationDisabled())
                }
            }
        }
    }

    fun requestActivityPermission() {
        android.util.Log.d("MainActivity", "Requesting activity permission...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestActivityPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            // For older versions, just mark as granted
            updatePermissionState { currentState ->
                currentState.copy(
                    activityRecognitionGranted = true,
                    activityRecognitionAsked = true
                )
            }
        }
    }

    fun requestNotificationPermission() {
        android.util.Log.d("MainActivity", "Requesting notification permission...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // For older versions, just mark as granted
            updatePermissionState { currentState ->
                currentState.copy(
                    notificationGranted = true,
                    notificationAsked = true
                )
            }
        }
    }

    private fun isBatteryOptimizationDisabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            true // Not applicable for older versions
        }
    }

    fun requestBatteryOptimizationExemption() {
        android.util.Log.d("MainActivity", "Requesting battery optimization exemption...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            try {
                batteryOptimizationLauncher.launch(intent)
                updatePermissionState { currentState ->
                    currentState.copy(batteryOptimizationAsked = true)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to open battery optimization settings", e)
                openBatteryOptimizationSettings()
            }
        } else {
            // For older versions, just mark as disabled
            updatePermissionState { currentState ->
                currentState.copy(
                    batteryOptimizationDisabled = true,
                    batteryOptimizationAsked = true
                )
            }
        }
    }

    fun openBatteryOptimizationSettings() {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to open battery optimization settings", e)
            openAppSettings()
        }
    }

    private fun startStepCounterService() {
        val intent = Intent(this, StepCounterService::class.java).apply {
            action = StepCounterService.ACTION_START_SERVICE
        }

        // Start as foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // Bind to service for communication
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        android.util.Log.i("MainActivity", "Step counter service started")
    }

    fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()

        android.util.Log.d("MainActivity", "onResume - refreshing permission states")

        // Refresh all permission statuses when returning to app
        val activityGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val batteryOptimizationDisabled = isBatteryOptimizationDisabled()

        updatePermissionState { currentState ->
            currentState.copy(
                activityRecognitionGranted = activityGranted,
                notificationGranted = notificationGranted,
                batteryOptimizationDisabled = batteryOptimizationDisabled
            )
        }

        // Start service if permissions are now available
        checkAndStartService()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Unbind from service
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }

        // Note: We don't stop the service here as it should continue running
        // to track steps even when the app is closed
    }

    // Helper methods for permission checking
    fun shouldShowActivityRationale(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            shouldShowRequestPermissionRationale(Manifest.permission.ACTIVITY_RECOGNITION)
        } else false
    }

    fun shouldShowNotificationRationale(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        } else false
    }

    // Public method to get step service (for debugging or direct access)
    fun getStepService(): StepCounterService? = stepService
}

// Complete PermissionStates data class with all required properties
data class PermissionStates(
    val activityRecognitionGranted: Boolean = false,
    val activityRecognitionAsked: Boolean = false,
    val notificationGranted: Boolean = false,
    val notificationAsked: Boolean = false,
    val batteryOptimizationDisabled: Boolean = false,
    val batteryOptimizationAsked: Boolean = false
)