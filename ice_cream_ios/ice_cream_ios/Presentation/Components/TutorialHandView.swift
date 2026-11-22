import SwiftUI

struct TutorialHandView: View {
    @State private var bounce = false

    var body: some View {
        ZStack {
            Canvas { context, size in
                let yellow = Color(red: 1.0, green: 0.80, blue: 0.0)
                let strokeColor = Color(red: 0.2, green: 0.2, blue: 0.2)

                // Index finger pointing up
                var fingerPath = Path()
                fingerPath.move(to: CGPoint(x: 20, y: 32))
                fingerPath.addLine(to: CGPoint(x: 20, y: 6))
                fingerPath.addArc(
                    center: CGPoint(x: 17, y: 6),
                    radius: 3,
                    startAngle: .degrees(0),
                    endAngle: .degrees(180),
                    clockwise: true
                )
                fingerPath.addLine(to: CGPoint(x: 14, y: 32))
                fingerPath.closeSubpath()

                context.fill(fingerPath, with: .color(yellow))
                context.stroke(fingerPath, with: .color(strokeColor), lineWidth: 2.0)

                // Palm
                var palmPath = Path()
                palmPath.move(to: CGPoint(x: 14, y: 28))
                palmPath.addLine(to: CGPoint(x: 38, y: 28))
                palmPath.addQuadCurve(to: CGPoint(x: 36, y: 50), control: CGPoint(x: 42, y: 40))
                palmPath.addLine(to: CGPoint(x: 16, y: 50))
                palmPath.addQuadCurve(to: CGPoint(x: 12, y: 36), control: CGPoint(x: 10, y: 46))
                palmPath.closeSubpath()

                context.fill(palmPath, with: .color(yellow))
                context.stroke(palmPath, with: .color(strokeColor), lineWidth: 2.0)

                // Knuckle lines
                var line1 = Path()
                line1.move(to: CGPoint(x: 25, y: 34))
                line1.addLine(to: CGPoint(x: 35, y: 34))
                context.stroke(line1, with: .color(strokeColor), lineWidth: 1.5)

                var line2 = Path()
                line2.move(to: CGPoint(x: 25, y: 41))
                line2.addLine(to: CGPoint(x: 34, y: 41))
                context.stroke(line2, with: .color(strokeColor), lineWidth: 1.5)
            }
            .frame(width: 48, height: 56)
            .offset(y: bounce ? -12 : 0)
            .animation(
                Animation.easeInOut(duration: 0.65).repeatForever(autoreverses: true),
                value: bounce
            )
            .onAppear {
                bounce = true
            }
        }
    }
}
