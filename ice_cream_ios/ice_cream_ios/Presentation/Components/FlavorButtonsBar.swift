import SwiftUI

struct FlavorButtonsBar: View {
    let flavors: [Flavor]
    let activeFlavors: Set<Flavor>
    let showTutorial: Bool
    let onFlavorPressed: (Flavor) -> Void
    let onFlavorReleased: (Flavor) -> Void

    init(
        flavors: [Flavor],
        activeFlavors: Set<Flavor>,
        showTutorial: Bool = false,
        onFlavorPressed: @escaping (Flavor) -> Void,
        onFlavorReleased: @escaping (Flavor) -> Void
    ) {
        self.flavors = flavors
        self.activeFlavors = activeFlavors
        self.showTutorial = showTutorial
        self.onFlavorPressed = onFlavorPressed
        self.onFlavorReleased = onFlavorReleased
    }

    var body: some View {
        HStack(spacing: 32) {
            ForEach(Array(flavors.enumerated()), id: \.element.id) { index, flavor in
                ZStack(alignment: .bottom) {
                    FlavorButtonItem(
                        flavor: flavor,
                        isPressed: activeFlavors.contains(flavor),
                        onPress: { onFlavorPressed(flavor) },
                        onRelease: { onFlavorReleased(flavor) }
                    )

                    if showTutorial && index == 0 && activeFlavors.isEmpty {
                        TutorialHandView()
                            .offset(y: 46)
                    }
                }
            }
        }
        .padding(.horizontal, 24)
        .padding(.bottom, 28)
    }
}

struct FlavorButtonItem: View {
    let flavor: Flavor
    let isPressed: Bool
    let onPress: () -> Void
    let onRelease: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            ZStack {
                Circle()
                    .fill(Color.white)
                    .frame(width: 64, height: 64)
                    .shadow(color: .black.opacity(0.18), radius: 4, y: 3)

                Circle()
                    .fill(flavor.color)
                    .frame(width: 54, height: 54)
                    .scaleEffect(isPressed ? 0.92 : 1.0)
                    .animation(.easeInOut(duration: 0.1), value: isPressed)
            }
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in
                        if !isPressed {
                            onPress()
                        }
                    }
                    .onEnded { _ in
                        onRelease()
                    }
            )

            Text(flavor.displayName)
                .font(.system(size: 13, weight: .bold, design: .rounded))
                .foregroundColor(.white)
                .shadow(color: .black.opacity(0.3), radius: 2, y: 1)
        }
    }
}
