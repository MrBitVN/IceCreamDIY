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

                    // White & Blue Swirling Background during DECORATING
                    if viewModel.uiState.phase == .decorating {
                        WhiteBlueSwirlBackgroundView()
                            .ignoresSafeArea()
                    }

                    // 3D Scene View with Swirl Extrusion, Stacked Cones, Sprinkles, Cherry, Shop & Customer
                    IceCreamSceneView(
                        activeFlavors: viewModel.uiState.activeFlavors,
                        isRotating: viewModel.uiState.isRotating,
                        pourProgress: viewModel.uiState.pourProgress,
                        extraCones: viewModel.uiState.targetConfig.extraCones,
                        sprinklesCount: viewModel.uiState.targetConfig.sprinklesCount,
                        hasCherry: viewModel.uiState.targetConfig.hasCherry,
                        isDecorated: viewModel.uiState.isDecorated,
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

                        // Center-Left MATCH Preview Box (Only during Pouring)
                        if viewModel.uiState.phase == .pouring {
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

                        // Bottom Controls
                        if viewModel.uiState.phase == .pouring {
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
                        } else if viewModel.uiState.phase == .decorating && !viewModel.uiState.isDecorated {
                            DecorateActionButtonView(bonusCoins: viewModel.uiState.decorationBonusCoins) {
                                viewModel.onDecorateTapped()
                            }
                            .padding(.bottom, 34)
                        }
                    }

                    // Results Overlay
                    if viewModel.uiState.showResultsOverlay {
                        ResultsOverlayView(
                            stars: viewModel.uiState.stars,
                            matchScore: viewModel.uiState.matchScore,
                            earnedCoins: viewModel.uiState.earnedCoins,
                            targetLayers: viewModel.uiState.targetConfig.targetLayers,
                            userSegments: viewModel.uiState.userPouredSegments,
                            hasCherry: viewModel.uiState.targetConfig.hasCherry,
                            isRewardDoubled: viewModel.uiState.isRewardDoubled,
                            onDoubleReward: {
                                viewModel.onDoubleRewardTapped()
                            },
                            onNextLevel: {
                                viewModel.onRequestNextLevel()
                            },
                            onReplayLevel: {
                                viewModel.restartLevel()
                            }
                        )
                    }

                    // Mystery Cone Popup
                    if viewModel.uiState.showMysteryCone {
                        MysteryConeOverlayView(
                            level: viewModel.uiState.level,
                            onNextTapped: {
                                viewModel.onDismissMysteryCone()
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

private struct WhiteBlueSwirlBackgroundView: View {
    @State private var angleOffset: Double = 0.0

    var body: some View {
        Canvas { context, size in
            context.fill(Path(CGRect(origin: .zero, size: size)), with: .color(.white))
            let cx = size.width * 0.5
            let cy = size.height * 0.48
            let radius = max(size.width, size.height) * 1.2
            let rayCount = 14
            let stepDeg = 360.0 / Double(rayCount)

            for i in stride(from: 0, to: rayCount, by: 2) {
                let a1 = (Double(i) * stepDeg + angleOffset) * .pi / 180.0
                let a2 = (Double(i + 1) * stepDeg + angleOffset) * .pi / 180.0
                var path = Path()
                path.move(to: CGPoint(x: cx, y: cy))
                path.addLine(to: CGPoint(x: cx + radius * cos(a1), y: cy + radius * sin(a1)))
                path.addLine(to: CGPoint(x: cx + radius * cos(a2), y: cy + radius * sin(a2)))
                path.closeSubpath()
                context.fill(path, with: .color(Color(red: 0.74, green: 0.90, blue: 1.0)))
            }
        }
        .onAppear {
            withAnimation(.linear(duration: 12.0).repeatForever(autoreverses: false)) {
                angleOffset = 360.0
            }
        }
    }
}

private struct DecorateActionButtonView: View {
    let bonusCoins: Int
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Text("DECORATE")
                    .font(.system(size: 22, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .tracking(1.2)

                HStack(spacing: 4) {
                    ZStack {
                        Circle()
                            .fill(Color(red: 1.0, green: 0.60, blue: 0.0))
                            .frame(width: 16, height: 16)
                        Text("$")
                            .font(.system(size: 10, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                    }
                    Text("+\(bonusCoins)")
                        .font(.system(size: 14, weight: .black, design: .rounded))
                        .foregroundColor(.white)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(Color(red: 1.0, green: 0.76, blue: 0.03))
                .cornerRadius(14)
            }
            .frame(width: 280, height: 64)
            .background(
                LinearGradient(
                    colors: [Color(red: 0.26, green: 0.84, blue: 0.45), Color(red: 0.16, green: 0.65, blue: 0.27)],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            .cornerRadius(32)
            .overlay(
                RoundedRectangle(cornerRadius: 32)
                    .stroke(Color.white, lineWidth: 3)
            )
            .shadow(color: Color.green.opacity(0.35), radius: 10, y: 5)
        }
    }
}
