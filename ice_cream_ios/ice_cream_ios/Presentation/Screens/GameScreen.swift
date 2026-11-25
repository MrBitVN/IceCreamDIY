import SwiftUI

struct GameScreen: View {
    @ObservedObject var viewModel: GameViewModel

    var body: some View {
        Group {
            if viewModel.uiState.phase == .prepareMinigame {
                MinigameBoardView(
                    level: viewModel.uiState.level,
                    coins: viewModel.uiState.coins,
                    onMinigameComplete: {
                        viewModel.completeMinigame()
                    },
                    onAddCoins: { amount in
                        viewModel.addCoins(amount)
                    },
                    onSettingsTapped: {
                        viewModel.openSettings()
                    },
                    onRestartTapped: {
                        viewModel.restartLevel()
                    }
                )
            } else if viewModel.uiState.phase == .blenderMinigame {
                BlenderMinigameView(
                    level: viewModel.uiState.level,
                    coins: viewModel.uiState.coins,
                    onBlenderComplete: {
                        viewModel.completeBlender()
                    },
                    onSettingsTapped: {
                        viewModel.openSettings()
                    },
                    onRestartTapped: {
                        viewModel.restartLevel()
                    }
                )
            } else {
                ZStack {
                    // Gradient Background matching image 1 & 2
                    LinearGradient(
                        colors: [GameColors.bgGradientTop, GameColors.bgGradientBottom],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .ignoresSafeArea()

                    // 3D Scene View with Swirl Extrusion, Stacked Cones, Sprinkles, Cherry, Shop & Customer
                    IceCreamSceneView(
                        activeFlavors: viewModel.uiState.activeFlavors,
                        isRotating: viewModel.uiState.isRotating,
                        pourProgress: viewModel.uiState.pourProgress,
                        extraCones: viewModel.uiState.targetConfig.extraCones,
                        sprinklesCount: viewModel.uiState.targetConfig.sprinklesCount,
                        hasCherry: viewModel.uiState.targetConfig.hasCherry,
                        level: viewModel.uiState.level,
                        gamePhase: viewModel.uiState.phase,
                        customerVariant: viewModel.uiState.customerVariant,
                        stars: viewModel.uiState.stars
                    )
                    .ignoresSafeArea()

                    // 2D HUD Overlay
                    VStack {
                        // Top HUD
                        TopHudView(
                            level: viewModel.uiState.level,
                            coins: viewModel.uiState.coins,
                            stars: viewModel.uiState.stars,
                            onSettingsTapped: {
                                viewModel.openSettings()
                            },
                            onRestartTapped: {
                                viewModel.restartLevel()
                            }
                        )

                        Spacer()

                        // Center-Left MATCH Preview Box (Hidden during Customer Reaction)
                        if viewModel.uiState.phase != .customerReaction {
                            HStack {
                                MatchPreviewBox(
                                    targetLayers: viewModel.uiState.targetConfig.targetLayers,
                                    hasCherry: viewModel.uiState.targetConfig.hasCherry
                                )
                                .padding(.leading, 12)

                                Spacer()
                            }
                        }

                        Spacer()

                        // Bottom Flavor Buttons (Hidden during Customer Reaction)
                        if viewModel.uiState.phase != .customerReaction {
                            FlavorButtonsBar(
                                flavors: viewModel.uiState.targetConfig.availableFlavors,
                                activeFlavors: viewModel.uiState.activeFlavors,
                                showTutorial: viewModel.uiState.showTutorial,
                                onFlavorPressed: { flavor in
                                    viewModel.onFlavorPressed(flavor)
                                },
                                onFlavorReleased: { flavor in
                                    viewModel.onFlavorReleased(flavor)
                                }
                            )
                        }
                    }

                    // Results Overlay
                    if viewModel.uiState.showResultsOverlay {
                        ResultsOverlayView(
                            stars: viewModel.uiState.stars,
                            matchScore: viewModel.uiState.matchScore,
                            earnedCoins: viewModel.uiState.earnedCoins,
                            onNextLevel: {
                                viewModel.nextLevel()
                            },
                            onReplayLevel: {
                                viewModel.restartLevel()
                            }
                        )
                    }

                    // Settings Dialog
                    if viewModel.uiState.showSettingsDialog {
                        SettingsDialogView(
                            level: viewModel.uiState.level,
                            coins: viewModel.uiState.coins,
                            isSoundEnabled: viewModel.uiState.isSoundEnabled,
                            isHapticsEnabled: viewModel.uiState.isHapticsEnabled,
                            onToggleSound: {
                                viewModel.toggleSound()
                            },
                            onToggleHaptics: {
                                viewModel.toggleHaptics()
                            },
                            onResetProgress: {
                                viewModel.resetProgress()
                            },
                            onDismiss: {
                                viewModel.closeSettings()
                            }
                        )
                    }
                }
            }
        }
    }
}
