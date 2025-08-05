package com.ajaytechsolutions.goalr.ui.screens

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.ajaytechsolutions.goalr.MainActivity
import com.ajaytechsolutions.goalr.PermissionStates
import com.ajaytechsolutions.goalr.data.entities.DailyProgressEntity
import com.ajaytechsolutions.goalr.ui.components.WeeklyProgressView
import com.ajaytechsolutions.goalr.viewmodel.GoalrViewModel
import com.example.steptracker.ProgressCircleView
import java.util.*
import kotlin.math.min

@Composable
fun HomeScreen(
    viewModel: GoalrViewModel = hiltViewModel(),
    permissionStates: PermissionStates,
    onRequestActivityPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    // Show permission dialogs if needed
    PermissionDialogs(
        permissionStates = permissionStates,
        activity = activity,
        onRequestActivityPermission = onRequestActivityPermission,
        onRequestNotificationPermission = onRequestNotificationPermission,
        onOpenAppSettings = onOpenAppSettings
    )

    val stepsToday by viewModel.currentSteps.collectAsState()
    val user by viewModel.user.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val weeklyProgress by viewModel.weeklyProgress.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    val dailyGoal = user?.dailyGoal ?: 4000
    val userName = user?.name ?: "Welcome"

    // Get steps for selected date
    val selectedDateSteps = remember(selectedDate, weeklyProgress, stepsToday) {
        val calendar = Calendar.getInstance()
        val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val todayCal = Calendar.getInstance()

        val isToday = selectedCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                selectedCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

        if (isToday) {
            stepsToday
        } else {
            // Find matching date in weekly progress
            weeklyProgress.firstOrNull { progress ->
                val progressCal = Calendar.getInstance()
                val progressDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .parse(progress.date)
                progressDate?.let { progressCal.time = it }

                progressCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                        progressCal.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR)
            }?.steps ?: 0
        }
    }

    val progressPercentage = remember(selectedDateSteps, dailyGoal) {
        min((selectedDateSteps.toFloat() / dailyGoal * 100).toInt(), 100)
    }

    // Animation states
    var animatePulse by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animatePulse = true
    }

    // Brand colors - matching SwiftUI exactly
    val brandPrimary = Color(0xFF00B9BE)
    val brandSecondary = Color(0xFF009699)

    // Pulse animation for outer glow
    val pulseAlpha by animateFloatAsState(
        targetValue = if (animatePulse) 0.8f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF121212), // Dark theme background
                        Color(0xFF1A1A1A),
                        Color(0xFF181818)
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset.Infinite
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
        ) {
            // Permission warning banner (if needed)
            if (!permissionStates.activityRecognitionGranted) {
                PermissionWarningBanner(
                    onRequestPermission = onRequestActivityPermission,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // MARK: - Header Section
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = getGreetingText(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Text(
                            text = userName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Profile Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { /* Handle profile click */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            modifier = Modifier.size(24.dp),
                            tint = brandPrimary
                        )
                    }
                }
            }

            // MARK: - Main Progress Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Enhanced Progress Circle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Subtle outer glow - matching SwiftUI RadialGradient
                    Box(
                        modifier = Modifier
                            .size(320.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        brandPrimary.copy(alpha = 0.1f * pulseAlpha),
                                        Color.Transparent
                                    ),
                                    center = androidx.compose.ui.geometry.Offset.Zero,
                                    radius = 160f
                                ),
                                CircleShape
                            )
                    )

                    val isToday = remember(selectedDate) {
                        val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                        val todayCal = Calendar.getInstance()
                        selectedCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                                selectedCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
                    }

                    ProgressCircleView(
                        steps = selectedDateSteps,
                        goal = dailyGoal,
                        date = Date(selectedDate),
                        isSelectedToday = isToday,
                        animatePulse = animatePulse
                    )
                }

                // MARK: - Stats Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalFireDepartment,
                        iconColor = Color(0xFFFF9500), // Orange
                        title = "Streak",
                        value = "$streak",
                        subtitle = "days"
                    )

                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.GpsFixed, // Target icon
                        iconColor = brandPrimary,
                        title = "Progress",
                        value = "$progressPercentage%",
                        subtitle = "complete"
                    )

                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CalendarMonth,
                        iconColor = Color(0xFF9C27B0), // Purple
                        title = "Weekly",
                        value = formatSteps(getWeeklyAverage(weeklyProgress)),
                        subtitle = "avg steps"
                    )
                }

                // MARK: - Weekly Progress
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "This Week",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )

                        Text(
                            text = "Details",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = brandPrimary,
                            modifier = Modifier.clickable { /* Handle details click */ }
                        )
                    }

                    WeeklyProgressView(
                        weeklyProgress = weeklyProgress,
                        goal = dailyGoal,
                        selectedDate = selectedDate,
                        onDateSelected = { dateMillis ->
                            viewModel.onDateSelected(dateMillis)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // MARK: - Quick Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "Quick Actions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HomeActionButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.GpsFixed, // Target
                            title = "Set Goal",
                            color = brandPrimary
                        ) {
                            // Handle set goal
                        }

                        HomeActionButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.BarChart,
                            title = "Analytics",
                            color = Color(0xFF9C27B0) // Purple
                        ) {
                            // Handle analytics
                        }

                        HomeActionButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.People,
                            title = "Friends",
                            color = Color(0xFFFF9500) // Orange
                        ) {
                            // Handle friends
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun PermissionWarningBanner(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF6B35).copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, Color(0xFFFF6B35).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Activity tracking permission needed",
                    color = Color(0xFFFF6B35),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            TextButton(
                onClick = onRequestPermission,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFFFF6B35)
                )
            ) {
                Text(
                    text = "Grant",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    subtitle: String
) {
    Column(
        modifier = modifier
            .background(
                Color.White.copy(alpha = 0.03f),
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(16.dp)
            )
            .padding(vertical = 16.dp), // Reduced from 20dp
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp) // Reduced from 12dp
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(18.dp),
                tint = iconColor
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.6f)
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun HomeActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .background(
                Color.White.copy(alpha = 0.03f),
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(16.dp),
                tint = color
            )
        }

        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun PermissionDialogs(
    permissionStates: PermissionStates,
    activity: ComponentActivity,
    onRequestActivityPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    var showActivityPermissionRationale by remember { mutableStateOf(false) }
    var showNotificationPermissionRationale by remember { mutableStateOf(false) }
    var showActivityPermissionSettingsDialog by remember { mutableStateOf(false) }
    var showNotificationPermissionSettingsDialog by remember { mutableStateOf(false) }

    // Check for permission denial states
    LaunchedEffect(permissionStates) {
        // Activity recognition permission handling
        if (permissionStates.activityRecognitionAsked && !permissionStates.activityRecognitionGranted) {
            val shouldShowRationale = activity.shouldShowActivityRationale()
            if (shouldShowRationale) {
                showActivityPermissionRationale = true
            } else {
                showActivityPermissionSettingsDialog = true
            }
        }

        // Notification permission handling
        if (permissionStates.notificationAsked && !permissionStates.notificationGranted) {
            val shouldShowRationale = activity.shouldShowNotificationRationale()
            if (shouldShowRationale) {
                showNotificationPermissionRationale = true
            } else {
                showNotificationPermissionSettingsDialog = true
            }
        }
    }

    // Rationale dialogs
    if (showActivityPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showActivityPermissionRationale = false },
            title = {
                Text(
                    "Activity Recognition Permission Needed",
                    color = Color.White
                )
            },
            text = {
                Text(
                    "This app needs access to activity recognition to count your steps accurately.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showActivityPermissionRationale = false
                    onRequestActivityPermission()
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showActivityPermissionRationale = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    if (showNotificationPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showNotificationPermissionRationale = false },
            title = {
                Text(
                    "Notification Permission Needed",
                    color = Color.White
                )
            },
            text = {
                Text(
                    "This app needs permission to send notifications about your progress.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationPermissionRationale = false
                    onRequestNotificationPermission()
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationPermissionRationale = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    // Settings dialogs for permanently denied permissions
    if (showActivityPermissionSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showActivityPermissionSettingsDialog = false },
            title = {
                Text(
                    "Activity Recognition Permission Required",
                    color = Color.White
                )
            },
            text = {
                Text(
                    "Step tracking requires activity recognition permission. Please enable it in app settings.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showActivityPermissionSettingsDialog = false
                    onOpenAppSettings()
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showActivityPermissionSettingsDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    if (showNotificationPermissionSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationPermissionSettingsDialog = false },
            title = {
                Text(
                    "Notification Permission Denied",
                    color = Color.White
                )
            },
            text = {
                Text(
                    "To receive progress notifications, please enable notification permission in app settings.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationPermissionSettingsDialog = false
                    onOpenAppSettings()
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationPermissionSettingsDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }
}

// Extension functions to access MainActivity methods
private fun ComponentActivity.shouldShowActivityRationale(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            android.Manifest.permission.ACTIVITY_RECOGNITION
        )
    } else false
}

private fun ComponentActivity.shouldShowNotificationRationale(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
    } else false
}

// Helper functions
private fun getGreetingText(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..21 -> "Good Evening"
        else -> "Good Night"
    }
}

private fun getWeeklyAverage(weeklyProgress: List<DailyProgressEntity>): Int {
    if (weeklyProgress.isEmpty()) return 0
    val total = weeklyProgress.sumOf { it.steps }
    return total / weeklyProgress.size
}

private fun formatSteps(steps: Int): String {
    return if (steps >= 1000) {
        String.format("%.1fk", steps / 1000.0)
    } else {
        steps.toString()
    }
}