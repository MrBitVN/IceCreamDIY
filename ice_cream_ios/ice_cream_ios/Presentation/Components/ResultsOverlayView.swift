import SwiftUI

struct ResultsOverlayView: View {
    let stars: Int
    let matchScore: Int
    let earnedCoins: Int
    let onNextLevel: () -> Void
    let onReplayLevel: () -> Void

    @State private var starScale1: CGFloat = 0.0
    @State private var starScale2: CGFloat = 0.0
    @State private var starScale3: CGFloat = 0.0

    var body: some View {
        ZStack {
            Color.black.opacity(0.38)
                .ignoresSafeArea()

            VStack(spacing: 20) {
                // Title
                VStack(spacing: 4) {
                    Text(titleText)
                        .font(.system(size: 28, weight: .black, design: .rounded))
                        .foregroundColor(Color(red: 1.0, green: 0.25, blue: 0.43))

                    Text("\(matchScore)% MATCH")
                        .font(.system(size: 16, weight: .bold, design: .rounded))
                        .foregroundColor(Color(white: 0.45))
                }

                // 3 Stars Row
                HStack(spacing: 12) {
                    Image(systemName: "star.fill")
                        .font(.system(size: 46))
                        .foregroundColor(stars >= 1 ? Color(red: 1.0, green: 0.76, blue: 0.03) : Color(white: 0.85))
                        .scaleEffect(stars >= 1 ? starScale1 : 0.8)

                    Image(systemName: "star.fill")
                        .font(.system(size: 58))
                        .foregroundColor(stars >= 2 ? Color(red: 1.0, green: 0.76, blue: 0.03) : Color(white: 0.85))
                        .offset(y: -6)
                        .scaleEffect(stars >= 2 ? starScale2 : 0.8)

                    Image(systemName: "star.fill")
                        .font(.system(size: 46))
                        .foregroundColor(stars >= 3 ? Color(red: 1.0, green: 0.76, blue: 0.03) : Color(white: 0.85))
                        .scaleEffect(stars >= 3 ? starScale3 : 0.8)
                }
                .padding(.vertical, 4)

                // Earned Coins Badge
                HStack(spacing: 8) {
                    ZStack {
                        Circle()
                            .fill(Color(red: 1.0, green: 0.70, blue: 0.0))
                            .frame(width: 28, height: 28)
                        Text("$")
                            .font(.system(size: 16, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                    }

                    Text("+\(earnedCoins) COINS")
                        .font(.system(size: 18, weight: .black, design: .rounded))
                        .foregroundColor(Color(red: 1.0, green: 0.55, blue: 0.0))
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 10)
                .background(Color(red: 1.0, green: 0.96, blue: 0.88))
                .cornerRadius(20)
                .overlay(
                    RoundedRectangle(cornerRadius: 20)
                        .stroke(Color(red: 1.0, green: 0.70, blue: 0.0), lineWidth: 2)
                )

                // Next Level Button
                Button(action: onNextLevel) {
                    Text("NEXT LEVEL")
                        .font(.system(size: 20, weight: .black, design: .rounded))
                        .foregroundColor(.white)
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

                // Replay Button
                Button(action: onReplayLevel) {
                    HStack(spacing: 6) {
                        Image(systemName: "arrow.counterclockwise")
                            .font(.system(size: 15, weight: .bold))
                        Text("Replay")
                            .font(.system(size: 16, weight: .bold, design: .rounded))
                    }
                    .foregroundColor(Color(white: 0.40))
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .background(Color(white: 0.93))
                    .cornerRadius(22)
                }
            }
            .padding(26)
            .background(Color.white)
            .cornerRadius(32)
            .overlay(
                RoundedRectangle(cornerRadius: 32)
                    .stroke(Color(red: 1.0, green: 0.58, blue: 0.66), lineWidth: 3.5)
            )
            .shadow(color: Color.black.opacity(0.2), radius: 20)
            .padding(.horizontal, 28)
        }
        .onAppear {
            if stars >= 1 {
                withAnimation(.spring(response: 0.4, dampingFraction: 0.55)) {
                    starScale1 = 1.0
                }
            }
            if stars >= 2 {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                    withAnimation(.spring(response: 0.4, dampingFraction: 0.55)) {
                        starScale2 = 1.2
                    }
                }
            }
            if stars >= 3 {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.30) {
                    withAnimation(.spring(response: 0.4, dampingFraction: 0.55)) {
                        starScale3 = 1.0
                    }
                }
            }
        }
    }

    private var titleText: String {
        switch stars {
        case 3: return "PERFECT!"
        case 2: return "GREAT JOB!"
        default: return "GOOD TRY!"
        }
    }
}
