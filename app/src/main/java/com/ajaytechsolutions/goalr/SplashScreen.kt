package com.ajaytechsolutions.goalr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000) // 2 seconds splash
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080808)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Goalr",
                fontSize = 40.sp,
                fontFamily = FontFamily(Font(R.font.montserrat_extrabold)),
                color = Color.White,
                modifier = Modifier.shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = Color(0xFF00B9BE).copy(alpha = 0.6f),
                    spotColor = Color(0xFF00B9BE).copy(alpha = 0.6f)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Greatness is Earned",
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily(Font(R.font.inter_regular)),
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
