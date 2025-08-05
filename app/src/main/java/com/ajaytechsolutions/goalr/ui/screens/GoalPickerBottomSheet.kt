package com.ajaytechsolutions.goalr.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.math.*

data class GoalPreset(
    val steps: Int,
    val title: String,
    val description: String,
    val color: Color,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalPickerBottomSheet(
    dailyGoal: Int,
    onDismiss: () -> Unit,
    onGoalSelected: (Int) -> Unit
) {
    var selectedGoal by remember { mutableIntStateOf(dailyGoal) }
    var animateAppear by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    // Add haptic feedback
    val hapticFeedback = LocalHapticFeedback.current

    // Add scroll state for content
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    val brandPrimary = Color(0xFF00B9BE)
    val brandSecondary = Color(0xFF00969B)

    val goalPresets = remember {
        listOf(
            GoalPreset(3000, "Getting Started", "Perfect for beginners", Color(0xFF4CAF50), Icons.Default.DirectionsWalk),
            GoalPreset(5000, "Recommended", "Health experts' choice", brandPrimary, Icons.Default.Favorite),
            GoalPreset(8000, "Active", "For fitness enthusiasts", Color(0xFFFF9800), Icons.Default.LocalFireDepartment),
            GoalPreset(10000, "Challenging", "Push your limits", Color(0xFFF44336), Icons.Default.Bolt),
            GoalPreset(12000, "Elite", "For champions", Color(0xFF9C27B0), Icons.Default.EmojiEvents)
        )
    }

    LaunchedEffect(Unit) {
        delay(100)
        animateAppear = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { onDismiss() }
        ) {
            // Modal Content - Improved sizing and layout
            val animatedOffset by animateDpAsState(
                targetValue = if (animateAppear) 0.dp else 400.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "modal_slide"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight() // Changed from fixed height
                    .heightIn(min = 600.dp, max = 800.dp) // Set min/max constraints
                    .align(Alignment.BottomCenter)
                    .offset(y = animatedOffset)
                    .offset(y = with(density) { dragOffset.toDp() })
                    .clickable(enabled = false) { } // Prevent clicks from passing through
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F0F0F),
                                Color(0xFF080808)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp) // Slightly smaller radius
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                // Close if dragged down more than 30% of the sheet height
                                if (dragOffset > 200) {
                                    onDismiss()
                                } else {
                                    dragOffset = 0f
                                }
                            }
                        ) { _, dragAmount ->
                            if (dragAmount.y > 0) {
                                dragOffset += dragAmount.y
                            }
                        }
                    }
            ) {
                // Header Section - Fixed at top
                HeaderSection(
                    animateAppear = animateAppear,
                    onDismiss = onDismiss
                )

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(bottom = 16.dp) // Add bottom padding for scroll
                ) {
                    // Current Goal Display
                    CurrentGoalDisplay(
                        selectedGoal = selectedGoal,
                        brandPrimary = brandPrimary,
                        brandSecondary = brandSecondary,
                        animateAppear = animateAppear
                    )

                    Spacer(modifier = Modifier.height(32.dp)) // Increased spacing

                    // Preset Goals Section
                    PresetGoalsSection(
                        goalPresets = goalPresets,
                        selectedGoal = selectedGoal,
                        animateAppear = animateAppear,
                        onPresetSelected = { newGoal ->
                            selectedGoal = newGoal
                            // Add haptic feedback
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp)) // Increased spacing

                    // Custom Goal Slider Section
                    CustomGoalSection(
                        selectedGoal = selectedGoal,
                        brandPrimary = brandPrimary,
                        animateAppear = animateAppear,
                        onGoalChanged = { newGoal ->
                            selectedGoal = newGoal
                            // Add haptic feedback for slider
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp)) // Add spacing before buttons
                }

                // Action Buttons - Fixed at bottom
                ActionButtonsSection(
                    showConfirmation = showConfirmation,
                    brandPrimary = brandPrimary,
                    animateAppear = animateAppear,
                    onConfirm = {
                        showConfirmation = true
                        // Success haptic feedback
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onGoalSelected(selectedGoal)
                    },
                    onCancel = onDismiss
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    animateAppear: Boolean,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp), // Balanced padding
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Drag Handle
        AnimatedVisibility(
            visible = animateAppear,
            enter = fadeIn()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            Color.White.copy(alpha = 0.3f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }


        // Title and Close Button
        AnimatedVisibility(
            visible = animateAppear,
            enter = fadeIn() + slideInVertically { -it }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Set Your Daily Goal",
                        fontSize = 22.sp, // Slightly smaller
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Choose a goal that challenges you",
                        fontSize = 14.sp, // Slightly smaller
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp) // Slightly larger
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp) // Slightly larger icon
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentGoalDisplay(
    selectedGoal: Int,
    brandPrimary: Color,
    brandSecondary: Color,
    animateAppear: Boolean
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (animateAppear) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "goal_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .padding(horizontal = 20.dp, vertical = 16.dp), // Better balanced padding
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp) // Increased spacing
    ) {
        // Goal Circle View
        GoalCircleView(
            selectedGoal = selectedGoal,
            brandPrimary = brandPrimary,
            brandSecondary = brandSecondary
        )

        // Goal Level Badge
        GoalLevelBadge(goal = selectedGoal)
    }
}

