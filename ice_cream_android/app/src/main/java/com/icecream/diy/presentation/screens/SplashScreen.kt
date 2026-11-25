package com.icecream.diy.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icecream.diy.presentation.theme.BgGradientBottom
import com.icecream.diy.presentation.theme.BgGradientTop
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val bounceScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        delay(2000) // ~2 seconds splash
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BgGradientTop, BgGradientBottom)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(bounceScale)
        ) {
            Text(
                text = "🍦",
                fontSize = 90.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ice Cream DIY",
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "3D SIMULATOR",
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
        }
    }
}
