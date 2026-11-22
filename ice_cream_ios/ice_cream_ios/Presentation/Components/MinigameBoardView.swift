import SwiftUI

enum IngredientType: String, CaseIterable, Identifiable {
    case chocolate = "Chocolate"
    case flower = "Flower"
    case strawberry = "Strawberry"

    var id: String { rawValue }
}

struct MinigameBoardView: View {
    let level: Int
    let coins: Int
    let onMinigameComplete: () -> Void
    let onAddCoins: (Int) -> Void
    let onSettingsTapped: () -> Void
    let onRestartTapped: () -> Void

    @State private var sortedItems: Set<IngredientType> = []
    @State private var currentProgress: CGFloat = 0.0

    @State private var strawberrySlotFrame: CGRect = .zero
    @State private var flowerSlotFrame: CGRect = .zero
    @State private var chocolateSlotFrame: CGRect = .zero

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [GameColors.bgGradientTop, GameColors.bgGradientBottom],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 16) {
                // Top HUD
                TopHudView(
                    level: level,
                    coins: coins,
                    stars: 0,
                    onSettingsTapped: onSettingsTapped,
                    onRestartTapped: onRestartTapped
                )

                // Milestone Progress Bar
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color.white)
                        .frame(width: 160, height: 26)
                        .shadow(color: .black.opacity(0.12), radius: 2, y: 1)

                    Capsule()
                        .fill(GameColors.progressGreen)
                        .frame(width: max(160 * currentProgress, 0), height: 20)
                        .padding(.leading, 3)

                    HStack {
                        Spacer()
                        Text("+5")
                            .font(.system(size: 11, weight: .bold, design: .rounded))
                            .foregroundColor(GameColors.textDark)
                        Spacer()
                        Text("+10")
                            .font(.system(size: 11, weight: .bold, design: .rounded))
                            .foregroundColor(GameColors.textDark)
                        Spacer()
                    }
                    .frame(width: 160)
                }

                Spacer().frame(height: 20)

                // 2. Wooden Cutting Board with Handle
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 22)
                        .fill(Color(red: 0.98, green: 0.86, blue: 0.73))
                        .overlay(
                            RoundedRectangle(cornerRadius: 22)
                                .stroke(Color(red: 0.89, green: 0.72, blue: 0.55), lineWidth: 3)
                        )
                        .frame(height: 180)
                        .shadow(color: .black.opacity(0.15), radius: 6, y: 4)

                    // Handle hole
                    RoundedRectangle(cornerRadius: 9)
                        .fill(GameColors.bgGradientTop)
                        .overlay(
                            RoundedRectangle(cornerRadius: 9)
                                .stroke(Color(red: 0.82, green: 0.65, blue: 0.48), lineWidth: 2)
                        )
                        .frame(width: 18, height: 44)
                        .padding(.leading, 14)

                    // Ingredients on board
                    HStack(spacing: 28) {
                        Spacer().frame(width: 24)

                        if !sortedItems.contains(.chocolate) {
                            DraggableIngredientItem(type: .chocolate, onDrop: { dropPoint in
                                checkDrop(type: .chocolate, dropPoint: dropPoint)
                            })
                        }
                        if !sortedItems.contains(.flower) {
                            DraggableIngredientItem(type: .flower, onDrop: { dropPoint in
                                checkDrop(type: .flower, dropPoint: dropPoint)
                            })
                        }
                        if !sortedItems.contains(.strawberry) {
                            DraggableIngredientItem(type: .strawberry, onDrop: { dropPoint in
                                checkDrop(type: .strawberry, dropPoint: dropPoint)
                            })
                        }
                    }
                    .frame(maxWidth: .infinity)
                }
                .padding(.horizontal, 16)

                Spacer().frame(height: 16)

                // 3. Wooden Sorting Box with Compartments
                ZStack {
                    RoundedRectangle(cornerRadius: 18)
                        .fill(Color(red: 0.77, green: 0.60, blue: 0.40))
                        .overlay(
                            RoundedRectangle(cornerRadius: 18)
                                .stroke(Color(red: 0.62, green: 0.44, blue: 0.24), lineWidth: 4)
                        )
                        .frame(height: 220)
                        .shadow(color: .black.opacity(0.18), radius: 8, y: 5)

                    VStack(spacing: 8) {
                        HStack(spacing: 8) {
                            // Strawberry slot
                            GeometryReader { geo in
                                ZStack {
                                    RoundedRectangle(cornerRadius: 12)
                                        .fill(Color(red: 0.91, green: 0.78, blue: 0.64))
                                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(red: 0.71, green: 0.54, blue: 0.36), lineWidth: 2))

                                    if sortedItems.contains(.strawberry) {
                                        IngredientView(type: .strawberry)
                                    }
                                }
                                .preference(key: SlotPreferenceKey.self, value: [ "strawberry": geo.frame(in: .global) ])
                            }

                            // Flower slot
                            GeometryReader { geo in
                                ZStack {
                                    RoundedRectangle(cornerRadius: 12)
                                        .fill(Color(red: 0.91, green: 0.78, blue: 0.64))
                                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(red: 0.71, green: 0.54, blue: 0.36), lineWidth: 2))

                                    if sortedItems.contains(.flower) {
                                        IngredientView(type: .flower)
                                    }
                                }
                                .preference(key: SlotPreferenceKey.self, value: [ "flower": geo.frame(in: .global) ])
                            }
                        }

                        // Chocolate slot
                        GeometryReader { geo in
                            ZStack {
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(Color(red: 0.91, green: 0.78, blue: 0.64))
                                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(red: 0.71, green: 0.54, blue: 0.36), lineWidth: 2))

                                if sortedItems.contains(.chocolate) {
                                    IngredientView(type: .chocolate)
                                }
                            }
                            .preference(key: SlotPreferenceKey.self, value: [ "chocolate": geo.frame(in: .global) ])
                        }
                    }
                    .padding(8)
                }
                .padding(.horizontal, 22)
                .onPreferenceChange(SlotPreferenceKey.self) { prefs in
                    if let f = prefs["strawberry"] { strawberrySlotFrame = f }
                    if let f = prefs["flower"] { flowerSlotFrame = f }
                    if let f = prefs["chocolate"] { chocolateSlotFrame = f }
                }

                Spacer()
            }
        }
    }

    private func checkDrop(type: IngredientType, dropPoint: CGPoint) -> Bool {
        let targetFrame: CGRect
        switch type {
        case .strawberry: targetFrame = strawberrySlotFrame
        case .flower: targetFrame = flowerSlotFrame
        case .chocolate: targetFrame = chocolateSlotFrame
        }

        if targetFrame.contains(dropPoint) {
            sortedItems.insert(type)
            withAnimation(.spring()) {
                currentProgress = CGFloat(sortedItems.count) / 3.0
            }
            if sortedItems.count == 2 { onAddCoins(5) }
            if sortedItems.count == 3 {
                onAddCoins(10)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
                    onMinigameComplete()
                }
            }
            return true
        }
        return false
    }
}

