package com.example.steptracker

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

@Composable
fun ProgressCircleView(
    steps: Int,
    goal: Int,
    date: Date,
    isSelectedToday: Boolean,
    animatePulse: Boolean,
    modifier: Modifier = Modifier
) {
    // Brand colors
    val brandPrimary = Color(0, 185, 190)
    val brandSecondary = Color(0, 150, 155)

    // Animation states
    var showStepCount by remember { mutableStateOf(false) }
    var showBurst by remember { mutableStateOf(false) }

    // Progress calculations
    val progressFraction = remember(steps, goal) {
        minOf(steps.toDouble() / goal.toDouble(), 1.0)
    }
    val remainingSteps = maxOf(goal - steps, 0)

    // Animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.toFloat(),
        animationSpec = tween(800, easing = EaseInOut),
        label = "progress"
    )

    // Pulse animation
    val pulseScale by animateFloatAsState(
        targetValue = if (animatePulse) 1.02f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val pulseOpacity by animateFloatAsState(
        targetValue = if (animatePulse) 0.8f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseOpacity"
    )

    // Step count animation
    val stepCountScale by animateFloatAsState(
        targetValue = if (showStepCount) 1f else 0.8f,
        animationSpec = tween(800, easing = EaseInOut),
        label = "stepScale"
    )

    val stepCountAlpha by animateFloatAsState(
        targetValue = if (showStepCount) 1f else 0f,
        animationSpec = tween(800, easing = EaseInOut),
        label = "stepAlpha"
    )

    // Goal achievement effect
    LaunchedEffect(steps, goal) {
        if (steps >= goal && !showBurst) {
            showBurst = true
            delay(2500)
            showBurst = false
        }
    }

    // Show step count animation on appear
    LaunchedEffect(Unit) {
        delay(300)
        showStepCount = true
    }

    Box(
        modifier = modifier.size(320.dp),
        contentAlignment = Alignment.Center
    ) {
        // Canvas for circles
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(pulseScale)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val outerRadius = 140.dp.toPx()
            val innerRadius = 120.dp.toPx()
            val strokeWidth = 28.dp.toPx()

            // Outer glow ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        brandPrimary.copy(alpha = 0.1f * pulseOpacity),
                        brandPrimary.copy(alpha = 0.05f * pulseOpacity)
                    ),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Background circle
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.04f),
                        Color.White.copy(alpha = 0.08f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                radius = innerRadius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress circle with gradient
            if (animatedProgress > 0) {
                drawProgressArc(
                    center = center,
                    radius = innerRadius,
                    strokeWidth = strokeWidth,
                    progress = animatedProgress,
                    brandPrimary = brandPrimary,
                    brandSecondary = brandSecondary,
                    animatePulse = animatePulse
                )
            }
        }

        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(stepCountScale)
                .graphicsLayer { alpha = stepCountAlpha }
        ) {
            // Date text
            Text(
                text = if (isSelectedToday) "Today" else formatDate(date),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Steps count with proper formatting and dynamic sizing
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val (formattedSteps, fontSize) = when {
                    steps >= 1000000 -> Pair(formatLargeNumbers(steps), 36.sp) // For millions
                    steps >= 100000 -> Pair(formatLargeNumbers(steps), 40.sp)  // For hundred thousands
                    steps >= 10000 -> Pair(formatLargeNumbers(steps), 44.sp)   // For ten thousands
                    else -> Pair(formatLargeNumbers(steps), 48.sp)             // Normal size
                }

                Text(
                    text = formattedSteps,
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Text(
                    text = "steps",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Goal status
            if (isSelectedToday) {
                if (steps >= goal) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Goal Achieved",
                            tint = brandPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Goal Achieved!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = brandPrimary
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${formatLargeNumbers(remainingSteps)} to go",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Goal: ${formatNumber(goal)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (steps >= goal) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                        contentDescription = if (steps >= goal) "Goal Achieved" else "Goal Missed",
                        tint = if (steps >= goal) brandPrimary else Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (steps >= goal) "Goal Achieved" else "Goal Missed",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (steps >= goal) brandPrimary else Color.Red.copy(alpha = 0.7f)
                    )
                }
            }

            // Progress percentage
            if (progressFraction > 0 && progressFraction < 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(progressFraction * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }

        // Particle burst effect
        if (showBurst) {
            ParticleBurstView(
                color = brandPrimary,
                modifier = Modifier.size(320.dp)
            )
        }
    }
}

fun DrawScope.drawProgressArc(
    center: Offset,
    radius: Float,
    strokeWidth: Float,
    progress: Float,
    brandPrimary: Color,
    brandSecondary: Color,
    animatePulse: Boolean
) {
    val sweepAngle = 360f * progress

    // Create gradient brush
    val gradientBrush = Brush.sweepGradient(
        colors = listOf(
            brandPrimary,
            brandSecondary,
            brandPrimary.copy(alpha = 0.8f),
            brandPrimary
        ),
        center = center
    )

    // Draw shadow effect first
    if (progress > 0) {
        val shadowRadius = if (animatePulse) 20.dp.toPx() else 8.dp.toPx()
        drawArc(
            color = brandPrimary.copy(alpha = 0.4f * progress),
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(
                center.x - radius - 2,
                center.y - radius - 2
            ),
            size = androidx.compose.ui.geometry.Size((radius + 2) * 2, (radius + 2) * 2),
            style = Stroke(
                width = strokeWidth + 4,
                cap = StrokeCap.Round
            )
        )
    }

    // Draw progress arc
    drawArc(
        brush = gradientBrush,
        startAngle = -90f,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(
            center.x - radius,
            center.y - radius
        ),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round
        )
    )
}

@Composable
fun ParticleBurstView(
    color: Color,
    modifier: Modifier = Modifier
) {
    var particles by remember { mutableStateOf(List(20) { createParticle() }) }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 2500) {
            delay(16) // ~60fps
            particles = particles.map { particle ->
                particle.copy(
                    x = particle.x + particle.velocityX,
                    y = particle.y + particle.velocityY,
                    alpha = particle.alpha * 0.98f,
                    size = particle.size * 0.99f
                )
            }
        }
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        particles.forEach { particle ->
            drawCircle(
                color = color.copy(alpha = particle.alpha),
                radius = particle.size,
                center = center + Offset(particle.x, particle.y)
            )
        }
    }
}

data class Particle(
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val alpha: Float,
    val size: Float
)

fun createParticle(): Particle {
    val angle = Math.random() * 2 * PI
    val speed = Math.random() * 5 + 2
    return Particle(
        x = 0f,
        y = 0f,
        velocityX = (cos(angle) * speed).toFloat(),
        velocityY = (sin(angle) * speed).toFloat(),
        alpha = 1f,
        size = (Math.random() * 4 + 2).toFloat()
    )
}

fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
    return formatter.format(date)
}

fun formatNumber(number: Int): String {
    return if (number >= 1000) {
        String.format("%.1fk", number / 1000.0)
    } else {
        number.toString()
    }
}

// Format large numbers with commas for step count display
fun formatLargeNumbers(number: Int): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return formatter.format(number)
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun ProgressCircleViewPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProgressCircleView(
            steps = 3200,
            goal = 4000,
            date = Date(),
            isSelectedToday = true,
            animatePulse = true
        )

        ProgressCircleView(
            steps = 12500,
            goal = 10000,
            date = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }.time,
            isSelectedToday = false,
            animatePulse = true
        )

        ProgressCircleView(
            steps = 1250000,
            goal = 1000000,
            date = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -2)
            }.time,
            isSelectedToday = false,
            animatePulse = true
        )
    }
}
