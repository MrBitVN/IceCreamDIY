import SwiftUI

struct HomeScreen: View {
    @ObservedObject var viewModel: GameViewModel
    @State private var toastMessage: String? = nil

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [GameColors.bgGradientTop, GameColors.bgGradientBottom],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                // Top status bar
                HStack {
                    CoinPillView(coins: viewModel.uiState.coins)

                    Spacer()

                    ZStack {
                        Capsule()
                            .fill(Color.white)
                            .frame(width: 120, height: 36)
                            .shadow(color: .black.opacity(0.12), radius: 3, y: 1)

                        Text("LEVEL \(viewModel.uiState.level)")
                            .font(.system(size: 15, weight: .black, design: .rounded))
                            .foregroundColor(GameColors.textDark)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)

                Spacer()

                // Title
                Text("ICE CREAM DIY")
                    .font(.system(size: 32, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .shadow(color: .black.opacity(0.15), radius: 4, y: 2)

                // 3D Procedural Waving Customer SceneView
                HomeCustomerSceneView()
                    .frame(maxWidth: .infinity)
                    .frame(height: 320)
                    .padding(.horizontal, 24)

                Spacer()

                // Big Green START Button
                Button(action: {
                    viewModel.startGame()
                }) {
                    HStack(spacing: 10) {
                        Image(systemName: "play.fill")
                            .font(.system(size: 26, weight: .black))
                            .foregroundColor(.white)

                        Text("START")
                            .font(.system(size: 26, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                            .tracking(2)
                    }
                    .frame(width: 220, height: 72)
                    .background(
                        LinearGradient(
                            colors: [Color(red: 0.55, green: 0.91, blue: 0.22), Color(red: 0.34, green: 0.72, blue: 0.09)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                    .cornerRadius(36)
                    .overlay(
                        RoundedRectangle(cornerRadius: 36)
                            .stroke(Color.white.opacity(0.85), lineWidth: 3)
                    )
                    .shadow(color: Color.green.opacity(0.4), radius: 10, y: 5)
                }
                .padding(.bottom, 48)
            }

            // Right side icon column: VIP, Shop, Spin
            HStack {
                Spacer()

                VStack(spacing: 16) {
                    HomeSideIconButton(
                        systemName: "star.fill",
                        label: "VIP",
                        bgColor: Color(red: 1.0, green: 0.70, blue: 0.0),
                        action: { showToast("VIP Club coming soon!") }
                    )

                    HomeSideIconButton(
                        systemName: "cart.fill",
                        label: "Shop",
                        bgColor: Color(red: 0.26, green: 0.65, blue: 0.96),
                        action: { showToast("Shop coming soon!") }
                    )

                    HomeSideIconButton(
                        systemName: "gift.fill",
                        label: "Spin",
                        bgColor: Color(red: 0.67, green: 0.28, blue: 0.74),
                        action: { showToast("Lucky Spin coming soon!") }
                    )
                }
                .padding(.trailing, 16)
            }

            // Toast Alert
            if let message = toastMessage {
                VStack {
                    Spacer()
                    Text(message)
                        .font(.system(size: 14, weight: .bold, design: .rounded))
                        .foregroundColor(.white)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 10)
                        .background(Color.black.opacity(0.75))
                        .cornerRadius(20)
                        .padding(.bottom, 130)
                }
                .transition(.opacity)
            }
        }
    }

    private func showToast(_ message: String) {
        withAnimation {
            toastMessage = message
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) {
            withAnimation {
                toastMessage = nil
            }
        }
    }
}

struct HomeSideIconButton: View {
    let systemName: String
    let label: String
    let bgColor: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                ZStack {
                    Circle()
                        .fill(bgColor)
                        .frame(width: 52, height: 52)
                        .overlay(
                            Circle()
                                .stroke(Color.white, lineWidth: 2.5)
                        )
                        .shadow(color: .black.opacity(0.18), radius: 4, y: 2)

                    Image(systemName: systemName)
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(.white)
                }

                Text(label)
                    .font(.system(size: 12, weight: .bold, design: .rounded))
                    .foregroundColor(.white)
            }
        }
    }
}
