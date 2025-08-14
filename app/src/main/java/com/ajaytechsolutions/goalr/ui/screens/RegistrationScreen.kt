package com.ajaytechsolutions.goalr.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ajaytechsolutions.goalr.data.entities.UserEntity
import com.ajaytechsolutions.goalr.viewmodel.GoalrViewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    viewModel: GoalrViewModel = hiltViewModel(),
    onRegistrationComplete: () -> Unit,
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var dailyGoal by remember { mutableIntStateOf(6000) }
    var showGoalPicker by remember { mutableStateOf(false) }
    var animateElements by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    // Add haptic feedback
    val hapticFeedback = LocalHapticFeedback.current

    // Add scroll state for better keyboard handling
    val scrollState = rememberScrollState()

    val totalSteps = 3
    val brandPrimary = Color(0xFF00B9BE)
    val brandSecondary = Color(0xFF00969B)

    // Background particles animation
    val particleOffsets = remember {
        List(8) {
            Animatable(
                initialValue = Offset(
                    Random.nextFloat() * 400f - 200f,
                    Random.nextFloat() * 800f - 400f
                ),
                typeConverter = Offset.VectorConverter
            )
        }
    }

    LaunchedEffect(Unit) {
        animateElements = true
        // Animate particles
        particleOffsets.forEachIndexed { index, particle ->
            launch {
                particle.animateTo(
                    targetValue = Offset(
                        Random.nextFloat() * 400f - 200f,
                        Random.nextFloat() * 800f - 400f
                    ),
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 4000 + index * 1000,
                            easing = EaseInOut
                        ),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF000000),
                        Color(0xFF0A0A0A),
                        Color(0xFF080808)
                    )
                )
            )
    ) {
        // Background particles
        particleOffsets.forEachIndexed { index, particle ->
            val offset by particle.asState()
            Box(
                modifier = Modifier
                    .offset(
                        x = with(LocalDensity.current) { offset.x.toDp() },
                        y = with(LocalDensity.current) { offset.y.toDp() }
                    )
                    .size((30 + index * 10).dp)
                    .clip(CircleShape)
                    .background(brandPrimary.copy(alpha = if (animateElements) 0.05f else 0.01f))
                    .blur(20.dp)
            )
        }

        // Make the entire content scrollable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Progress Indicator
            ProgressIndicator(
                currentStep = currentStep,
                totalSteps = totalSteps,
                brandPrimary = brandPrimary,
                animateElements = animateElements
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Welcome Header
            WelcomeHeader(
                animateElements = animateElements,
                brandPrimary = brandPrimary
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Form Steps
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "step_transition"
            ) { step ->
                when (step) {
                    0 -> FormStepView(
                        title = "What's your name?",
                        subtitle = "Let's personalize your experience",
                        isVisible = true
                    ) {
                        ModernTextField(
                            title = "Full Name",
                            value = name,
                            onValueChange = { name = it },
                            icon = Icons.Default.Person,
                            brandPrimary = brandPrimary
                        )
                    }
                    1 -> FormStepView(
                        title = "Choose a username",
                        subtitle = "This is how others will find you",
                        isVisible = true
                    ) {
                        ModernTextField(
                            title = "Username",
                            value = username,
                            onValueChange = { username = it },
                            icon = Icons.Default.AlternateEmail,
                            brandPrimary = brandPrimary
                        )
                    }
                    2 -> FormStepView(
                        title = "Set your daily goal",
                        subtitle = "Challenge yourself to grow every day",
                        isVisible = true
                    ) {
                        GoalSelectionCard(
                            dailyGoal = dailyGoal,
                            brandPrimary = brandPrimary,
                            onClick = {
                                showGoalPicker = true
                                // Add haptic feedback
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Action Buttons
            ActionButtons(
                currentStep = currentStep,
                totalSteps = totalSteps,
                canProceed = canProceed(currentStep, name, username),
                canComplete = canComplete(name, username),
                showSuccess = showSuccess,
                brandPrimary = brandPrimary,
                onNext = {
                    currentStep++
                    // Add haptic feedback for navigation
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                onBack = {
                    currentStep--
                    // Add haptic feedback for navigation
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                onComplete = {
                    showSuccess = true
                    // Success haptic feedback
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    val user = UserEntity(
                        name = name,
                        username = username,
                        dailyGoal = dailyGoal
                    )
                    viewModel.saveUser(user)
                    onRegistrationComplete()
                }
            )

            // Add extra space at bottom to ensure buttons are always visible
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Goal Picker Bottom Sheet
    if (showGoalPicker) {
        GoalPickerBottomSheet(
            dailyGoal = dailyGoal,
            onDismiss = { showGoalPicker = false },
            onGoalSelected = { newGoal ->
                dailyGoal = newGoal
                showGoalPicker = false
                // Success haptic feedback
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        )
    }
}

@Composable
private fun ProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    brandPrimary: Color,
    animateElements: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 20.dp)
    ) {
        repeat(totalSteps) { step ->
            val isCompleted = step <= currentStep
            val animatedWidth by animateDpAsState(
                targetValue = if (isCompleted) 40.dp else 20.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "progress_width"
            )

            Box(
                modifier = Modifier
                    .width(animatedWidth)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isCompleted) brandPrimary else Color.White.copy(alpha = 0.2f)
                    )
            )
        }
    }
}

@Composable
private fun WelcomeHeader(
    animateElements: Boolean,
    brandPrimary: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = animateElements,
            enter = fadeIn() + slideInVertically { -it / 2 }
        ) {
            Text(
                text = "Welcome to",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        AnimatedVisibility(
            visible = animateElements,
            enter = fadeIn() + scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy
                )
            )
        ) {
            Text(
                text = "Goalr",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                brandPrimary.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            radius = 200f
                        ),
                        shape = CircleShape
                    )
                    .padding(16.dp)
            )
        }

        AnimatedVisibility(
            visible = animateElements,
            enter = fadeIn() + slideInVertically { it / 2 }
        ) {
            Text(
                text = "Greatness is Earned",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Italic,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FormStepView(
    title: String,
    subtitle: String,
    isVisible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically { it / 2 }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = subtitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            content()
        }
    }
}

@Composable
private fun ModernTextField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    brandPrimary: Color
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.8f)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
            placeholder = {
                Text(
                    text = "Enter ${title.lowercase()}",
                    color = Color.White.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isFocused) brandPrimary else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = brandPrimary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                cursorColor = brandPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (title == "Username") KeyboardType.Email else KeyboardType.Text
            )
        )
    }
}

