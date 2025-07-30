package com.ajaytechsolutions.goalr

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ProgressCircleView(
    steps: Int,
    goal: Int,
    isToday: Boolean,
    date: LocalDate,
    modifier: Modifier = Modifier
) {
    val progress = (steps.toFloat() / goal).coerceAtMost(1f)
    val showConfetti = remember { mutableStateOf(false) }

    LaunchedEffect(steps) {
        if (steps >= goal && !showConfetti.value) {
            showConfetti.value = true
            kotlinx.coroutines.delay(2000)
            showConfetti.value = false
        }
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(240.dp)) {
        // Background Circle
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.08f), style = Stroke(width = 24f))
            drawArc(
                brush = Brush.linearGradient(
                    listOf(Color(0f, 185f/255f, 190f/255f), Color.Cyan)
                ),
                startAngle = -90f,
                sweepAngle = 360 * progress,
                useCenter = false,
                style = Stroke(width = 24f, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isToday) "Steps Today" else date.toString(),
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = steps.toString(),
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (isToday) {
                if (steps >= goal) {
                    Text("Goal Achieved ✅", fontSize = 16.sp, color = Color(0f, 185f/255f, 190f/255f))
                } else {
                    Text("Goal: $goal", fontSize = 16.sp, color = Color.White.copy(alpha = 0.5f))
                }
            }
        }

        if (showConfetti.value) {
            ConfettiBurst()
        }
    }
}

@Composable
fun ConfettiBurst() {
    val particles = (0..20).map { Pair(Random.nextFloat(), Random.nextFloat()) }
    val transition = rememberInfiniteTransition()

    val alphaAnim by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { (dx, dy) ->
            val angle = Random.nextDouble(0.0, 2 * Math.PI)
            val radius = size.minDimension / 2
            val x = (size.width / 2) + (radius * dx * cos(angle)).toFloat()
            val y = (size.height / 2) + (radius * dy * sin(angle)).toFloat()

            drawCircle(
                color = Color(0f, 185f/255f, 190f/255f).copy(alpha = alphaAnim),
                radius = 6f,
                center = Offset(x, y)
            )
        }
    }
}
