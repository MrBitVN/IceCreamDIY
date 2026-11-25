import Foundation

struct TargetLayer: Identifiable, Codable {
    var id: String { "\(flavors.map { $0.rawValue }.joined(separator: "_"))_\(proportion)" }
    let flavors: [Flavor]
    let proportion: Float

    enum CodingKeys: String, CodingKey {
        case flavors
        case proportion
    }

    init(flavors: [Flavor], proportion: Float) {
        self.flavors = flavors
        self.proportion = proportion
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let flavorStrings = try container.decode([String].self, forKey: .flavors)
        self.flavors = flavorStrings.compactMap { Flavor(rawValue: $0) }
        self.proportion = try container.decode(Float.self, forKey: .proportion)
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(flavors.map { $0.rawValue }, forKey: .flavors)
        try container.encode(proportion, forKey: .proportion)
    }
}

struct CustomerConfig: Codable {
    let id: String
    let name: String
    let gender: String

    init(id: String = "customer_1", name: String = "Guest", gender: String = "female") {
        self.id = id
        self.name = name
        self.gender = gender
    }
}

struct MinigamesConfig: Codable, Equatable {
    let slicing: Bool
    let blender: Bool

    init(slicing: Bool = false, blender: Bool = false) {
        self.slicing = slicing
        self.blender = blender
    }
}

struct LevelConfig: Identifiable, Codable {
    var id: Int { levelNumber }
    let levelNumber: Int
    let hasMinigame: Bool
    let minigames: MinigamesConfig
    let availableFlavors: [Flavor]
    let targetLayers: [TargetLayer]
    let sprinklesCount: Int
    let hasCherry: Bool
    let extraCones: Int
    let customer: CustomerConfig

    enum CodingKeys: String, CodingKey {
        case levelNumber
        case hasMinigame
        case minigames
        case availableFlavors
        case targetLayers
        case sprinklesCount
        case hasCherry
        case extraCones
        case customer
    }

    init(
        levelNumber: Int,
        hasMinigame: Bool = false,
        minigames: MinigamesConfig? = nil,
        availableFlavors: [Flavor] = [.vanilla, .strawberry, .chocolate],
        targetLayers: [TargetLayer] = [TargetLayer(flavors: [.vanilla], proportion: 1.0)],
        sprinklesCount: Int = 0,
        hasCherry: Bool = false,
        extraCones: Int = 1,
        customer: CustomerConfig = CustomerConfig()
    ) {
        let resolvedMini = minigames ?? MinigamesConfig(slicing: hasMinigame, blender: hasMinigame)
        self.levelNumber = levelNumber
        self.hasMinigame = resolvedMini.slicing || resolvedMini.blender
        self.minigames = resolvedMini
        self.availableFlavors = availableFlavors
        self.targetLayers = targetLayers
        self.sprinklesCount = sprinklesCount
        self.hasCherry = hasCherry
        self.extraCones = extraCones
        self.customer = customer
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.levelNumber = try container.decode(Int.self, forKey: .levelNumber)
        let legacyMini = try container.decodeIfPresent(Bool.self, forKey: .hasMinigame) ?? false
        let decodedMinigames = try container.decodeIfPresent(MinigamesConfig.self, forKey: .minigames)
            ?? MinigamesConfig(slicing: legacyMini, blender: legacyMini)

        self.minigames = decodedMinigames
        self.hasMinigame = decodedMinigames.slicing || decodedMinigames.blender

        let flavorStrings = try container.decode([String].self, forKey: .availableFlavors)
        self.availableFlavors = flavorStrings.compactMap { Flavor(rawValue: $0) }

        self.targetLayers = try container.decode([TargetLayer].self, forKey: .targetLayers)
        self.sprinklesCount = try container.decode(Int.self, forKey: .sprinklesCount)
        self.hasCherry = try container.decode(Bool.self, forKey: .hasCherry)
        self.extraCones = try container.decode(Int.self, forKey: .extraCones)
        self.customer = try container.decode(CustomerConfig.self, forKey: .customer)
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(levelNumber, forKey: .levelNumber)
        try container.encode(hasMinigame, forKey: .hasMinigame)
        try container.encode(minigames, forKey: .minigames)
        try container.encode(availableFlavors.map { $0.rawValue }, forKey: .availableFlavors)
        try container.encode(targetLayers, forKey: .targetLayers)
        try container.encode(sprinklesCount, forKey: .sprinklesCount)
        try container.encode(hasCherry, forKey: .hasCherry)
        try container.encode(extraCones, forKey: .extraCones)
        try container.encode(customer, forKey: .customer)
    }

    func initialPhase() -> GamePhase {
        if minigames.slicing {
            return .prepareMinigame
        } else if minigames.blender {
            return .blenderMinigame
        } else {
            return .pouring
        }
    }

    func phaseAfterSlicing() -> GamePhase {
        return minigames.blender ? .blenderMinigame : .pouring
    }
}
