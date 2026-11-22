import SwiftUI

struct HomeScreen: View {
    @ObservedObject var viewModel: GameViewModel

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [GameColors.bgGradientTop, GameColors.bgGradientBottom],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 28) {
                // Top Coin & Level status
                HStack {
                    CoinPillView(coins: viewModel.uiState.coins)
                    Spacer()
                    ZStack {
                        Capsule()
                            .fill(Color.white)
                            .frame(width: 110, height: 32)
                            .shadow(color: .black.opacity(0.12), radius: 2, y: 1)

                        Text("LEVEL \(viewModel.uiState.level)")
                            .font(.system(size: 15, weight: .black, design: .rounded))
                            .foregroundColor(GameColors.textDark)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)

                Spacer()

                // Title
                VStack(spacing: 8) {
                    Text("🍦 ICE CREAM")
                        .font(.system(size: 38, weight: .black, design: .rounded))
                        .foregroundColor(.white)
                        .shadow(color: .black.opacity(0.2), radius: 4, y: 2)

                    Text("DIY SIMULATOR 3D")
                        .font(.system(size: 20, weight: .heavy, design: .rounded))
                        .foregroundColor(Color.white.opacity(0.92))
                        .tracking(2)
                }

                Spacer()

                // Big PLAY Button
                Button(action: {
                    withAnimation {
                        viewModel.startGame()
                    }
                }) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 24)
                            .fill(
                                LinearGradient(
                                    colors: [Color(red: 0.52, green: 0.90, blue: 0.28), Color(red: 0.38, green: 0.78, blue: 0.16)],
                                    startPoint: .top,
                                    endPoint: .bottom
                                )
                            )
                            .frame(width: 200, height: 68)
                            .overlay(
                                RoundedRectangle(cornerRadius: 24)
                                    .stroke(Color.white, lineWidth: 3)
                            )
                            .shadow(color: .black.opacity(0.25), radius: 6, y: 4)

                        HStack(spacing: 10) {
                            Image(systemName: "play.fill")
                                .font(.system(size: 26))
                                .foregroundColor(.white)

                            Text("PLAY")
                                .font(.system(size: 28, weight: .black, design: .rounded))
                                .foregroundColor(.white)
                        }
                    }
                }
                .padding(.bottom, 60)
            }
        }
    }
}
