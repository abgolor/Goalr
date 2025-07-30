package com.ajaytechsolutions.goalr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajaytechsolutions.goalr.model.DailyProgress
import java.time.LocalDate


@Composable
fun WeeklyProgressView(
    progress: List<DailyProgress>,
    goal: Int,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val weekDates = (0..6).map { today.minusDays(it.toLong()) }.reversed()

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        weekDates.forEach { day ->
            val dayProgress = progress.find { it.date == day }
            val steps = dayProgress?.steps ?: 0
            val goalMet = dayProgress?.goalMet ?: false
            val fraction = (steps.toFloat() / goal).coerceAtMost(1f)

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                onDateSelected(day)
            }) {
                Text(
                    text = day.dayOfWeek.name.take(3),
                    fontSize = 12.sp,
                    color = if (day == selectedDate) Color.Cyan else Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(40.dp)) {
                        drawCircle(Color.White.copy(alpha = 0.1f), style = Stroke(width = 6f))
                        drawArc(
                            brush = Brush.linearGradient(
                                listOf(Color(0f, 185f/255f, 190f/255f), Color.Cyan)
                            ),
                            startAngle = -90f,
                            sweepAngle = 360 * fraction,
                            useCenter = false,
                            style = Stroke(width = 6f, cap = StrokeCap.Round)
                        )
                    }
                    if (goalMet) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

