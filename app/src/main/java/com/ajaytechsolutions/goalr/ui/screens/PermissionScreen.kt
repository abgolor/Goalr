package com.ajaytechsolutions.goalr.ui.screens

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajaytechsolutions.goalr.MainActivity
import com.ajaytechsolutions.goalr.PermissionStates
import kotlinx.coroutines.delay

@Composable
fun PermissionScreen(
    permissionStates: PermissionStates,
    onRequestActivityPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    // Animation states
    var currentPermissionIndex by remember { mutableStateOf(0) }
    var showContent by remember { mutableStateOf(false) }
    var animateElements by remember { mutableStateOf(false) }

    // Brand colors matching your design
    val brandPrimary = Color(0xFF00B9BE)
    val brandSecondary = Color(0xFF009699)

    // Define permissions to check
    val permissions = remember {
        buildList {
            // Only add Activity Tracking permission if Android 10+ (API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(PermissionData(
                    title = "Activity Tracking",
                    description = "Track your steps and physical activity throughout the day",
                    detailedDescription = "This permission allows Goalr to count your steps automatically using your device's built-in sensors. We use this data to help you reach your daily fitness goals.",
                    icon = Icons.Default.DirectionsRun,
                    color = brandPrimary,
                    onGrant = onRequestActivityPermission,
                    isRequired = true,
                    needsAndroidVersion = true
                ))
            }

            // Only add Notification permission if Android 13+ (API 33+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(PermissionData(
                    title = "Notifications",
                    description = "Receive motivational updates and goal achievements",
                    detailedDescription = "Get notified when you reach milestones, achieve your daily goals, or need encouragement to stay active. All notifications can be customized.",
                    icon = Icons.Default.Notifications,
                    color = Color(0xFF9C27B0),
                    onGrant = onRequestNotificationPermission,
                    isRequired = true,
                    needsAndroidVersion = true
                ))
            }

            // Only add Battery Optimization if Android 6+ (API 23+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                add(PermissionData(
                    title = "Battery Optimization",
                    description = "Ensure accurate step tracking in the background",
                    detailedDescription = "Disable battery optimization to ensure Goalr can track your steps accurately even when the app is closed. This helps maintain consistent step counting.",
                    icon = Icons.Default.Battery4Bar,
                    color = Color(0xFF4CAF50),
                    onGrant = onRequestBatteryOptimization,
                    isRequired = false,
                    needsAndroidVersion = true
                ))
            }
        }
    }

    // Get current permission to display
    val currentPermission = permissions.getOrNull(currentPermissionIndex)

    // Check if all required permissions are granted - use individual boolean values for proper reactivity
    val allRequiredGranted = remember(
        permissionStates.activityRecognitionGranted,
        permissionStates.notificationGranted
    ) {
        permissions.filter { it.isRequired }.all { getIsGranted(it, permissionStates) }
    }

    // All permissions including optional ones
    val allPermissionsGranted = remember(
        permissionStates.activityRecognitionGranted,
        permissionStates.notificationGranted,
        permissionStates.batteryOptimizationDisabled
    ) {
        permissions.all { getIsGranted(it, permissionStates) }
    }

    // Handle automatic progression to next permission - watch individual permission states
    LaunchedEffect(
        permissionStates.activityRecognitionGranted,
        permissionStates.notificationGranted,
        permissionStates.batteryOptimizationDisabled,
        currentPermissionIndex // Add this to ensure LaunchedEffect reruns when we manually change index
    ) {
        android.util.Log.d("PermissionScreen", "Permission states changed: $permissionStates")
        android.util.Log.d("PermissionScreen", "Current permission index: $currentPermissionIndex")

        if (allPermissionsGranted) {
            // All permissions granted, proceed to home
            android.util.Log.d("PermissionScreen", "All permissions granted, proceeding to home")
            delay(1500) // Show success state briefly
            onPermissionsGranted()
            return@LaunchedEffect
        }

        // Find the first ungranted permission
        val nextUngranted = permissions.indexOfFirst { !getIsGranted(it, permissionStates) }
        android.util.Log.d("PermissionScreen", "Next ungranted permission index: $nextUngranted, current: $currentPermissionIndex")
        android.util.Log.d("PermissionScreen", "Permission states: Activity=${permissionStates.activityRecognitionGranted}, Notification=${permissionStates.notificationGranted}, Battery=${permissionStates.batteryOptimizationDisabled}")
        android.util.Log.d("PermissionScreen", "Total permissions available: ${permissions.size}")
        permissions.forEachIndexed { index, perm ->
            android.util.Log.d("PermissionScreen", "Permission $index: ${perm.title} - Granted: ${getIsGranted(perm, permissionStates)}")
        }

        if (nextUngranted != -1 && nextUngranted != currentPermissionIndex) {
            // Add a small delay to show the granted state briefly
            delay(800)
            android.util.Log.d("PermissionScreen", "Moving to permission index: $nextUngranted")
            currentPermissionIndex = nextUngranted
        } else if (nextUngranted == -1) {
            // No ungranted permissions found, proceed to home
            android.util.Log.d("PermissionScreen", "No ungranted permissions found, proceeding to home")
            delay(1500)
            onPermissionsGranted()
        }
    }

    // Handle completion when all required permissions are granted but optional ones remain
    LaunchedEffect(allRequiredGranted) {
        android.util.Log.d("PermissionScreen", "Required permissions check - allRequiredGranted: $allRequiredGranted")
        if (allRequiredGranted) {
            android.util.Log.d("PermissionScreen", "All required permissions granted")
            // Check if there are any optional permissions left to show
            val hasUnGrantedOptional = permissions.any { !it.isRequired && !getIsGranted(it, permissionStates) }

            if (!hasUnGrantedOptional) {
                // No optional permissions left, go to home
                android.util.Log.d("PermissionScreen", "No optional permissions left, proceeding to home")
                delay(1500) // Show success state briefly
                onPermissionsGranted()
            } else {
                // Show optional permissions
                val nextOptional = permissions.indexOfFirst { !it.isRequired && !getIsGranted(it, permissionStates) }
                if (nextOptional != -1 && nextOptional != currentPermissionIndex) {
                    delay(800)
                    android.util.Log.d("PermissionScreen", "Moving to optional permission index: $nextOptional")
                    currentPermissionIndex = nextOptional
                }
            }
        }
    }

    // Backup effect to force progression if main effect doesn't work
    LaunchedEffect(permissionStates) {
        delay(1000) // Wait a bit to let main effect handle it
        val nextUngranted = permissions.indexOfFirst { !getIsGranted(it, permissionStates) }
        android.util.Log.d("PermissionScreen", "Backup effect - nextUngranted: $nextUngranted, current: $currentPermissionIndex")

        if (nextUngranted == -1) {
            // All permissions granted
            android.util.Log.d("PermissionScreen", "Backup effect: All permissions granted, proceeding to home")
            onPermissionsGranted()
        } else if (nextUngranted != currentPermissionIndex) {
            android.util.Log.d("PermissionScreen", "Backup effect: Forcing move to index $nextUngranted")
            currentPermissionIndex = nextUngranted
        }
    }

    LaunchedEffect(Unit) {
        // Set initial permission to first ungranted one
        val firstUngranted = permissions.indexOfFirst { !getIsGranted(it, permissionStates) }
        if (firstUngranted != -1) {
            currentPermissionIndex = firstUngranted
            android.util.Log.d("PermissionScreen", "Initial permission index: $firstUngranted")
        }

        showContent = true
        delay(300)
        animateElements = true
    }

    // Scale animation for the main content
    val contentScale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "contentScale"
    )

    // Pulse animation for granted state
    val pulseScale by animateFloatAsState(
        targetValue = if (currentPermission?.let { getIsGranted(it, permissionStates) } == true && animateElements) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF121212),
                        Color(0xFF1A1A1A),
                        Color(0xFF181818)
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset.Infinite
                )
            )
            .statusBarsPadding()
    ) {
        currentPermission?.let { permission ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(contentScale)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section with progress and header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    AnimatedVisibility(
                        visible = animateElements,
                        enter = slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = tween(800, easing = EaseOutCubic)
                        ) + fadeIn(tween(800))
                    ) {
                        // Progress indicator
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            permissions.forEachIndexed { index, permissionData ->
                                Box(
                                    modifier = Modifier
                                        .size(if (index == currentPermissionIndex) 12.dp else 8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                getIsGranted(permissionData, permissionStates) -> Color(0xFF4CAF50)
                                                index == currentPermissionIndex -> brandPrimary
                                                else -> Color.White.copy(alpha = 0.3f)
                                            }
                                        )
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = animateElements,
                        enter = slideInVertically(
                            initialOffsetY = { -it/2 },
                            animationSpec = tween(1000, delayMillis = 200, easing = EaseOutCubic)
                        ) + fadeIn(tween(1000, delayMillis = 200))
                    ) {
                        Text(
                            text = when {
                                allPermissionsGranted -> "All Set! 🎉"
                                allRequiredGranted -> "Optional Permissions"
                                else -> "Permissions Setup"
                            },
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Main permission card
                AnimatedVisibility(
                    visible = animateElements,
                    enter = slideInVertically(
                        initialOffsetY = { it/2 },
                        animationSpec = tween(1200, delayMillis = 400, easing = EaseOutCubic)
                    ) + fadeIn(tween(1200, delayMillis = 400))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(pulseScale),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.05f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (getIsGranted(permission, permissionStates)) Color(0xFF4CAF50).copy(alpha = 0.5f)
                            else permission.color.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Permission icon with animated background
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                if (getIsGranted(permission, permissionStates)) Color(0xFF4CAF50).copy(alpha = 0.2f)
                                                else permission.color.copy(alpha = 0.2f),
                                                Color.Transparent
                                            ),
                                            radius = 60f
                                        )
                                    )
                                    .border(
                                        2.dp,
                                        if (getIsGranted(permission, permissionStates)) Color(0xFF4CAF50).copy(alpha = 0.3f)
                                        else permission.color.copy(alpha = 0.3f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (getIsGranted(permission, permissionStates)) Icons.Default.CheckCircle else permission.icon,
                                    contentDescription = permission.title,
                                    modifier = Modifier.size(48.dp),
                                    tint = if (getIsGranted(permission, permissionStates)) Color(0xFF4CAF50) else permission.color
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = permission.title,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = permission.description,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 24.sp
                                )

                                Text(
                                    text = permission.detailedDescription,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                // Bottom action buttons
                AnimatedVisibility(
                    visible = animateElements,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(1000, delayMillis = 600, easing = EaseOutCubic)
                    ) + fadeIn(tween(1000, delayMillis = 600))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when {
                            // All permissions granted - ready to proceed
                            allPermissionsGranted -> {
                                Text(
                                    text = "Perfect! Ready to start tracking your fitness journey!",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF4CAF50),
                                    textAlign = TextAlign.Center
                                )
                            }
                            // Required permissions granted, showing optional
                            allRequiredGranted && !permission.isRequired -> {
                                // Permission action button for optional permission
                                Button(
                                    onClick = {
                                        android.util.Log.d("PermissionScreen", "Requesting optional permission: ${permission.title}")
                                        if (!getIsGranted(permission, permissionStates)) {
                                            handlePermissionRequest(
                                                permission = permission,
                                                permissionStates = permissionStates, // Pass permissionStates
                                                activity = activity,
                                                onOpenAppSettings = onOpenAppSettings,
                                                onOpenBatterySettings = onOpenBatterySettings
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (getIsGranted(permission, permissionStates)) Color(0xFF4CAF50) else permission.color,
                                        disabledContainerColor = permission.color.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    enabled = !getIsGranted(permission, permissionStates)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (getIsGranted(permission, permissionStates)) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Granted",
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Granted",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Security,
                                                contentDescription = "Grant Permission",
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Grant Permission",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                // Continue without this optional permission
                                TextButton(
                                    onClick = {
                                        android.util.Log.d("PermissionScreen", "Skipping optional permission, proceeding to home")
                                        // Go to home since required permissions are done
                                        onPermissionsGranted()
                                    }
                                ) {
                                    Text(
                                        text = "Continue without this",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            // Required permission not granted
                            else -> {
                                // Permission action button for required permission
                                Button(
                                    onClick = {
                                        android.util.Log.d("PermissionScreen", "Requesting required permission: ${permission.title}")
                                        if (!getIsGranted(permission, permissionStates)) {
                                            handlePermissionRequest(
                                                permission = permission,
                                                permissionStates = permissionStates, // Pass permissionStates
                                                activity = activity,
                                                onOpenAppSettings = onOpenAppSettings,
                                                onOpenBatterySettings = onOpenBatterySettings
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (getIsGranted(permission, permissionStates)) Color(0xFF4CAF50) else permission.color,
                                        disabledContainerColor = permission.color.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    enabled = !getIsGranted(permission, permissionStates)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (getIsGranted(permission, permissionStates)) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Granted",
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Granted",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Security,
                                                contentDescription = "Grant Permission",
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Grant Permission",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun handlePermissionRequest(
    permission: PermissionData,
    permissionStates: PermissionStates, // Add permissionStates parameter
    activity: ComponentActivity,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    when (permission.title) {
        "Activity Tracking" -> {
            // Always try to request the permission first
            permission.onGrant()
        }
        "Notifications" -> {
            // Always try to request the permission first
            permission.onGrant()
        }
        "Battery Optimization" -> {
            if (!getIsGranted(permission, permissionStates)) {
                // Request battery optimization exemption
                permission.onGrant()
            }
        }
        else -> {
            permission.onGrant()
        }
    }
}

private fun getIsGranted(permission: PermissionData, states: PermissionStates): Boolean {
    return when (permission.title) {
        "Activity Tracking" -> states.activityRecognitionGranted
        "Notifications" -> states.notificationGranted
        "Battery Optimization" -> states.batteryOptimizationDisabled
        else -> false // Fallback, though unlikely
    }
}

data class PermissionData(
    val title: String,
    val description: String,
    val detailedDescription: String,
    val icon: ImageVector,
    val color: Color,
    val onGrant: () -> Unit,
    val isRequired: Boolean,
    val needsAndroidVersion: Boolean
)