package com.ajaytechsolutions.goalr.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ajaytechsolutions.goalr.viewmodel.GoalrViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

@Composable
fun SplashScreen(onFinish: (Boolean) -> Unit) {
    val viewModel: GoalrViewModel = hiltViewModel()
    val user by viewModel.user.collectAsState()

    var isActive by remember { mutableStateOf(false) }
    var logoScale by remember { mutableFloatStateOf(0.5f) }
    var logoRotation by remember { mutableFloatStateOf(0f) }
    var opacity by remember { mutableFloatStateOf(0f) }
    var pulse by remember { mutableStateOf(false) }
    var showParticles by remember { mutableStateOf(false) }
    var backgroundOffset by remember { mutableFloatStateOf(0f) }

    val brandPrimary = Color(0xFF00B9BE)
    val brandSecondary = Color(0xFF00969B)

    // Animated values
    val animatedLogoScale by animateFloatAsState(
        targetValue = logoScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    val animatedOpacity by animateFloatAsState(
        targetValue = opacity,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "opacity"
    )

    val animatedBackgroundOffset by animateFloatAsState(
        targetValue = backgroundOffset,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "background_offset"
    )

    val infiniteRotation by rememberInfiniteTransition(label = "rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "infinite_rotation"
    )

    // Floating orbs animation - Fixed type inference
    val orbOffsets = remember {
        List(5) { index ->
            Animatable(
                initialValue = Offset(
                    cos(index * 2.0).toFloat() * 150f,
                    sin(index * 1.5).toFloat() * 200f
                ),
                typeConverter = Offset.VectorConverter
            )
        }
    }

    // Particle animation
    val particleStates = remember {
        List(20) {
            ParticleState(
                offset = Offset(
                    Random.nextFloat() * 400f - 200f,
                    Random.nextFloat() * 800f - 400f
                ),
                scale = Random.nextFloat() * 0.5f + 0.5f,
                opacity = Random.nextFloat() * 0.6f + 0.4f
            )
        }
    }

    LaunchedEffect(user) {
        startAnimations(
            onLogoScale = { logoScale = it },
            onOpacity = { opacity = it },
            onPulse = { pulse = it },
            onBackgroundOffset = { backgroundOffset = it },
            onShowParticles = { showParticles = it },
            onActive = { isActive = it }
        )

        // Animate floating orbs - Fixed type specification
        orbOffsets.forEachIndexed { index, orb ->
            launch {
                orb.animateTo(
                    targetValue = Offset(
                        cos(index * 2.0 + 1.0).toFloat() * 150f,
                        sin(index * 1.5 + 1.0).toFloat() * 200f
                    ),
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 3000 + index * 1000,
                            easing = EaseInOut
                        ),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
        }

        delay(3500)
        onFinish(user != null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF000000),
                        Color(0xFF0A0A0A),
                        Color(0xFF080808),
                        Color(0xFF000000)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
    ) {
        // Animated Background Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = animatedBackgroundOffset.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            brandPrimary.copy(alpha = 0.02f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Floating Orbs - Fixed asState() call
        orbOffsets.forEachIndexed { index, orb ->
            val offset by orb.asState()
            val size = (60 + index * 40).dp
            val orbPulse by animateFloatAsState(
                targetValue = if (pulse) 0.3f else 0.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 3000 + index * 1000,
                        easing = EaseInOut
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "orb_pulse_$index"
            )

            Box(
                modifier = Modifier
                    .size(size)
                    .offset(
                        x = offset.x.dp,
                        y = offset.y.dp
                    )
                    .blur(40.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                brandPrimary.copy(alpha = orbPulse),
                                brandSecondary.copy(alpha = orbPulse * 0.5f),
                                Color.Transparent
                            ),
                            radius = size.value * 2
                        ),
                        CircleShape
                    )
            )
        }

        // Main Content
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Logo Section
            LogoSection(
                logoScale = animatedLogoScale,
                opacity = animatedOpacity,
                rotation = infiniteRotation,
                pulse = pulse,
                brandPrimary = brandPrimary,
                brandSecondary = brandSecondary
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Brand Text
            BrandTextSection(
                logoScale = animatedLogoScale,
                opacity = animatedOpacity,
                brandPrimary = brandPrimary
            )

            Spacer(modifier = Modifier.weight(1f))

            // Loading Indicator
            LoadingIndicator(
                opacity = animatedOpacity,
                pulse = pulse,
                brandPrimary = brandPrimary
            )

            Spacer(modifier = Modifier.height(60.dp))
        }

        // Particle Effects
        if (showParticles) {
            ParticleEffects(
                particles = particleStates,
                showParticles = showParticles,
                brandPrimary = brandPrimary
            )
        }
    }
}

