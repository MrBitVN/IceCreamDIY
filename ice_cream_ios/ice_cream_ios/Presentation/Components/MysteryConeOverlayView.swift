import SwiftUI

struct MysteryConeOverlayView: View {
    let level: Int
    let onNextTapped: () -> Void

    @State private var cardScale: CGFloat = 0.75
    @State private var animatedProgress: CGFloat = 0.0

    private var targetStep: Int {
        ((level / 2) - 1 + 4) % 4 + 1
    }

    private var targetProgress: CGFloat {
        CGFloat(targetStep) / 4.0
    }

    var body: some View {
        ZStack {
            Color.black.opacity(0.45)
                .ignoresSafeArea()

            VStack(spacing: 18) {
                Text("MYSTERY CONE")
                    .font(.system(size: 24, weight: .black, design: .rounded))
                    .foregroundColor(Color(red: 1.0, green: 0.25, blue: 0.43))
                    .tracking(1.2)

                // Black Mystery Cone Silhouette with '?'
                ZStack {
                    Circle()
                        .fill(
                            RadialGradient(
                                colors: [Color(red: 1.0, green: 0.89, blue: 0.93), Color(red: 1.0, green: 0.96, blue: 0.97)],
                                center: .center,
                                startRadius: 5,
                                endRadius: 75
                            )
                        )
                        .frame(width: 150, height: 150)
                        .overlay(
                            Circle()
                                .stroke(Color(red: 1.0, green: 0.70, blue: 0.77), lineWidth: 3)
                        )

                    Canvas { context, size in
                        let w = size.width
                        let h = size.height

                        // Dark Ice Cream Swirl Silhouette
                        let circle1 = Path(ellipseIn: CGRect(x: w * 0.26, y: h * 0.12, width: w * 0.48, height: w * 0.44))
                        context.fill(circle1, with: .color(Color(red: 0.12, green: 0.13, blue: 0.16)))

                        let circle2 = Path(ellipseIn: CGRect(x: w * 0.31, y: h * 0.04, width: w * 0.38, height: w * 0.34))
                        context.fill(circle2, with: .color(Color(red: 0.16, green: 0.17, blue: 0.21)))

                        // Black Waffle Cone Silhouette
                        var conePath = Path()
                        conePath.move(to: CGPoint(x: w * 0.26, y: h * 0.42))
                        conePath.addLine(to: CGPoint(x: w * 0.74, y: h * 0.42))
                        conePath.addLine(to: CGPoint(x: w * 0.50, y: h * 0.92))
                        conePath.closeSubpath()
                        context.fill(conePath, with: .color(Color(red: 0.09, green: 0.10, blue: 0.13)))
                    }
                    .frame(width: 110, height: 110)

                    Text("?")
                        .font(.system(size: 44, weight: .black, design: .rounded))
                        .foregroundColor(Color(red: 1.0, green: 0.76, blue: 0.03))
                }

                // Progress Text & Bar
                VStack(spacing: 8) {
                    let pct = Int(animatedProgress * 100)
                    Text(pct >= 100 ? "UNLOCKED! (\(pct)%)" : "UNLOCKING... \(pct)%")
                        .font(.system(size: 14, weight: .heavy, design: .rounded))
                        .foregroundColor(Color(white: 0.35))

                    ZStack(alignment: .leading) {
                        Capsule()
                            .fill(Color(white: 0.93))
                            .frame(width: 220, height: 24)
                            .overlay(Capsule().stroke(Color(white: 0.88), lineWidth: 2))

                        Capsule()
                            .fill(
                                LinearGradient(
                                    colors: [Color(red: 0.30, green: 0.85, blue: 0.39), Color(red: 0.18, green: 0.80, blue: 0.44)],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .frame(width: max(12, 214 * animatedProgress), height: 18)
                            .padding(.leading, 3)
                    }
                }

                // Glossy Green NEXT Button
                Button(action: onNextTapped) {
                    Text("NEXT")
                        .font(.system(size: 20, weight: .black, design: .rounded))
                        .foregroundColor(.white)
                        .tracking(1.5)
                        .frame(maxWidth: .infinity)
                        .frame(height: 54)
                        .background(
                            LinearGradient(
                                colors: [Color(red: 0.30, green: 0.69, blue: 0.31), Color(red: 0.22, green: 0.56, blue: 0.24)],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .cornerRadius(27)
                        .shadow(color: Color.green.opacity(0.35), radius: 8, y: 4)
                }
            }
            .padding(24)
            .background(Color.white)
            .cornerRadius(32)
            .overlay(
                RoundedRectangle(cornerRadius: 32)
                    .stroke(Color(red: 1.0, green: 0.58, blue: 0.66), lineWidth: 4)
            )
            .shadow(color: Color.black.opacity(0.25), radius: 20)
            .padding(.horizontal, 30)
            .scaleEffect(cardScale)
        }
        .onAppear {
            animatedProgress = max(0.0, CGFloat(targetStep - 1) / 4.0)
            withAnimation(.spring(response: 0.45, dampingFraction: 0.65)) {
                cardScale = 1.0
            }
            withAnimation(.easeOut(duration: 0.7).delay(0.15)) {
                animatedProgress = targetProgress
            }
        }
    }
}
