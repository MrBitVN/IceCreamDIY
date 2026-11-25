import SwiftUI

struct ResultsOverlayView: View {
    let stars: Int
    let matchScore: Int
    let earnedCoins: Int
    var targetLayers: [TargetLayer] = []
    var userSegments: [UserPouredSegment] = []
    var hasCherry: Bool = false
    var isRewardDoubled: Bool = false
    var onDoubleReward: () -> Void = {}
    let onNextLevel: () -> Void
    let onReplayLevel: () -> Void

    @State private var starScale1: CGFloat = 0.0
    @State private var starScale2: CGFloat = 0.0
    @State private var starScale3: CGFloat = 0.0
    @State private var displayedCoins: Int = 0

    var body: some View {
        ZStack {
            Color.black.opacity(0.38)
                .ignoresSafeArea()

            VStack(spacing: 14) {
                // Title & Match %
                VStack(spacing: 2) {
                    Text(titleText)
                        .font(.system(size: 26, weight: .black, design: .rounded))
                        .foregroundColor(Color(red: 1.0, green: 0.25, blue: 0.43))

                    Text("\(matchScore)% MATCH")
                        .font(.system(size: 15, weight: .bold, design: .rounded))
                        .foregroundColor(Color(white: 0.45))
                }

                // 3 Stars Row
                HStack(spacing: 10) {
                    Image(systemName: "star.fill")
                        .font(.system(size: 42))
                        .foregroundColor(stars >= 1 ? Color(red: 1.0, green: 0.76, blue: 0.03) : Color(white: 0.85))
                        .scaleEffect(stars >= 1 ? starScale1 : 0.8)

                    Image(systemName: "star.fill")
                        .font(.system(size: 54))
                        .foregroundColor(stars >= 2 ? Color(red: 1.0, green: 0.76, blue: 0.03) : Color(white: 0.85))
                        .offset(y: -6)
                        .scaleEffect(stars >= 2 ? starScale2 : 0.8)

                    Image(systemName: "star.fill")
                        .font(.system(size: 42))
                        .foregroundColor(stars >= 3 ? Color(red: 1.0, green: 0.76, blue: 0.03) : Color(white: 0.85))
                        .scaleEffect(stars >= 3 ? starScale3 : 0.8)
                }
                .padding(.vertical, 2)

                // Side-by-side 2 Ice Creams Comparison (TARGET vs YOURS)
                HStack(spacing: 20) {
                    MiniComparisonConeView(
                        label: "TARGET",
                        layers: targetLayers.map { ($0.flavors, $0.proportion) },
                        hasCherry: hasCherry
                    )

                    Text("VS")
                        .font(.system(size: 14, weight: .black, design: .rounded))
                        .foregroundColor(Color(red: 1.0, green: 0.49, blue: 0.61))

                    MiniComparisonConeView(
                        label: "YOURS",
                        layers: summarizeUserSegments(userSegments),
                        hasCherry: hasCherry && stars > 0
                    )
                }
                .padding(.vertical, 10)
                .padding(.horizontal, 18)
                .background(Color(red: 1.0, green: 0.96, blue: 0.97))
                .cornerRadius(18)
                .overlay(
                    RoundedRectangle(cornerRadius: 18)
                        .stroke(Color(red: 1.0, green: 0.84, blue: 0.88), lineWidth: 2)
                )

                // Earned Coins Count-up Badge
                HStack(spacing: 8) {
                    ZStack {
                        Circle()
                            .fill(Color(red: 1.0, green: 0.70, blue: 0.0))
                            .frame(width: 26, height: 26)
                        Text("$")
                            .font(.system(size: 15, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                    }

                    Text("+\(displayedCoins) COINS")
                        .font(.system(size: 17, weight: .black, design: .rounded))
                        .foregroundColor(Color(red: 1.0, green: 0.55, blue: 0.0))
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 8)
                .background(Color(red: 1.0, green: 0.96, blue: 0.88))
                .cornerRadius(20)
                .overlay(
                    RoundedRectangle(cornerRadius: 20)
                        .stroke(Color(red: 1.0, green: 0.70, blue: 0.0), lineWidth: 2)
                )

                // 2X Coins Placeholder Button
                if !isRewardDoubled && earnedCoins > 0 {
                    Button(action: onDoubleReward) {
                        HStack(spacing: 6) {
                            Image(systemName: "play.fill")
                                .font(.system(size: 14, weight: .black))
                            Text("2X COINS (+\(earnedCoins))")
                                .font(.system(size: 16, weight: .black, design: .rounded))
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 46)
                        .background(
                            LinearGradient(
                                colors: [Color(red: 1.0, green: 0.76, blue: 0.03), Color(red: 1.0, green: 0.58, blue: 0.0)],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                        .cornerRadius(23)
                        .shadow(color: Color.orange.opacity(0.35), radius: 5, y: 3)
                    }
                }

                // Next Level Button (Green Glossy) when stars >= 1
                if stars >= 1 {
                    Button(action: onNextLevel) {
                        Text("NEXT LEVEL")
                            .font(.system(size: 19, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(
                                LinearGradient(
                                    colors: [Color(red: 0.30, green: 0.69, blue: 0.31), Color(red: 0.22, green: 0.56, blue: 0.24)],
                                    startPoint: .top,
                                    endPoint: .bottom
                                )
                            )
                            .cornerRadius(26)
                            .shadow(color: Color.green.opacity(0.35), radius: 8, y: 4)
                    }
                }

                // Replay Button
                Button(action: onReplayLevel) {
                    HStack(spacing: 6) {
                        Image(systemName: "arrow.counterclockwise")
                            .font(.system(size: 15, weight: .bold))
                        Text("Replay")
                            .font(.system(size: 15, weight: .bold, design: .rounded))
                    }
                    .foregroundColor(Color(white: 0.40))
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                    .background(Color(white: 0.93))
                    .cornerRadius(22)
                }
            }
            .padding(22)
            .background(Color.white)
            .cornerRadius(32)
            .overlay(
                RoundedRectangle(cornerRadius: 32)
                    .stroke(Color(red: 1.0, green: 0.58, blue: 0.66), lineWidth: 3.5)
            )
            .shadow(color: Color.black.opacity(0.2), radius: 20)
            .padding(.horizontal, 24)
        }
        .onAppear {
            animateStars()
            animateCoinsCountUp(to: earnedCoins)
        }
        .onChange(of: earnedCoins) { newVal in
            animateCoinsCountUp(to: newVal)
        }
    }

    private var titleText: String {
        switch stars {
        case 3: return "PERFECT!"
        case 2: return "GREAT JOB!"
        case 1: return "GOOD TRY!"
        default: return "TRY AGAIN!"
        }
    }

    private func animateStars() {
        if stars >= 1 {
            withAnimation(.spring(response: 0.4, dampingFraction: 0.55)) {
                starScale1 = 1.0
            }
        }
        if stars >= 2 {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.14) {
                withAnimation(.spring(response: 0.4, dampingFraction: 0.55)) {
                    starScale2 = 1.2
                }
            }
        }
        if stars >= 3 {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.28) {
                withAnimation(.spring(response: 0.4, dampingFraction: 0.55)) {
                    starScale3 = 1.0
                }
            }
        }
    }

    private func animateCoinsCountUp(to target: Int) {
        let start = displayedCoins
        let diff = target - start
        guard diff > 0 else {
            displayedCoins = target
            return
        }
        let steps = 20
        for step in 1...steps {
            DispatchQueue.main.asyncAfter(deadline: .now() + Double(step) * 0.03) {
                displayedCoins = start + (diff * step) / steps
            }
        }
    }

    private func summarizeUserSegments(_ segments: [UserPouredSegment]) -> [([Flavor], Float)] {
        guard !segments.isEmpty else { return [] }
        var grouped: [([Flavor], Float)] = []
        var currentFlavors = Array(segments[0].flavors).sorted { $0.rawValue < $1.rawValue }
        var currentHeight: Float = 0

        for seg in segments {
            let fList = Array(seg.flavors).sorted { $0.rawValue < $1.rawValue }
            let h = max(0, seg.heightEnd - seg.heightStart)
            if fList == currentFlavors {
                currentHeight += h
            } else {
                if currentHeight > 0 { grouped.append((currentFlavors, currentHeight)) }
                currentFlavors = fList
                currentHeight = h
            }
        }
        if currentHeight > 0 { grouped.append((currentFlavors, currentHeight)) }
        return grouped
    }
}

private struct MiniComparisonConeView: View {
    let label: String
    let layers: [([Flavor], Float)]
    let hasCherry: Bool

    var body: some View {
        VStack(spacing: 4) {
            Canvas { context, size in
                let w = size.width
                let h = size.height

                var conePath = Path()
                conePath.move(to: CGPoint(x: w * 0.25, y: h * 0.62))
                conePath.addLine(to: CGPoint(x: w * 0.75, y: h * 0.62))
                conePath.addLine(to: CGPoint(x: w * 0.50, y: h * 0.96))
                conePath.closeSubpath()
                context.fill(conePath, with: .color(Color(red: 0.92, green: 0.75, blue: 0.49)))

                let totalRatio = max(0.01, layers.reduce(0) { $0 + $1.1 })
                var currentBottom = h * 0.62
                let maxSwirlH = h * 0.44

                for (idx, item) in layers.enumerated() {
                    let layerH = CGFloat(item.1 / totalRatio) * maxSwirlH
                    let topY = currentBottom - layerH
                    let widthFrac = 0.48 - (CGFloat(idx) / CGFloat(layers.count + 1)) * 0.22
                    let primary = item.0.first?.color ?? Color(red: 0.95, green: 0.90, blue: 0.67)
                    let radius = w * widthFrac * 0.5
                    let centerY = (currentBottom + topY) * 0.5

                    let c1 = Path(ellipseIn: CGRect(x: w * 0.5 - radius, y: centerY - radius, width: radius * 2, height: radius * 2))
                    context.fill(c1, with: .color(primary))

                    if item.0.count >= 2 {
                        let r2 = w * widthFrac * 0.26
                        let c2 = Path(ellipseIn: CGRect(x: w * 0.54 - r2, y: centerY - r2, width: r2 * 2, height: r2 * 2))
                        context.fill(c2, with: .color(item.0[1].color))
                    }
                    currentBottom = topY
                }

                if hasCherry {
                    let cr = w * 0.08
                    let cy = max(h * 0.12, currentBottom - 4)
                    let cherryPath = Path(ellipseIn: CGRect(x: w * 0.5 - cr, y: cy - cr, width: cr * 2, height: cr * 2))
                    context.fill(cherryPath, with: .color(Color(red: 0.92, green: 0.08, blue: 0.16)))
                }
            }
            .frame(width: 64, height: 84)

            Text(label)
                .font(.system(size: 11, weight: .heavy, design: .rounded))
                .foregroundColor(Color(white: 0.45))
        }
    }
}
