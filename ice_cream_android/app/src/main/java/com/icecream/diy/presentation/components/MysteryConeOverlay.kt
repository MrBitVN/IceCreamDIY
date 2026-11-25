package com.icecream.diy.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MysteryConeOverlay(
    level: Int,
    onNextClick: () -> Unit
) {
    // Cycle 25% -> 50% -> 75% -> 100% across mystery cone milestones
    val step = ((level / 2) - 1).mod(4) + 1
    val targetProgress = step / 4f
    val progressAnim = remember { Animatable(((step - 1).coerceAtLeast(0)) / 4f) }
    val cardScale = remember { Animatable(0.75f) }

    LaunchedEffect(level) {
        cardScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        progressAnim.animateTo(targetProgress, tween(durationMillis = 700, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .scale(cardScale.value)
                .shadow(24.dp, RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White)
                .border(4.dp, Color(0xFFFF94A8), RoundedCornerShape(32.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MYSTERY CONE",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF406E),
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Black Mystery Cone Silhouette with '?' Badge
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFFE4EC), Color(0xFFFFF5F8))
                            )
                        )
                        .border(3.dp, Color(0xFFFFB2C4), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(110.dp)) {
                        val w = size.width
                        val h = size.height

                        // Dark/Black Ice Cream Swirl Silhouette
                        drawCircle(
                            color = Color(0xFF1F222A),
                            radius = w * 0.24f,
                            center = Offset(w * 0.5f, h * 0.34f)
                        )
                        drawCircle(
                            color = Color(0xFF282C36),
                            radius = w * 0.19f,
                            center = Offset(w * 0.5f, h * 0.22f)
                        )

                        // Black Waffle Cone Silhouette
                        val conePath = Path().apply {
                            moveTo(w * 0.26f, h * 0.42f)
                            lineTo(w * 0.74f, h * 0.42f)
                            lineTo(w * 0.50f, h * 0.92f)
                            close()
                        }
                        drawPath(conePath, color = Color(0xFF171920))
                    }

                    Text(
                        text = "?",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFC107)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Progress Bar
                val pct = (progressAnim.value * 100).toInt()
                Text(
                    text = if (pct >= 100) "UNLOCKED! ($pct%)" else "UNLOCKING... $pct%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF555555)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEEEEEE))
                        .border(2.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressAnim.value.coerceIn(0.05f, 1f))
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF4CD964), Color(0xFF2ECC71))
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Glossy Green NEXT Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(8.dp, RoundedCornerShape(27.dp))
                        .clip(RoundedCornerShape(27.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF4CAF50), Color(0xFF388E3C))
                            )
                        )
                        .clickable { onNextClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NEXT",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }
    }
}
