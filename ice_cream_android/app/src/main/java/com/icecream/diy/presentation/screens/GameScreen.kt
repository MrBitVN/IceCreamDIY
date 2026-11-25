package com.icecream.diy.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.icecream.diy.data.model.Flavor
import com.icecream.diy.data.model.GamePhase
import com.icecream.diy.presentation.components.*
import com.icecream.diy.presentation.theme.BgGradientBottom
import com.icecream.diy.presentation.theme.BgGradientTop
import com.icecream.diy.presentation.viewmodel.GameUiState
import com.icecream.diy.scene3d.IceCreamSceneView

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
                    // 3D Scene Layer with Swirl Extrusion, Extra Stacked Cones, Sprinkles, Cherry, Shop & Customer
                    IceCreamSceneView(
                        activeFlavors = uiState.activeFlavors,
                        isRotating = uiState.isRotating,
                        pourProgress = uiState.pourProgress,
                        extraCones = uiState.targetConfig.extraCones,
                        sprinklesCount = uiState.targetConfig.sprinklesCount,
                        hasCherry = uiState.targetConfig.hasCherry,
                        level = uiState.level,
                        gamePhase = uiState.phase,
                        customerVariant = uiState.customerVariant,
                        stars = uiState.stars
                    )

                    // 2D HUD UI Layer
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TopHudView(
                            level = uiState.level,
                            coins = uiState.coins,
                            stars = uiState.stars,
                            onSettingsClick = onSettingsClick,
                            onRestartClick = onRestartClick
                        )

                        Spacer(modifier = Modifier.weight(0.4f))

                        if (uiState.phase != GamePhase.CUSTOMER_REACTION) {
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

                        if (uiState.phase != GamePhase.CUSTOMER_REACTION) {
                            FlavorButtonsBar(
                                flavors = uiState.targetConfig.availableFlavors,
                                activeFlavors = uiState.activeFlavors,
                                showTutorial = uiState.showTutorial,
                                onFlavorPressed = onFlavorPressed,
                                onFlavorReleased = onFlavorReleased
                            )
                        }
                    }

                    // Results Overlay Screen (Stars, +Coins, Next Level, Replay)
                    if (uiState.showResultsOverlay) {
                        ResultsOverlayView(
                            stars = uiState.stars,
                            matchScore = uiState.matchScore,
                            earnedCoins = uiState.earnedCoins,
                            onNextLevel = onNextLevel,
                            onReplayLevel = onReplayLevel
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
