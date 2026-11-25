import SwiftUI

struct FloatingEmojiData: Identifiable {
    let id: UUID = UUID()
    let emoji: String
    let position: CGPoint
}

struct FlyingCoinData: Identifiable {
    let id: UUID = UUID()
    let amount: Int
    let startPosition: CGPoint
}

struct SlashPointItem: Identifiable {
    let id: UUID = UUID()
    let x: CGFloat
    let y: CGFloat
    let timestamp: Date
}

struct MinigameBoardView: View {
    let level: Int
    let coins: Int
    let onMinigameComplete: () -> Void
    let onAddCoins: (Int) -> Void
    let onSettingsTapped: () -> Void
    let onRestartTapped: () -> Void

    @State private var slicedItems: Set<MinigameIngredient> = []
    @State private var sortedItems: Set<MinigameIngredient> = []
    @State private var progress: CGFloat = 0.0

    @State private var slashPoints: [SlashPointItem] = []
    @State private var floatingEmojis: [FloatingEmojiData] = []
    @State private var flyingCoins: [FlyingCoinData] = []

    private let coinPillTarget: CGPoint = CGPoint(x: 75, y: 55)

    var body: some View {
        ZStack {
            // Pink Gradient Background
            LinearGradient(
                colors: [GameColors.bgGradientTop, GameColors.bgGradientBottom],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            // 1. Procedural 3D SceneKit View (Cutting Board, Ingredients, Knife, 3-Compartment Box)
            SlicingMinigameSceneView(
                onKnifeSlice: { ingredient, screenX, screenY in
                    slicedItems.insert(ingredient)
                    updateProgress()

                    floatingEmojis.append(
                        FloatingEmojiData(
                            emoji: "😊",
                            position: CGPoint(x: screenX, y: screenY - 30)
                        )
                    )
                },
                onSwipeTrail: { screenX, screenY in
                    let now = Date()
                    slashPoints.append(SlashPointItem(x: screenX, y: screenY, timestamp: now))
                    if slashPoints.count > 16 {
                        slashPoints.removeFirst()
                    }
                },
                onIngredientSorted: { ingredient, screenX, screenY in
                    guard !sortedItems.contains(ingredient) else { return }
                    sortedItems.insert(ingredient)
                    updateProgress()

                    // Floating Reward Emoji
                    floatingEmojis.append(
                        FloatingEmojiData(
                            emoji: ingredient.rewardEmoji,
                            position: CGPoint(x: screenX, y: screenY - 45)
                        )
                    )

                    // Milestone coins (+5 at 2 items, +10 at 3 items)
                    let bonus = MinigameEngine.milestoneCoinsForCount(sortedItems.count)
                    if bonus > 0 {
                        onAddCoins(bonus)
                        flyingCoins.append(
                            FlyingCoinData(
                                amount: bonus,
                                startPosition: CGPoint(x: screenX, y: screenY)
                            )
                        )
                    }

                    // Complete minigame when all 3 ingredients are sorted
                    if sortedItems.count >= 3 {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.1) {
                            onMinigameComplete()
                        }
                    }
                },
                onWrongCompartment: {
                    // Feedback on wrong compartment
                }
            )
            .ignoresSafeArea()

            // 2. Knife Slash Trail Line
            Canvas { context, _ in
                guard slashPoints.count >= 2 else { return }
                for i in 1..<slashPoints.count {
                    let p0 = CGPoint(x: slashPoints[i - 1].x, y: slashPoints[i - 1].y)
                    let p1 = CGPoint(x: slashPoints[i].x, y: slashPoints[i].y)
                    let fraction = CGFloat(i) / CGFloat(slashPoints.count)

                    var path = Path()
                    path.move(to: p0)
                    path.addLine(to: p1)

                    context.stroke(
                        path,
                        with: .color(Color.white.opacity(0.85 * Double(fraction))),
                        lineWidth: 8 * fraction
                    )
                }
            }
            .allowsHitTesting(false)
            .ignoresSafeArea()

            // 3. 2D HUD Overlay
            VStack(spacing: 0) {
                // Top HUD
                TopHudView(
                    level: level,
                    coins: coins,
                    stars: 0,
                    onSettingsTapped: onSettingsTapped,
                    onRestartTapped: onRestartTapped
                )

                Spacer().frame(height: 10)

                // Level Capsule & Milestone Progress bar
                VStack(spacing: 6) {
                    ZStack {
                        Capsule()
                            .fill(Color.white)
                            .frame(width: 110, height: 30)
                            .shadow(color: .black.opacity(0.12), radius: 2, y: 1)

                        Text("LEVEL \(level)")
                            .font(.system(size: 14, weight: .black, design: .rounded))
                            .foregroundColor(GameColors.textDark)
                    }

                    // Milestone Bar (+5, +10) matching reference photo 4
                    ZStack(alignment: .leading) {
                        Capsule()
                            .fill(Color.white)
                            .frame(width: 180, height: 26)
                            .shadow(color: .black.opacity(0.10), radius: 2, y: 1)

                        Capsule()
                            .fill(GameColors.progressGreen)
                            .frame(width: max(8, 180 * progress), height: 22)
                            .padding(.leading, 2)
                            .animation(.spring(response: 0.45, dampingFraction: 0.7), value: progress)

                        HStack {
                            Spacer()
                            Text("+5")
                                .font(.system(size: 11, weight: .bold, design: .rounded))
                                .foregroundColor(GameColors.textDark)
                            Spacer()
                            Text("+10")
                                .font(.system(size: 11, weight: .bold, design: .rounded))
                                .foregroundColor(GameColors.textDark)
                            Spacer()
                        }
                        .frame(width: 180)
                    }
                }

                Spacer().frame(height: 12)

                Text("SWIPE TO SLICE & SORT!")
                    .font(.system(size: 15, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .tracking(1.2)
                    .shadow(color: .black.opacity(0.18), radius: 2, y: 1)

                Spacer()
            }

            // 4. Floating Reward Emojis
            ForEach(floatingEmojis) { item in
                FloatingEmojiView(data: item)
            }

            // 5. Flying Coins into Coin Pill
            ForEach(flyingCoins) { coin in
                FlyingCoinView(data: coin, target: coinPillTarget)
            }
        }
    }

    private func updateProgress() {
        let count = sortedItems.count
        if count == 3 {
            progress = 1.0
        } else if count == 2 {
            progress = 0.67
        } else if count == 1 {
            progress = 0.33
        } else {
            progress = CGFloat(slicedItems.count) * 0.08
        }
    }
}

struct FloatingEmojiView: View {
    let data: FloatingEmojiData
    @State private var offsetY: CGFloat = 0
    @State private var opacity: Double = 1.0

    var body: some View {
        Text(data.emoji)
            .font(.system(size: 34))
            .position(x: data.position.x, y: data.position.y + offsetY)
            .opacity(opacity)
            .onAppear {
                withAnimation(.easeOut(duration: 1.1)) {
                    offsetY = -90
                    opacity = 0.0
                }
            }
            .allowsHitTesting(false)
    }
}

struct FlyingCoinView: View {
    let data: FlyingCoinData
    let target: CGPoint

    @State private var currentPos: CGPoint = .zero
    @State private var opacity: Double = 1.0
    @State private var scale: CGFloat = 1.0

    var body: some View {
        ZStack {
            Capsule()
                .fill(Color(red: 1.0, green: 0.80, blue: 0.0))
                .frame(width: 44, height: 26)
                .overlay(Capsule().stroke(Color(red: 0.83, green: 0.53, blue: 0.02), lineWidth: 2))
                .shadow(color: .black.opacity(0.18), radius: 3, y: 2)

            Text("+\(data.amount)")
                .font(.system(size: 12, weight: .black, design: .rounded))
                .foregroundColor(.white)
        }
        .scaleEffect(scale)
        .position(currentPos)
        .opacity(opacity)
        .onAppear {
            currentPos = data.startPosition
            withAnimation(.easeInOut(duration: 0.75)) {
                currentPos = target
                scale = 1.2
            }
            withAnimation(.easeIn(duration: 0.2).delay(0.65)) {
                opacity = 0.0
            }
        }
        .allowsHitTesting(false)
    }
}