@Composable
private fun GoalCircleView(
    selectedGoal: Int,
    brandPrimary: Color,
    brandSecondary: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressForGoal(selectedGoal),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "progress_animation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(140.dp) // Increased size for better prominence
    ) {
        // Background Circle
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round) // Slightly thicker
            )
        }

        // Progress Ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sweepAngle = animatedProgress * 360f
            drawArc(
                brush = Brush.linearGradient(
                    colors = listOf(brandPrimary, brandSecondary)
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round) // Thicker for better visibility
            )
        }

        // Goal Number Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = selectedGoal,
                transitionSpec = {
                    slideInVertically { it } + fadeIn() togetherWith
                            slideOutVertically { -it } + fadeOut()
                },
                label = "goal_number"
            ) { goal ->
                Text(
                    text = goal.toString(),
                    fontSize = 32.sp, // Larger for better readability
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                text = "steps",
                fontSize = 16.sp, // Slightly larger
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun GoalLevelBadge(goal: Int) {
    val goalLevel = getGoalLevel(goal)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                goalLevel.color.copy(alpha = 0.15f),
                RoundedCornerShape(16.dp) // More rounded
            )
            .border(
                1.dp,
                goalLevel.color.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp) // More padding
    ) {
        Box(
            modifier = Modifier
                .size(10.dp) // Slightly larger dot
                .background(goalLevel.color, CircleShape)
        )
        Text(
            text = goalLevel.text,
            fontSize = 15.sp, // Slightly larger
            fontWeight = FontWeight.SemiBold,
            color = goalLevel.color
        )
    }
}

@Composable
private fun PresetGoalsSection(
    goalPresets: List<GoalPreset>,
    selectedGoal: Int,
    animateAppear: Boolean,
    onPresetSelected: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp) // Increased spacing
    ) {
        AnimatedVisibility(
            visible = animateAppear,
            enter = fadeIn() + slideInHorizontally { -it }
        ) {
            Text(
                text = "Quick Select",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        AnimatedVisibility(
            visible = animateAppear,
            enter = fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = 300))
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp), // Better spacing between cards
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                itemsIndexed(goalPresets) { index, preset ->
                    // Individual card animation
                    var cardVisible by remember { mutableStateOf(false) }

                    LaunchedEffect(animateAppear) {
                        if (animateAppear) {
                            delay(300L + index * 100L)
                            cardVisible = true
                        }
                    }

                    AnimatedVisibility(
                        visible = cardVisible,
                        enter = fadeIn(
                            animationSpec = tween(durationMillis = 300)
                        ) + slideInVertically(
                            animationSpec = tween(durationMillis = 300)
                        ) { it }
                    ) {
                        GoalPresetCard(
                            preset = preset,
                            isSelected = selectedGoal == preset.steps,
                            onTap = { onPresetSelected(preset.steps) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalPresetCard(
    preset: GoalPreset,
    isSelected: Boolean,
    onTap: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "card_scale"
    )

    Card(
        onClick = onTap,
        modifier = Modifier
            .width(140.dp) // Slightly wider
            .height(180.dp) // Slightly taller for better proportions
            .scale(animatedScale),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = if (isSelected) 0.08f else 0.03f)
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) preset.color.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp) // More rounded corners
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Increased padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon Section
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp) // Slightly larger
                    .background(
                        preset.color.copy(alpha = if (isSelected) 0.3f else 0.15f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = preset.icon,
                    contentDescription = null,
                    tint = preset.color,
                    modifier = Modifier.size(20.dp) // Slightly larger icon
                )
            }

            // Text Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp) // Better spacing
            ) {
                Text(
                    text = preset.steps.toString(),
                    fontSize = 18.sp, // Larger number
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = preset.title,
                    fontSize = 13.sp, // Slightly larger
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) preset.color else Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = preset.description,
                    fontSize = 11.sp, // Slightly larger
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 14.sp // Better line height
                )
            }
        }
    }
}

