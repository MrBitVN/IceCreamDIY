import SwiftUI

struct SplashScreen: View {
    @ObservedObject var viewModel: GameViewModel
    @State private var scale: CGFloat = 0.8
    @State private var opacity: Double = 0.0

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [GameColors.bgGradientTop, GameColors.bgGradientBottom],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 18) {
                Text("🍦")
                    .font(.system(size: 80))
                    .scaleEffect(scale)

                Text("Ice Cream DIY")
                    .font(.system(size: 34, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .shadow(color: .black.opacity(0.2), radius: 3, y: 2)

                Text("Simulator 3D")
                    .font(.system(size: 18, weight: .bold, design: .rounded))
                    .foregroundColor(Color.white.opacity(0.85))
            }
            .opacity(opacity)
            .onAppear {
                withAnimation(.easeOut(duration: 0.8)) {
                    scale = 1.0
                    opacity = 1.0
                }
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
                    withAnimation {
                        viewModel.navigateToHome()
                    }
                }
            }
        }
    }
}
