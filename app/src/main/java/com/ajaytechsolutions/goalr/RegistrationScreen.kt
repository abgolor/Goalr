package com.ajaytechsolutions.goalr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajaytechsolutions.goalr.model.User

@Composable
fun RegistrationScreen(
    viewModel: GoalrViewModel,
    onRegister: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var dailyGoal by remember { mutableStateOf(6000) }
    var showGoalPicker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080808))
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                "Welcome to",
                fontSize = 20.sp,
                color = Color.White.copy(alpha = 0.7f),
                fontFamily = FontFamily(Font(R.font.inter_medium))
            )

            Text(
                "Goalr",
                fontSize = 40.sp,
                fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                color = Color.White,
                modifier = Modifier.shadow(12.dp) // Simple shadow
            )

            Text(
                "Greatness is Earned",
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                color = Color.White.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(32.dp))

            // Name Field
            CustomTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Name"
            )

            Spacer(Modifier.height(16.dp))

            // Username Field
            CustomTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = "Username"
            )

            Spacer(Modifier.height(16.dp))

            // Daily Goal Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clickable { showGoalPicker = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, Color(0xFF00B9BE).copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Daily Goal", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp)
                    Text(
                        "$dailyGoal steps",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontFamily = FontFamily(Font(R.font.montserrat_bold))
                    )
                    Text(
                        goalLevelText(dailyGoal),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Submit Button
            Button(
                onClick = {
                    viewModel.saveUser(User(name, username, dailyGoal))
                    onRegister()
                },
                enabled = name.isNotEmpty() && username.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B9BE))
            ) {
                Text(
                    "Enter Greatness",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(R.font.montserrat_bold))
                )
            }
        }

        // Goal Picker Bottom Sheet
        if (showGoalPicker) {
            GoalPickerSheet(
                currentGoal = dailyGoal,
                onGoalSelected = {
                    dailyGoal = it
                    showGoalPicker = false
                },
                onDismiss = { showGoalPicker = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.3f)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color(0xFF00B9BE),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

fun goalLevelText(goal: Int): String = when (goal) {
    in 3000..4999 -> "Easier Goal"
    in 5000..7999 -> "Recommended Goal"
    in 8000..12000 -> "Challenging Goal"
    else -> "Elite Goal"
}

// Simple placeholder for GoalPickerSheet
@Composable
fun GoalPickerSheet(
    currentGoal: Int,
    onGoalSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Select Goal", fontSize = 20.sp)
                Spacer(Modifier.height(16.dp))
                listOf(3000, 5000, 8000, 12000).forEach { goal ->
                    Text(
                        "$goal steps",
                        modifier = Modifier
                            .clickable { onGoalSelected(goal) }
                            .padding(8.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onDismiss) { Text("Cancel") }
            }
        }
    }
}
