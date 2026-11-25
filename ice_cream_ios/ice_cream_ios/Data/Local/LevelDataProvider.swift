import Foundation

struct LevelDataProvider {
    private static var cachedLevels: [LevelConfig]?

    static func loadLevels() -> [LevelConfig] {
        if let cached = cachedLevels { return cached }

        guard let url = Bundle.main.url(forResource: "levels", withExtension: "json") else {
            return fallbackLevels
        }

        do {
            let data = try Data(contentsOf: url)
            let decoded = try JSONDecoder().decode([LevelConfig].self, from: data)
            cachedLevels = decoded
            return decoded
        } catch {
            return fallbackLevels
        }
    }

    static func getLevel(_ number: Int) -> LevelConfig {
        let allLevels = loadLevels()
        if let config = allLevels.first(where: { $0.levelNumber == number }) {
            return config
        }

        let allFlavors = Flavor.allCases
        let f1 = allFlavors[(number - 1) % allFlavors.count]
        let f2 = allFlavors[number % allFlavors.count]
        return LevelConfig(
            levelNumber: number,
            hasMinigame: number % 2 == 0,
            availableFlavors: [.vanilla, .strawberry, .chocolate],
            targetLayers: [TargetLayer(flavors: [f1], proportion: 0.5), TargetLayer(flavors: [f2], proportion: 0.5)],
            sprinklesCount: 15,
            hasCherry: true,
            extraCones: 1
        )
    }

    private static let fallbackLevels: [LevelConfig] = [
        LevelConfig(
            levelNumber: 1,
            hasMinigame: false,
            minigames: MinigamesConfig(slicing: false, blender: false),
            availableFlavors: [.vanilla],
            targetLayers: [TargetLayer(flavors: [.vanilla], proportion: 1.0)],
            sprinklesCount: 0,
            hasCherry: false,
            extraCones: 1
        ),
        LevelConfig(
            levelNumber: 2,
            hasMinigame: true,
            minigames: MinigamesConfig(slicing: true, blender: true),
            availableFlavors: [.vanilla, .strawberry, .chocolate],
            targetLayers: [
                TargetLayer(flavors: [.vanilla], proportion: 0.5),
                TargetLayer(flavors: [.strawberry], proportion: 0.5)
            ],
            sprinklesCount: 0,
            hasCherry: false,
            extraCones: 1
        )
    ]
}