@Composable
private fun LogoSection(
    logoScale: Float,
    opacity: Float,
    rotation: Float,
    pulse: Boolean,
    brandPrimary: Color,
    brandSecondary: Color
) {
    val pulseScale by animateFloatAsState(
        targetValue = if (pulse) 1.2f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val pulseOpacity by animateFloatAsState(
        targetValue = if (pulse) 0.3f else 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_opacity"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .scale(logoScale)
            .graphicsLayer { alpha = opacity }
    ) {
        // Outer Glow Ring
        Canvas(
            modifier = Modifier
                .size(180.dp)
                .scale(pulseScale)
                .graphicsLayer { alpha = pulseOpacity }
        ) {
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        brandPrimary.copy(alpha = 0.3f),
                        brandSecondary.copy(alpha = 0.1f)
                    )
                ),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Inner Rotating Ring
        Canvas(
            modifier = Modifier
                .size(120.dp)
                .rotate(rotation)
        ) {
            val strokeWidth = 3.dp.toPx()

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        brandPrimary,
                        brandSecondary,
                        brandPrimary.copy(alpha = 0.3f),
                        brandPrimary
                    )
                ),
                startAngle = 0f,
                sweepAngle = 288f, // 0.8 * 360
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }

        // App Icon/Logo
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            brandPrimary.copy(alpha = 0.8f),
                            brandPrimary.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
                .shadow(
                    elevation = 15.dp,
                    shape = CircleShape,
                    ambientColor = brandPrimary.copy(alpha = 0.8f),
                    spotColor = brandPrimary.copy(alpha = 0.8f)
                )
        ) {
            Text(
                text = "G",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BrandTextSection(
    logoScale: Float,
    opacity: Float,
    brandPrimary: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Goalr",
            fontSize = 56.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .scale(logoScale * 0.9f)
                .graphicsLayer { alpha = opacity }
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    ambientColor = brandPrimary.copy(alpha = 0.6f),
                    spotColor = brandPrimary.copy(alpha = 0.6f)
                )
        )

        Text(
            text = "Greatness is Earned",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            fontStyle = FontStyle.Italic,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .scale(logoScale * 0.8f)
                .graphicsLayer { alpha = opacity * 0.8f }
        )
    }
}

@Composable
private fun LoadingIndicator(
    opacity: Float,
    pulse: Boolean,
    brandPrimary: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Animated Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { index ->
                val dotScale by animateFloatAsState(
                    targetValue = if (pulse) 1.2f else 0.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 800,
                            delayMillis = index * 200,
                            easing = EaseInOut
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot_scale_$index"
                )

                val dotOpacity by animateFloatAsState(
                    targetValue = if (pulse) 1.0f else 0.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 800,
                            delayMillis = index * 200,
                            easing = EaseInOut
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot_opacity_$index"
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(dotScale)
                        .background(
                            brandPrimary.copy(alpha = dotOpacity),
                            CircleShape
                        )
                )
            }
        }

        Text(
            text = "Preparing your journey...",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.graphicsLayer { alpha = opacity * 0.7f }
        )
    }
}

@Composable
private fun ParticleEffects(
    particles: List<ParticleState>,
    showParticles: Boolean,
    brandPrimary: Color
) {
    particles.forEachIndexed { index, particle ->
        val animatedOpacity by animateFloatAsState(
            targetValue = if (showParticles) particle.opacity else 0f,
            animationSpec = tween(
                durationMillis = Random.nextInt(1000, 3000),
                delayMillis = index * 100,
                easing = EaseOut
            ),
            label = "particle_opacity_$index"
        )

        val animatedScale by animateFloatAsState(
            targetValue = if (showParticles) particle.scale else 0f,
            animationSpec = tween(
                durationMillis = Random.nextInt(1000, 3000),
                delayMillis = index * 100,
                easing = EaseOut
            ),
            label = "particle_scale_$index"
        )

        Box(
            modifier = Modifier
                .size(3.dp)
                .offset(
                    x = particle.offset.x.dp,
                    y = particle.offset.y.dp
                )
                .scale(animatedScale)
                .background(
                    brandPrimary.copy(alpha = animatedOpacity),
                    CircleShape
                )
        )
    }
}

private suspend fun startAnimations(
    onLogoScale: (Float) -> Unit,
    onOpacity: (Float) -> Unit,
    onPulse: (Boolean) -> Unit,
    onBackgroundOffset: (Float) -> Unit,
    onShowParticles: (Boolean) -> Unit,
    onActive: (Boolean) -> Unit
) {
    // Start background animation
    onBackgroundOffset(20f)

    // Start pulse
    onPulse(true)

    // Logo entrance animation with delay
    delay(300)
    onLogoScale(1.0f)
    onOpacity(1.0f)

    // Show particles
    delay(1200)
    onShowParticles(true)

    // Mark as active
    delay(2000)
    onActive(true)
}

private data class ParticleState(
    val offset: Offset,
    val scale: Float,
    val opacity: Float
)