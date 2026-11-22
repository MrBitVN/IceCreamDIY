import SwiftUI

struct MatchPreviewBox: View {
    let targetLayers: [TargetLayer]
    let hasCherry: Bool

    init(targetLayers: [TargetLayer], hasCherry: Bool = false) {
        self.targetLayers = targetLayers
        self.hasCherry = hasCherry
    }

    var body: some View {
        VStack(spacing: 4) {
            ZStack {
                RoundedRectangle(cornerRadius: 14)
                    .fill(Color.white.opacity(0.45))
                    .frame(width: 84, height: 112)
                    .overlay(
                        RoundedRectangle(cornerRadius: 14)
                            .stroke(Color.white.opacity(0.85), lineWidth: 1.5)
                    )
                    .shadow(color: .black.opacity(0.08), radius: 3, y: 2)

                Canvas { context, size in
                    let midX = size.width / 2.0
                    let coneTopY = size.height * 0.60
                    let coneBottomY = size.height * 0.95
                    let coneWidthHalf = size.width * 0.28

                    // 1. Waffle Cone
                    var conePath = Path()
                    conePath.move(to: CGPoint(x: midX - coneWidthHalf, y: coneTopY))
                    conePath.addLine(to: CGPoint(x: midX + coneWidthHalf, y: coneTopY))
                    conePath.addLine(to: CGPoint(x: midX, y: coneBottomY))
                    conePath.closeSubpath()
                    context.fill(conePath, with: .color(GameColors.waffleCone))

                    // Waffle Grid Lines
                    let waffleColor = Color(red: 0.78, green: 0.62, blue: 0.38)
                    for step in -2...2 {
                        let offset = CGFloat(step) * 10.0
                        var line1 = Path()
                        line1.move(to: CGPoint(x: midX - coneWidthHalf + offset, y: coneTopY))
                        line1.addLine(to: CGPoint(x: midX + offset, y: coneBottomY))
                        context.stroke(line1, with: .color(waffleColor), lineWidth: 1.5)

                        var line2 = Path()
                        line2.move(to: CGPoint(x: midX + coneWidthHalf - offset, y: coneTopY))
                        line2.addLine(to: CGPoint(x: midX - offset, y: coneBottomY))
                        context.stroke(line2, with: .color(waffleColor), lineWidth: 1.5)
                    }

                    // 2. Swirl Layers
                    var currentBaseY = coneTopY
                    let totalSwirlHeight = size.height * 0.45

                    for (index, layer) in targetLayers.enumerated() {
                        let layerHeight = totalSwirlHeight * CGFloat(layer.proportion)
                        let layerTopY = currentBaseY - layerHeight

                        let progressBottom = (coneTopY - currentBaseY) / totalSwirlHeight
                        let progressTop = (coneTopY - layerTopY) / totalSwirlHeight
                        let widthBottom = (coneWidthHalf * 2.2) * (1.0 - 0.55 * progressBottom)
                        let widthTop = (coneWidthHalf * 2.2) * (1.0 - 0.55 * progressTop)
                        let avgWidth = (widthBottom + widthTop) / 2.0

                        let rect = CGRect(x: midX - avgWidth / 2.0, y: layerTopY, width: avgWidth, height: layerHeight * 1.15)

                        if layer.flavors.count <= 1 {
                            let color = layer.flavors.first?.color ?? .white
                            context.fill(Path(ellipseIn: rect), with: .color(color))
                        } else {
                            let numFlavors = layer.flavors.count
                            let angleStep = 360.0 / Double(numFlavors)

                            for (fIdx, flavor) in layer.flavors.enumerated() {
                                var arcPath = Path()
                                arcPath.move(to: CGPoint(x: rect.midX, y: rect.midY))
                                arcPath.addArc(
                                    center: CGPoint(x: rect.midX, y: rect.midY),
                                    radius: rect.width / 2.0,
                                    startAngle: .degrees(Double(fIdx) * angleStep + Double(index * 45)),
                                    endAngle: .degrees(Double(fIdx + 1) * angleStep + Double(index * 45)),
                                    clockwise: false
                                )
                                arcPath.closeSubpath()
                                context.fill(arcPath, with: .color(flavor.color))
                            }
                        }

                        currentBaseY = layerTopY
                    }

                    // 3. Cherry on top
                    if hasCherry {
                        let cherryCenter = CGPoint(x: midX, y: currentBaseY - 4.0)
                        let cherryRect = CGRect(x: cherryCenter.x - 5.5, y: cherryCenter.y - 5.5, width: 11, height: 11)
                        context.fill(Path(ellipseIn: cherryRect), with: .color(Color(red: 0.90, green: 0.10, blue: 0.18)))

                        var stem = Path()
                        stem.move(to: cherryCenter)
                        stem.addLine(to: CGPoint(x: cherryCenter.x + 4, y: cherryCenter.y - 8))
                        context.stroke(stem, with: .color(Color(red: 0.4, green: 0.22, blue: 0.1)), lineWidth: 1.5)
                    }
                }
            }

            Text("MATCH")
                .font(.system(size: 14, weight: .black, design: .rounded))
                .foregroundColor(.white)
                .shadow(color: .black.opacity(0.35), radius: 2, y: 1)
        }
    }
}