@Composable
private fun CustomGoalSection(
    selectedGoal: Int,
    brandPrimary: Color,
    animateAppear: Boolean,
    onGoalChanged: (Int) -> Unit
) {
    AnimatedVisibility(
        visible = animateAppear,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = 500
            )
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = 500
            )
        ) { it }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp) // Increased spacing
        ) {
            Text(
                text = "Or customize",
                fontSize = 18.sp, // Slightly larger
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f)
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { // Increased spacing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "3,000",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "15,000",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                CustomSlider(
                    value = selectedGoal.toFloat(),
                    onValueChange = { onGoalChanged(it.toInt()) },
                    valueRange = 3000f..15000f,
                    steps = 119, // (15000-3000)/100 - 1
                    brandPrimary = brandPrimary
                )
            }
        }
    }
}

@Composable
private fun CustomSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    brandPrimary: Color
) {
    var isDragging by remember { mutableStateOf(false) }

    Slider(
        value = value,
        onValueChange = {
            onValueChange((it / 100).roundToInt() * 100f) // Round to nearest 100
        },
        valueRange = valueRange,
        steps = steps,
        modifier = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = brandPrimary,
            activeTrackColor = brandPrimary,
            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
        )
    )
}

@Composable
private fun ActionButtonsSection(
    showConfirmation: Boolean,
    brandPrimary: Color,
    animateAppear: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AnimatedVisibility(
        visible = animateAppear,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = 600
            )
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = 600
            )
        ) { it }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp), // Better padding
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Confirm Button
            val confirmScale by animateFloatAsState(
                targetValue = if (showConfirmation) 0.95f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy
                ),
                label = "confirm_scale"
            )

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(confirmScale),
                colors = ButtonDefaults.buttonColors(
                    containerColor = brandPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 18.dp) // Slightly more padding
            ) {
                AnimatedContent(
                    targetState = showConfirmation,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "button_content"
                ) { success ->
                    if (success) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp) // Slightly larger
                        )
                    } else {
                        Text(
                            text = "Set Goal",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Cancel Button
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp) // Slightly more padding
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// Helper Functions
private fun progressForGoal(goal: Int): Float {
    return minOf(goal / 15000f, 1f)
}

private fun getGoalLevel(goal: Int): GoalLevel {
    return when (goal) {
        in 3000..4900 -> GoalLevel("Getting Started", Color(0xFF4CAF50))
        in 5000..7000 -> GoalLevel("Recommended", Color(0xFF00B9BE))
        in 7001..10000 -> GoalLevel("Active", Color(0xFFFF9800))
        in 10001..12000 -> GoalLevel("Challenging", Color(0xFFF44336))
        in 12001..Int.MAX_VALUE -> GoalLevel("Elite", Color(0xFF9C27B0))
        else -> GoalLevel("Custom", Color.White)
    }
}

private data class GoalLevel(val text: String, val color: Color)