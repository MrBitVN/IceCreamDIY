import SwiftUI

struct BlenderMinigameView: View {
    let level: Int
    let coins: Int
    let onBlenderComplete: () -> Void
    let onSettingsTapped: () -> Void
    let onRestartTapped: () -> Void

    @State private var fillProgress: Float = 0.05
    @State private var showAwesomeText: Bool = false

    var body: some View {
        ZStack {
            // Pink Gradient Background
            LinearGradient(
                colors: [GameColors.bgGradientTop, GameColors.bgGradientBottom],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            // 1. 3D Procedural SceneKit Blender View (3 Tapered Cups, Rising Liquid, Bubbles, Agitators)
            BlenderMinigameSceneView(fillProgress: fillProgress)
                .ignoresSafeArea()

            // 2. 2D HUD & Labels
            VStack {
                TopHudView(
                    level: level,
                    coins: coins,
                    stars: 0,
                    onSettingsTapped: onSettingsTapped,
                    onRestartTapped: onRestartTapped
                )

                Spacer().frame(height: 16)

                Text("BLENDING JUICES...")
                    .font(.system(size: 18, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .tracking(1.5)
                    .shadow(color: .black.opacity(0.18), radius: 2, y: 1)

                Spacer()

                // Cup Name Pills
                HStack(spacing: 52) {
                    cupLabel("MANGO")
                    cupLabel("BERRY")
                    cupLabel("MINT")
                }
                .padding(.bottom, 28)

                // AWESOME! Bouncy Pop Banner
                if showAwesomeText {
                    ZStack {
                        Capsule()
                            .fill(
                                LinearGradient(
                                    colors: [Color(red: 1.0, green: 0.60, blue: 0.0), Color(red: 1.0, green: 0.32, blue: 0.32)],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .frame(width: 250, height: 60)
                            .overlay(
                                Capsule()
                                    .stroke(Color.white, lineWidth: 3)
                            )
                            .shadow(color: .black.opacity(0.20), radius: 10, y: 5)

                        Text("AWESOME!")
                            .font(.system(size: 32, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                            .tracking(2)
                    }
                    .transition(.scale(scale: 0.1).combined(with: .opacity))
                    .padding(.bottom, 60)
                } else {
                    Spacer().frame(height: 60)
                }
            }
        }
        .onAppear {
            withAnimation(.linear(duration: 1.8).delay(0.25)) {
                fillProgress = 1.0
            }

            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                withAnimation(.interpolatingSpring(stiffness: 170, damping: 10)) {
                    showAwesomeText = true
                }
            }

            DispatchQueue.main.asyncAfter(deadline: .now() + 3.4) {
                onBlenderComplete()
            }
        }
    }

    private func cupLabel(_ text: String) -> some View {
        ZStack {
            Capsule()
                .fill(Color.white.opacity(0.92))
                .frame(width: 76, height: 26)
                .shadow(color: .black.opacity(0.12), radius: 2, y: 1)

            Text(text)
                .font(.system(size: 11, weight: .black, design: .rounded))
                .foregroundColor(GameColors.textDark)
        }
    }
}