@Composable
private fun GoalSelectionCard(
    dailyGoal: Int,
    brandPrimary: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            brandPrimary.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TrackChanges,
                    contentDescription = null,
                    tint = brandPrimary,
                    modifier = Modifier.size(24.dp)
                )

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = dailyGoal.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "steps daily",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = getGoalLevelText(dailyGoal),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = getGoalLevelColor(dailyGoal)
                    )
                    Text(
                        text = "Tap to customize",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                Text(
                    text = "›",
                    fontSize = 20.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    currentStep: Int,
    totalSteps: Int,
    canProceed: Boolean,
    canComplete: Boolean,
    showSuccess: Boolean,
    brandPrimary: Color,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (currentStep < totalSteps - 1) {
            ActionButton(
                title = "Continue",
                isPrimary = true,
                isEnabled = canProceed,
                showSuccess = false,
                brandPrimary = brandPrimary,
                onClick = onNext
            )
        } else {
            ActionButton(
                title = "Enter Greatness",
                isPrimary = true,
                isEnabled = canComplete,
                showSuccess = showSuccess,
                brandPrimary = brandPrimary,
                onClick = onComplete
            )
        }

        if (currentStep > 0) {
            ActionButton(
                title = "Back",
                isPrimary = false,
                isEnabled = true,
                showSuccess = false,
                brandPrimary = brandPrimary,
                onClick = onBack
            )
        }
    }
}

@Composable
private fun ActionButton(
    title: String,
    isPrimary: Boolean,
    isEnabled: Boolean,
    showSuccess: Boolean,
    brandPrimary: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) brandPrimary else Color.Transparent,
            contentColor = if (isPrimary) Color.Black else brandPrimary,
            disabledContainerColor = if (isPrimary) brandPrimary.copy(alpha = 0.6f) else Color.Transparent,
            disabledContentColor = if (isPrimary) Color.Black.copy(alpha = 0.6f) else brandPrimary.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (!isPrimary) BorderStroke(2.dp, brandPrimary) else null,
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        AnimatedContent(
            targetState = showSuccess,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "button_content"
        ) { success ->
            if (success) {
                Text(
                    text = "✓",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun canProceed(currentStep: Int, name: String, username: String): Boolean {
    return when (currentStep) {
        0 -> name.isNotBlank()
        1 -> username.isNotBlank()
        else -> true
    }
}

private fun canComplete(name: String, username: String): Boolean {
    return name.isNotBlank() && username.isNotBlank()
}

private fun getGoalLevelText(goal: Int): String {
    return when (goal) {
        in 3000..4900 -> "Easier Goal"
        5000 -> "Recommended"
        in 5100..12000 -> "Challenging"
        in 12001..Int.MAX_VALUE -> "Elite Level"
        else -> "Custom Goal"
    }
}

private fun getGoalLevelColor(goal: Int): Color {
    return when (goal) {
        in 3000..4900 -> Color(0xFF4CAF50)
        5000 -> Color(0xFF00B9BE)
        in 5100..12000 -> Color(0xFFFF9800)
        in 12001..Int.MAX_VALUE -> Color(0xFFF44336)
        else -> Color.White
    }
}