struct SlotPreferenceKey: PreferenceKey {
    static var defaultValue: [String: CGRect] = [:]
    static func reduce(value: inout [String: CGRect], nextValue: () -> [String: CGRect]) {
        value.merge(nextValue()) { $1 }
    }
}

struct DraggableIngredientItem: View {
    let type: IngredientType
    let onDrop: (CGPoint) -> Bool

    @State private var dragOffset: CGSize = .zero

    var body: some View {
        GeometryReader { geo in
            IngredientView(type: type)
                .offset(dragOffset)
                .gesture(
                    DragGesture(coordinateSpace: .global)
                        .onChanged { value in
                            dragOffset = value.translation
                        }
                        .onEnded { value in
                            let dropPoint = value.location
                            let success = onDrop(dropPoint)
                            if !success {
                                withAnimation(.spring()) {
                                    dragOffset = .zero
                                }
                            }
                        }
                )
        }
        .frame(width: 72, height: 60)
    }
}

struct IngredientView: View {
    let type: IngredientType

    var body: some View {
        ZStack {
            switch type {
            case .chocolate:
                RoundedRectangle(cornerRadius: 6)
                    .fill(Color(red: 0.42, green: 0.23, blue: 0.10))
                    .frame(width: 70, height: 34)
                    .overlay(
                        HStack {
                            Circle()
                                .fill(Color(red: 1.0, green: 0.83, blue: 0.14))
                                .frame(width: 20, height: 20)
                                .overlay(Text("😊").font(.system(size: 11)))
                                .padding(.leading, 4)
                            Spacer()
                        }
                    )
            case .flower:
                ZStack {
                    ForEach(0..<5) { i in
                        Circle()
                            .fill(Color(red: 0.98, green: 0.97, blue: 0.95))
                            .frame(width: 22, height: 22)
                            .offset(x: 14 * cos(Double(i) * 72 * Double.pi / 180),
                                    y: 14 * sin(Double(i) * 72 * Double.pi / 180))
                    }
                    Circle()
                        .fill(Color(red: 1.0, green: 0.83, blue: 0.14))
                        .frame(width: 14, height: 14)
                }
            case .strawberry:
                ZStack {
                    Circle()
                        .fill(Color(red: 0.91, green: 0.16, blue: 0.27))
                        .frame(width: 36, height: 42)
                    HStack(spacing: 3) {
                        Circle().fill(Color(red: 0.30, green: 0.69, blue: 0.16)).frame(width: 10, height: 10)
                        Circle().fill(Color(red: 0.30, green: 0.69, blue: 0.16)).frame(width: 12, height: 12)
                        Circle().fill(Color(red: 0.30, green: 0.69, blue: 0.16)).frame(width: 10, height: 10)
                    }
                    .offset(y: -18)
                }
            }
        }
    }
}
