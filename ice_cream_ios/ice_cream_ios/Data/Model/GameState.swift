import Foundation

enum AppScreen {
    case splash
    case home
    case game
}

enum GamePhase {
    case prepareMinigame
    case blenderMinigame
    case pouring
    case decorating
    case evaluating
    case customerReaction
}

struct PouredLayer: Identifiable {
    let id: UUID
    let flavors: [Flavor]
    let height: Float
    let yPosition: Float

    init(id: UUID = UUID(), flavors: [Flavor], height: Float, yPosition: Float) {
        self.id = id
        self.flavors = flavors
        self.height = height
        self.yPosition = yPosition
    }
}

struct GameState {
    var screen: AppScreen = .splash
    var phase: GamePhase = .pouring
    var level: Int = 1
    var coins: Int = 250
    var stars: Int = 0
    var activeFlavors: Set<Flavor> = []
    var pourProgress: Float = 0.0
    var coneRotationAngle: Float = 0.0
    var pouredLayers: [PouredLayer] = []
}
