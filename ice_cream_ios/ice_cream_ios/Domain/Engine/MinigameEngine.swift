import Foundation

enum MinigameIngredient: Int, CaseIterable, Identifiable {
    case blueberry = 0
    case mint = 1
    case mango = 2

    var id: Int { rawValue }

    var label: String {
        switch self {
        case .blueberry: return "BERRY"
        case .mint: return "MINT"
        case .mango: return "MANGO"
        }
    }

    var rewardEmoji: String {
        switch self {
        case .blueberry: return "😎"
        case .mint: return "😍"
        case .mango: return "😊"
        }
    }

    var boardX: Float {
        switch self {
        case .blueberry: return -0.58
        case .mint: return 0.12
        case .mango: return 0.82
        }
    }

    var boardZ: Float { -0.78 }

    var targetMinX: Float {
        switch self {
        case .mint: return -1.24
        case .mango: return -0.40
        case .blueberry: return 0.41
        }
    }

    var targetMaxX: Float {
        switch self {
        case .mint: return -0.41
        case .mango: return 0.40
        case .blueberry: return 1.24
        }
    }

    var targetMinZ: Float { 0.16 }
    var targetMaxZ: Float { 1.48 }

    var targetCenterX: Float {
        switch self {
        case .mint: return -0.82
        case .mango: return 0.00
        case .blueberry: return 0.82
        }
    }

    var targetCenterZ: Float { 0.82 }
}

enum MinigameEngine {

    static func hitTestCompartment(worldX: Float, worldZ: Float) -> MinigameIngredient? {
        for item in MinigameIngredient.allCases {
            if worldX >= item.targetMinX && worldX <= item.targetMaxX &&
                worldZ >= item.targetMinZ && worldZ <= item.targetMaxZ {
                return item
            }
        }
        return nil
    }

    static func hitTestIngredient(
        worldX: Float,
        worldZ: Float,
        posX: Float,
        posZ: Float,
        hitRadius: Float = 0.38
    ) -> Bool {
        let dx = worldX - posX
        let dz = worldZ - posZ
        return sqrt(dx * dx + dz * dz) <= hitRadius
    }

    static func elasticBounceScale(elapsedSec: Float) -> Float {
        if elapsedSec <= 0 { return 1.38 }
        if elapsedSec >= 0.85 { return 1.0 }
        return 1.0 + 0.38 * exp(-6.2 * elapsedSec) * cos(18.5 * elapsedSec)
    }

    static func milestoneCoinsForCount(_ sortedCount: Int) -> Int {
        switch sortedCount {
        case 2: return 5
        case 3: return 10
        default: return 0
        }
    }
}
