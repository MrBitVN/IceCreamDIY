package com.icecream.diy.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ResultsOverlayView(
    stars: Int,
    matchScore: Int,
    earnedCoins: Int,
    onNextLevel: () -> Unit,
    onReplayLevel: () -> Unit
) {
    val starScale1 = remember { Animatable(0f) }
    val starScale2 = remember { Animatable(0f) }
    val starScale3 = remember { Animatable(0f) }

    LaunchedEffect(stars) {
        if (stars >= 1) {
            starScale1.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        if (stars >= 2) {
            delay(120)
            starScale2.animateTo(1.2f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        if (stars >= 3) {
            delay(120)
            starScale3.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
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
                // Title
                val titleText = when (stars) {
                    3 -> "PERFECT!"
                    2 -> "GREAT JOB!"
                    else -> "GOOD TRY!"
                }
                Text(
                    text = titleText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF406E)
                )

                Text(
                    text = "$matchScore% MATCH",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF888888)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3 Stars Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Star 1
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier
                            .size(52.dp)
                            .scale(if (stars >= 1) starScale1.value else 0.85f),
                        tint = if (stars >= 1) Color(0xFFFFC107) else Color(0xFFDDDDDD)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Star 2 (Center, bigger)
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier
                            .size(66.dp)
                            .offset(y = (-8).dp)
                            .scale(if (stars >= 2) starScale2.value else 0.85f),
                        tint = if (stars >= 2) Color(0xFFFFC107) else Color(0xFFDDDDDD)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Star 3
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier
                            .size(52.dp)
                            .scale(if (stars >= 3) starScale3.value else 0.85f),
                        tint = if (stars >= 3) Color(0xFFFFC107) else Color(0xFFDDDDDD)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Reward Coin Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFFF7E6))
                        .border(2.dp, Color(0xFFFFB300), RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFB300)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+$earnedCoins COINS",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF8F00)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Next Level Button
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
                        .clickable { onNextLevel() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NEXT LEVEL",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Replay Level Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFFEEEEEE))
                        .clickable { onReplayLevel() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Replay",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }
        }
    }
}
