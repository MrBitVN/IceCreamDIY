import SwiftUI

struct TopHudView: View {
    let level: Int
    let coins: Int
    let stars: Int
    let onSettingsTapped: () -> Void
    let onRestartTapped: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            ZStack(alignment: .top) {
                // Left elements: Settings button and Coin pill below it
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 8) {
                        Button(action: onSettingsTapped) {
                            ZStack {
                                Circle()
                                    .fill(Color.white.opacity(0.85))
                                    .frame(width: 38, height: 38)
                                    .shadow(color: .black.opacity(0.12), radius: 2, y: 1)
                                
                                Image(systemName: "gearshape.fill")
                                    .font(.system(size: 20))
                                    .foregroundColor(Color.gray.opacity(0.85))
                            }
                        }

                        CoinPillView(coins: coins)
                    }

                    Spacer()

                    // Right element: Restart button
                    Button(action: onRestartTapped) {
                        ZStack {
                            Circle()
                                .fill(Color.white.opacity(0.85))
                                .frame(width: 38, height: 38)
                                .shadow(color: .black.opacity(0.12), radius: 2, y: 1)
                            
                            Image(systemName: "arrow.counterclockwise")
                                .font(.system(size: 19, weight: .bold))
                                .foregroundColor(Color.gray.opacity(0.85))
                        }
                    }
                }

                // Center elements: 3 Stars & LEVEL capsule
                VStack(spacing: 6) {
                    // 3 Stars in arc
                    HStack(spacing: 8) {
                        starView(isFilled: stars >= 1, angle: -8)
                        starView(isFilled: stars >= 2, angle: 0)
                            .offset(y: -4)
                        starView(isFilled: stars >= 3, angle: 8)
                    }

                    // LEVEL N badge (turns green when stars > 0)
                    ZStack {
                        Capsule()
                            .fill(stars > 0 ? Color(red: 0.46, green: 0.85, blue: 0.16) : Color.white)
                            .frame(width: 110, height: 30)
                            .shadow(color: .black.opacity(0.12), radius: 2, y: 1)

                        Text("LEVEL \(level)")
                            .font(.system(size: 14, weight: .black, design: .rounded))
                            .foregroundColor(stars > 0 ? .white : GameColors.textDark)
                    }
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 8)
    }

    private func starView(isFilled: Bool, angle: Double) -> some View {
        ZStack {
            Image(systemName: isFilled ? "star.fill" : "star")
                .font(.system(size: 28, weight: .black))
                .foregroundColor(isFilled ? GameColors.starFilled : .white)
                .shadow(color: .black.opacity(0.15), radius: 2, y: 1)
        }
        .rotationEffect(.degrees(angle))
    }
}
