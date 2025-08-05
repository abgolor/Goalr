package com.ajaytechsolutions.goalr.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajaytechsolutions.goalr.data.entities.DailyProgressEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

@Composable
fun WeeklyProgressView(
    weeklyProgress: List<DailyProgressEntity>,
    goal: Int,
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current

    // Generate last 7 days
    val weekDates = remember {
        (6 downTo 0).map {
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -it)
            }.timeInMillis
        }
    }

    var animateAppear by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500)
        animateAppear = true
    }

    val brandPrimary = Color(0xFF00B9BE)
    val brandSecondary = Color(0xFF009699)

    // Calculate summary stats
    val completedDays = weeklyProgress.count { it.steps >= goal }
    val totalSteps = weeklyProgress.sumOf { it.steps }
    val averageSteps = if (weeklyProgress.isNotEmpty()) totalSteps / weeklyProgress.size else 0

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Week Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { /* Handle previous week */ },
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Week",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }

            Text(
                text = "This Week",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = Color.White.copy(alpha = 0.8f)
            )

            IconButton(
                onClick = { /* Handle next week */ },
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Week",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        // Week Progress Circles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            weekDates.forEachIndexed { index, dateMillis ->
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = dateFormat.format(Date(dateMillis))
                val dayProgress = weeklyProgress.firstOrNull { it.date == formattedDate }
                val steps = dayProgress?.steps ?: 0
                val goalMet = steps >= goal
                val progressFraction = (steps.toFloat() / goal).coerceAtMost(1f)

                val todayCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val dayCalendar = Calendar.getInstance().apply {
                    timeInMillis = dateMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val isToday = todayCalendar.timeInMillis == dayCalendar.timeInMillis
                val isSelected = selectedDate == dateMillis

                WeeklyProgressDayView(
                    modifier = Modifier.weight(1f),
                    dateMillis = dateMillis,
                    steps = steps,
                    progressFraction = progressFraction,
                    goalMet = goalMet,
                    isToday = isToday,
                    isSelected = isSelected,
                    brandPrimary = brandPrimary,
                    animateAppear = animateAppear,
                    animationDelay = index * 100,
                    onTap = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDateSelected(dateMillis)
                    }
                )
            }
        }

        // Week Summary Stats
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.02f)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.White.copy(alpha = 0.02f),
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.05f),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WeekSummaryItem(
                        title = "Completed",
                        value = "$completedDays",
                        subtitle = "days",
                        color = brandPrimary
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )

                    WeekSummaryItem(
                        title = "Total Steps",
                        value = formatSteps(totalSteps),
                        subtitle = "this week",
                        color = Color(0xFFFF9500)
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )

                    WeekSummaryItem(
                        title = "Average",
                        value = formatSteps(averageSteps),
                        subtitle = "per day",
                        color = Color(0xFF9C27B0)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyProgressDayView(
    modifier: Modifier = Modifier,
    dateMillis: Long,
    steps: Int,
    progressFraction: Float,
    goalMet: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    brandPrimary: Color,
    animateAppear: Boolean,
    animationDelay: Int,
    onTap: () -> Unit
) {
    val dayFormatter = remember { SimpleDateFormat("E", Locale.getDefault()) }
    val dayNumberFormatter = remember { SimpleDateFormat("d", Locale.getDefault()) }
    val date = remember(dateMillis) { Date(dateMillis) }

    // Individual day animations
    val scaleAnimation by animateFloatAsState(
        targetValue = if (animateAppear) 1f else 0.3f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = animationDelay,
            easing = FastOutSlowInEasing
        ),
        label = "dayScale"
    )

    val alphaAnimation by animateFloatAsState(
        targetValue = if (animateAppear) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = animationDelay,
            easing = FastOutSlowInEasing
        ),
        label = "dayAlpha"
    )

    // Selection animation
    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "selection"
    )

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isToday && !isSelected) 1.3f else 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = modifier
            .scale(scaleAnimation * selectionScale)
            .alpha(alphaAnimation)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTap() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Day Label
        Text(
            text = dayFormatter.format(date),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = if (isToday) brandPrimary else Color.White.copy(alpha = 0.6f)
        )

        // Progress Circle
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            // Selection Ring
            if (isSelected) {
                Canvas(
                    modifier = Modifier
                        .size(52.dp)
                        .scale(pulseAnimation)
                        .alpha(if (pulseAnimation > 1.05f) 0.3f else 0.6f)
                ) {
                    drawCircle(
                        color = brandPrimary.copy(alpha = 0.3f),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // Today Pulse Ring
            if (isToday && !isSelected) {
                Canvas(
                    modifier = Modifier
                        .size(56.dp)
                        .scale(pulseAnimation)
                        .alpha(if (pulseAnimation > 1.15f) 0f else 0.4f)
                ) {
                    drawCircle(
                        color = brandPrimary.copy(alpha = 0.2f),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            Canvas(modifier = Modifier.size(44.dp)) {
                val strokeWidth = if (isSelected) 6.dp.toPx() else 4.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val center = this.center

                // Background Circle
                drawCircle(
                    color = Color.White.copy(alpha = if (isSelected) 0.15f else 0.08f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )

                // Progress Arc
                if (progressFraction > 0f) {
                    val progressColors = if (goalMet) {
                        listOf(brandPrimary, brandPrimary.copy(alpha = 0.8f))
                    } else {
                        listOf(brandPrimary.copy(alpha = 0.8f), brandPrimary.copy(alpha = 0.6f))
                    }

                    drawArc(
                        brush = Brush.linearGradient(
                            colors = progressColors,
                            start = Offset.Zero,
                            end = Offset(size.width, size.height)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * progressFraction,
                        useCenter = false,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }

            // Content inside the circle
            if (goalMet) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Goal Achieved",
                    modifier = Modifier
                        .size(12.dp)
                        .scale(if (isSelected) 1.1f else 1f),
                    tint = brandPrimary
                )
            } else if (steps > 0) {
                Text(
                    text = formatMiniSteps(steps),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp
                    ),
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.scale(if (isSelected) 1.1f else 1f)
                )
            }
        }

        // Day Number
        Text(
            text = dayNumberFormatter.format(date),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 10.sp
            ),
            color = if (isSelected) brandPrimary else Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun WeekSummaryItem(
    title: String,
    value: String,
    subtitle: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = color,
            textAlign = TextAlign.Center
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 9.sp
            ),
            color = Color.White.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}

// Helper functions
private fun formatSteps(steps: Int): String {
    return if (steps >= 1000) {
        String.format("%.1fk", steps / 1000.0)
    } else {
        steps.toString()
    }
}

private fun formatMiniSteps(steps: Int): String {
    return if (steps >= 1000) {
        "${steps / 1000}k"
    } else {
        steps.toString()
    }
}