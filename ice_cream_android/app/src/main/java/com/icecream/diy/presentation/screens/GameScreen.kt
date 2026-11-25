package com.icecream.diy.presentation.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icecream.diy.data.model.Flavor
import com.icecream.diy.data.model.GamePhase
import com.icecream.diy.presentation.components.*
import com.icecream.diy.presentation.theme.BgGradientBottom
import com.icecream.diy.presentation.theme.BgGradientTop
import com.icecream.diy.presentation.viewmodel.GameUiState
import com.icecream.diy.scene3d.IceCreamSceneView
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameScreen(
    uiState: GameUiState,
    onFlavorPressed: (Flavor) -> Unit,
    onFlavorReleased: (Flavor) -> Unit,
    onRestartClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCloseSettings: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onResetProgress: () -> Unit,
    onDecorateClick: () -> Unit = {},
    onDoubleReward: () -> Unit = {},
    onDismissMysteryCone: () -> Unit = {},
    onNextLevel: () -> Unit,
    onReplayLevel: () -> Unit,
    onMinigameComplete: () -> Unit,
    onBlenderComplete: () -> Unit = {},
    onMinigamePulse: () -> Unit = {},
    onBlenderTick: () -> Unit = {},
    onAwesomeFeedback: () -> Unit = {},
    onAddCoins: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState.phase) {
            GamePhase.PREPARE_MINIGAME -> {
                key(uiState.sessionNonce) {
                    MinigameBoardView(
                        level = uiState.level,
                        coins = uiState.coins,
                        onMinigameComplete = onMinigameComplete,
                        onAddCoins = onAddCoins,
                        onActionFeedback = onMinigamePulse,
                        onSettingsClick = onSettingsClick,
                        onRestartClick = onRestartClick
                    )
                }
            }
            GamePhase.BLENDER_MINIGAME -> {
                key(uiState.sessionNonce) {
                    BlenderMinigameView(
                        level = uiState.level,
                        coins = uiState.coins,
                        onBlenderComplete = onBlenderComplete,
                        onBlendTickFeedback = onBlenderTick,
                        onAwesomeFeedback = onAwesomeFeedback,
                        onSettingsClick = onSettingsClick,
                        onRestartClick = onRestartClick
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(BgGradientTop, BgGradientBottom)
                            )
                        )
                ) {
                    // White & Blue Swirling Background during DECORATING phase
                    if (uiState.phase == GamePhase.DECORATING) {
                        WhiteBlueSwirlBackground()
                    }

                    // 3D Scene Layer with Swirl Extrusion, Extra Stacked Cones, Sprinkles, Cherry, Shop & Customer
                    IceCreamSceneView(
                        activeFlavors = uiState.activeFlavors,
                        isRotating = uiState.isRotating,
                        pourProgress = uiState.pourProgress,
                        extraCones = uiState.targetConfig.extraCones,
                        sprinklesCount = uiState.targetConfig.sprinklesCount,
                        hasCherry = uiState.targetConfig.hasCherry,
                        isDecorated = uiState.isDecorated,
                        level = uiState.level,
                        gamePhase = uiState.phase,
                        customerVariant = uiState.customerVariant,
                        stars = uiState.stars
                    )

                    // 2D HUD UI Layer
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TopHudView(
                            level = uiState.level,
                            coins = uiState.coins,
                            stars = uiState.stars,
                            onSettingsClick = onSettingsClick,
                            onRestartClick = onRestartClick
                        )

                        Spacer(modifier = Modifier.weight(0.4f))

                        if (uiState.phase == GamePhase.POURING) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                MatchPreviewBox(
                                    targetLayers = uiState.targetConfig.targetLayers,
                                    hasCherry = uiState.targetConfig.hasCherry
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1.0f))

                        if (uiState.phase == GamePhase.POURING) {
                            FlavorButtonsBar(
                                flavors = uiState.targetConfig.availableFlavors,
                                activeFlavors = uiState.activeFlavors,
                                showTutorial = uiState.showTutorial,
                                onFlavorPressed = onFlavorPressed,
                                onFlavorReleased = onFlavorReleased
                            )
                        } else if (uiState.phase == GamePhase.DECORATING && !uiState.isDecorated) {
                            DecorateActionButton(
                                bonusCoins = uiState.decorationBonusCoins,
                                onClick = onDecorateClick
                            )
                            Spacer(modifier = Modifier.height(34.dp))
                        }
                    }

                    // Results Overlay Screen (Stars, 2 Cones VS, Count-up Coins, 2X, Next Level, Replay)
                    if (uiState.showResultsOverlay) {
                        ResultsOverlayView(
                            stars = uiState.stars,
                            matchScore = uiState.matchScore,
                            earnedCoins = uiState.earnedCoins,
                            targetLayers = uiState.targetConfig.targetLayers,
                            userSegments = uiState.userPouredSegments,
                            hasCherry = uiState.targetConfig.hasCherry,
                            isRewardDoubled = uiState.isRewardDoubled,
                            onDoubleReward = onDoubleReward,
                            onNextLevel = onNextLevel,
                            onReplayLevel = onReplayLevel
                        )
                    }

                    // Mystery Cone Popup
                    if (uiState.showMysteryCone) {
                        MysteryConeOverlay(
                            level = uiState.level,
                            onNextClick = onDismissMysteryCone
                        )
                    }
                }
            }
        }

        // Settings Modal Dialog accessible across all game phases (Slicing, Blender, Pouring)
        if (uiState.showSettingsDialog) {
            SettingsDialog(
                level = uiState.level,
                coins = uiState.coins,
                isSoundEnabled = uiState.isSoundEnabled,
                isHapticsEnabled = uiState.isHapticsEnabled,
                onToggleSound = onToggleSound,
                onToggleHaptics = onToggleHaptics,
                onResetProgress = onResetProgress,
                onDismiss = onCloseSettings
            )
        }
    }
}

@Composable
private fun WhiteBlueSwirlBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "decorateSwirl")
    val angleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "swirlAngle"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Color.White)
        val cx = size.width * 0.5f
        val cy = size.height * 0.48f
        val radius = maxOf(size.width, size.height) * 1.2f
        val rayCount = 14
        val stepDeg = 360f / rayCount

        for (i in 0 until rayCount step 2) {
            val a1 = Math.toRadians((i * stepDeg + angleOffset).toDouble())
            val a2 = Math.toRadians(((i + 1) * stepDeg + angleOffset).toDouble())
            val path = Path().apply {
                moveTo(cx, cy)
                lineTo(cx + (radius * cos(a1)).toFloat(), cy + (radius * sin(a1)).toFloat())
                lineTo(cx + (radius * cos(a2)).toFloat(), cy + (radius * sin(a2)).toFloat())
                close()
            }
            drawPath(path, color = Color(0xFFBCE6FF))
        }
    }
}

@Composable
private fun DecorateActionButton(
    bonusCoins: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.74f)
            .height(64.dp)
            .shadow(10.dp, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF42D674), Color(0xFF28A745))
                )
            )
            .border(3.dp, Color.White, RoundedCornerShape(32.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "DECORATE",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFC107))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9800)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "+$bonusCoins",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}
