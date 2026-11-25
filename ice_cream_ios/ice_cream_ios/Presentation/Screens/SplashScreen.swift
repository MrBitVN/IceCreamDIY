import SwiftUI

struct SplashScreen: View {
    @ObservedObject var viewModel: GameViewModel
    @State private var bounceScale: CGFloat = 0.92

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [GameColors.bgGradientTop, GameColors.bgGradientBottom],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 16) {
                Text("🍦")
                    .font(.system(size: 90))
                    .scaleEffect(bounceScale)

                VStack(spacing: 6) {
                    Text("Ice Cream DIY")
                        .font(.system(size: 38, weight: .black, design: .rounded))
                        .foregroundColor(.white)

                    Text("3D SIMULATOR")
                        .font(.system(size: 18, weight: .bold, design: .rounded))
                        .foregroundColor(Color.white.opacity(0.92))
                        .tracking(4)
                }
            }
        }
        .onAppear {
            withAnimation(
                .easeInOut(duration: 0.65)
                .repeatForever(autoreverses: true)
            ) {
                bounceScale = 1.08
            }

            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                viewModel.navigateToHome()
            }
        }
    }
}
