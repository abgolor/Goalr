package com.ajaytechsolutions.goalr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ajaytechsolutions.goalr.ui.screens.HomeScreen
import com.ajaytechsolutions.goalr.ui.screens.RegistrationScreen
import com.ajaytechsolutions.goalr.ui.screens.SplashScreen
import com.ajaytechsolutions.goalr.ui.theme.GoalrTheme
import com.ajaytechsolutions.goalr.viewmodel.GoalrViewModel
import com.ajaytechsolutions.goalr.worker.StepWorkerScheduler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: GoalrViewModel by viewModels()

    // Track permission states
    private var _permissionStates = mutableStateOf(PermissionStates())
    val permissionStates: State<PermissionStates> = _permissionStates

    // Permission launchers
    private val requestActivityPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            _permissionStates.value = _permissionStates.value.copy(
                activityRecognitionGranted = granted,
                activityRecognitionAsked = true
            )
            if (granted) {
                scheduleStepWorker()
            }
        }

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            _permissionStates.value = _permissionStates.value.copy(
                notificationGranted = granted,
                notificationAsked = true
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize permission states
        initializePermissionStates()

        // Request permissions on first launch
        requestPermissionsIfNeeded()

        setContent {
            GoalrTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val userState = viewModel.user.collectAsState()
                    val startDestination = if (userState.value != null) "home" else "splash"

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("splash") {
                            SplashScreen {
                                navController.navigate("registration") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        }
                        composable("registration") {
                            RegistrationScreen(navController = navController)
                        }
                        composable("home") {
                            HomeScreen(
                                permissionStates = permissionStates.value,
                                onRequestActivityPermission = { requestActivityPermission() },
                                onRequestNotificationPermission = { requestNotificationPermission() },
                                onOpenAppSettings = { openAppSettings() }
                            )
                        }
                    }
                }
            }
        }
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

        _permissionStates.value = PermissionStates(
            activityRecognitionGranted = activityGranted,
            notificationGranted = notificationGranted,
            activityRecognitionAsked = activityGranted,
            notificationAsked = notificationGranted
        )

        // Schedule worker if activity permission is already granted
        if (activityGranted) {
            scheduleStepWorker()
        }
    }

    private fun requestPermissionsIfNeeded() {
        // Request activity recognition first (more critical for core functionality)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !_permissionStates.value.activityRecognitionGranted) {
            requestActivityPermission()
        }

        // Request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !_permissionStates.value.notificationGranted) {
            requestNotificationPermission()
        }
    }

    private fun requestActivityPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestActivityPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun scheduleStepWorker() {
        StepWorkerScheduler.schedule(this)
    }

    fun shouldShowActivityRationale(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        } else false
    }

    fun shouldShowNotificationRationale(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else false
    }
}

data class PermissionStates(
    val activityRecognitionGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val activityRecognitionAsked: Boolean = false,
    val notificationAsked: Boolean = false
)