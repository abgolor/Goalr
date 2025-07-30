package com.ajaytechsolutions.goalr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun HomeScreen(
    viewModel: GoalrViewModel = viewModel(),
    stepManager: StepCounterManager
) {
    val user by viewModel.user.collectAsState()
    val dailyProgress by viewModel.dailyProgress.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val stepsToday by stepManager.stepsToday.observeAsState(0)

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val stepsToShow = if (selectedDate == LocalDate.now()) stepsToday
                      else dailyProgress.find { it.date == selectedDate }?.steps ?: 0

    val dailyGoal = user?.dailyGoal ?: 4000

    Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .background(Color(0xFF080808))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp)
        ) {

            Text(
                text = if (selectedDate == LocalDate.now()) "Today's Progress"
                       else selectedDate.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(32.dp))

            ProgressCircleView(
                steps = stepsToShow,
                goal = dailyGoal,
                isToday = selectedDate == LocalDate.now(),
                date = selectedDate
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = "Streak",
                    tint = Color(0xFFFFA726), // flame orange
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Streak: $streak days",
                    fontSize = 18.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            WeeklyProgressView(
                progress = dailyProgress,
                goal = dailyGoal,
                selectedDate = selectedDate,
                onDateSelected = { date -> selectedDate = date }
            )
        }
    }

    // Update ViewModel whenever steps change
    LaunchedEffect(stepsToday) {
        if (selectedDate == LocalDate.now()) {
            viewModel.addSteps(stepsToday)
        }
    }

    // Start step tracking
    LaunchedEffect(Unit) {
        stepManager.startTracking()
    }
}
