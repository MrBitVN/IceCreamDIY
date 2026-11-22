import SwiftUI

struct CoinPillView: View {
    let coins: Int

    var body: some View {
        HStack(spacing: 6) {
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [Color(red: 1.0, green: 0.88, blue: 0.3), Color(red: 0.95, green: 0.65, blue: 0.1)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 26, height: 26)
                    .shadow(color: .black.opacity(0.15), radius: 2, y: 1)
                
                Text("$")
                    .font(.system(size: 14, weight: .black, design: .rounded))
                    .foregroundColor(.white)
            }

            Text("\(coins)")
                .font(.system(size: 16, weight: .heavy, design: .rounded))
                .foregroundColor(GameColors.textDark)
                .padding(.trailing, 8)
        }
        .padding(.leading, 3)
        .padding(.vertical, 3)
        .background(
            Capsule()
                .fill(Color.white)
                .shadow(color: .black.opacity(0.12), radius: 3, y: 2)
        )
    }
}
