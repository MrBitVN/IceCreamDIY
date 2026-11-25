package com.icecream.diy.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icecream.diy.presentation.theme.BgGradientBottom
import com.icecream.diy.presentation.theme.BgGradientTop
import com.icecream.diy.presentation.theme.ProgressGreen
import com.icecream.diy.presentation.theme.TextDark
import com.icecream.diy.scene3d.BlenderMinigameSceneView
import kotlinx.coroutines.delay

@Composable
fun BlenderMinigameView(
    level: Int,
    coins: Int,
    onBlenderComplete: () -> Unit,
    onBlendTickFeedback: () -> Unit = {},
    onAwesomeFeedback: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onRestartClick: () -> Unit
) {
    var targetFill by remember { mutableFloatStateOf(0f) }
    var showAwesome by remember { mutableStateOf(false) }

    val animatedFill by animateFloatAsState(
        targetValue = targetFill,
        animationSpec = tween(durationMillis = 1950, easing = FastOutSlowInEasing),
        label = "blender3dFill"
    )

    // Elastic spring bounce for AWESOME! banner
    val awesomeScale by animateFloatAsState(
        targetValue = if (showAwesome) 1.0f else 0.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "awesomeElasticPop"
    )

    LaunchedEffect(Unit) {
        delay(120)
        targetFill = 1.0f
        // Gentle rhythmic blending feedback while the 3 cups fill
        repeat(8) {
            onBlendTickFeedback()
            delay(230)
        }
        delay(160)
        showAwesome = true
        onAwesomeFeedback()
        delay(1300)
        onBlenderComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BgGradientTop, BgGradientBottom)
                )
            )
    ) {
        // 1. 3D Procedural Blender & 3 Translucent Cups (Yellow, Purple, Green)
        BlenderMinigameSceneView(fillProgress = animatedFill)

        // 2. 2D HUD & Bouncy AWESOME! Banner
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Left: Settings & Coin Pill
                Column(
                    modifier = Modifier.align(Alignment.TopStart),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(3.dp, CircleShape)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape)
                            .clickable { onSettingsClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.Gray,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    CoinPillView(coins = coins)
                }

                // Center: LEVEL N + Fill Progress Bar
                Column(
                    modifier = Modifier.align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(3.dp, RoundedCornerShape(50))
                            .background(if (showAwesome) Color(0xFF76D828) else Color.White, RoundedCornerShape(50))
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "LEVEL $level",
                            color = if (showAwesome) Color.White else TextDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(18.dp)
                            .shadow(2.dp, RoundedCornerShape(50))
                            .background(Color.White, RoundedCornerShape(50))
                            .padding(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedFill.coerceIn(0f, 1f))
                                .background(ProgressGreen, RoundedCornerShape(50))
                        )
                    }
                }

                // Right: Restart button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .align(Alignment.TopEnd)
                        .shadow(3.dp, CircleShape)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                        .clickable { onRestartClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart",
                        tint = Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // AWESOME! Bouncy Pop Banner
            if (showAwesome) {
                Box(
                    modifier = Modifier
                        .scale(awesomeScale)
                        .padding(bottom = 26.dp)
                        .shadow(14.dp, RoundedCornerShape(32.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFFB300), Color(0xFFFF5252))
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .border(3.5.dp, Color.White, RoundedCornerShape(32.dp))
                        .padding(horizontal = 38.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AWESOME!",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }

            // Labels under the 3 3D Cups (Yellow, Purple, Green)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CupColorPill("MANGO", Color(0xFFFFCA28))
                CupColorPill("BERRY", Color(0xFF9B51E0))
                CupColorPill("MINT", Color(0xFF38D368))
            }
        }
    }
}

@Composable
private fun CupColorPill(label: String, dotColor: Color) {
    Row(
        modifier = Modifier
            .shadow(3.dp, RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(dotColor, CircleShape)
        )
        Text(
            text = label,
            color = TextDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
    }
}
