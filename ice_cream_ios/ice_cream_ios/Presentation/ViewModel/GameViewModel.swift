import Foundation
import Combine

struct GameUiState {
    var screen: AppScreen = .splash
    var phase: GamePhase = .pouring
    var level: Int = 1
    var coins: Int = 250
    var stars: Int = 0
    var matchScore: Int = 0
    var targetConfig: LevelConfig = LevelDataProvider.getLevel(1)
    var activeFlavors: Set<Flavor> = []
    var pourProgress: Float = 0.0
    var isRotating: Bool = true
    var showTutorial: Bool = true
    var customerVariant: Int = 0
    var earnedCoins: Int = 0
    var isSoundEnabled: Bool = true
    var isHapticsEnabled: Bool = true
    var showSettingsDialog: Bool = false
    var showResultsOverlay: Bool = false
    var matchResult: MatchResult? = nil
}

@MainActor
final class GameViewModel: ObservableObject {
    @Published var uiState = GameUiState()
    private let repository: GameProgressRepository
    private let soundHapticManager = SoundHapticManager()

    private var pouringTimer: Timer?
    let maxPourProgress: Float = 1.0
    private let pourSpeed: Float = 0.28

    private var recordedSegments: [UserPouredSegment] = []

    init(repository: GameProgressRepository = UserDefaultsProgressRepository()) {
        self.repository = repository
        loadInitialData()
    }

    private func loadInitialData() {
        let level = repository.loadLevel()
        let coins = repository.loadCoins()
        let sound = repository.loadSoundEnabled()
        let haptics = repository.loadHapticsEnabled()
        let config = LevelDataProvider.getLevel(level)

        soundHapticManager.isSoundEnabled = sound
        soundHapticManager.isHapticsEnabled = haptics

        uiState.level = level
        uiState.coins = coins
        uiState.isSoundEnabled = sound
        uiState.isHapticsEnabled = haptics
        uiState.targetConfig = config
        uiState.showTutorial = (level == 1)
        uiState.customerVariant = (level - 1) % 5
    }

    func navigateToHome() {
        uiState.screen = .home
    }

    func startGame() {
        stopPouring()
        recordedSegments.removeAll()
        let currentLvl = uiState.level
        let config = LevelDataProvider.getLevel(currentLvl)
        uiState.screen = .game
        uiState.phase = config.hasMinigame ? .prepareMinigame : .pouring
        uiState.targetConfig = config
        uiState.activeFlavors.removeAll()
        uiState.pourProgress = 0.0
        uiState.stars = 0
        uiState.matchScore = 0
        uiState.matchResult = nil
        uiState.showTutorial = (currentLvl == 1)
        uiState.customerVariant = (currentLvl - 1) % 5
        uiState.showResultsOverlay = false
        uiState.earnedCoins = 0
    }

    func restartLevel() {
        startGame()
    }

    func nextLevel() {
        stopPouring()
        let nextLvl = uiState.level + 1
        repository.saveLevel(nextLvl)
        let config = LevelDataProvider.getLevel(nextLvl)
        uiState.level = nextLvl
        uiState.targetConfig = config
        uiState.showTutorial = false
        uiState.customerVariant = (nextLvl - 1) % 5
        uiState.showResultsOverlay = false
        startGame()
    }

    func onFlavorPressed(_ flavor: Flavor) {
        guard uiState.pourProgress < maxPourProgress else { return }
        guard uiState.phase == .pouring else { return }
        uiState.activeFlavors.insert(flavor)
        uiState.showTutorial = false
        startPouring()
    }

    func onFlavorReleased(_ flavor: Flavor) {
        uiState.activeFlavors.remove(flavor)
        if uiState.activeFlavors.isEmpty {
            stopPouring()
        }
    }

    private func startPouring() {
        guard pouringTimer == nil else { return }

        var lastTime = Date().timeIntervalSince1970
        pouringTimer = Timer.scheduledTimer(withTimeInterval: 0.016, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            let now = Date().timeIntervalSince1970
            let dt = Float(min(max(now - lastTime, 0.001), 0.05))
            lastTime = now

            self.soundHapticManager.startPouringFeedback()

            let currentProgress = self.uiState.pourProgress
            let addedFraction = self.pourSpeed * dt
            let newProgress = min(currentProgress + addedFraction, self.maxPourProgress)

            self.recordedSegments.append(
                UserPouredSegment(
                    flavors: self.uiState.activeFlavors,
                    heightStart: currentProgress,
                    heightEnd: newProgress,
                    durationSec: dt
                )
            )

            self.uiState.pourProgress = newProgress

            if newProgress >= self.maxPourProgress {
                self.stopPouring()
                self.uiState.activeFlavors.removeAll()
                self.evaluateScore(finalProgress: newProgress)
            }
        }
    }

    private func stopPouring() {
        pouringTimer?.invalidate()
        pouringTimer = nil
        soundHapticManager.stopPouringFeedback()
    }

    func evaluateScore(finalProgress: Float) {
        let result = IceCreamMatcher.evaluate(
            userSegments: recordedSegments,
            targetLayers: uiState.targetConfig.targetLayers,
            finalPourProgress: finalProgress
        )

        let reward = result.stars == 3 ? 100 : (result.stars == 2 ? 60 : 30)

        uiState.matchScore = result.totalScore
        uiState.stars = result.stars
        uiState.matchResult = result
        uiState.phase = .customerReaction
        uiState.earnedCoins = reward

        // Reveal results popup after customer animation
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) { [weak self] in
            guard let self = self else { return }
            self.soundHapticManager.playSuccessFeedback()
            self.addCoins(reward)
            self.uiState.showResultsOverlay = true
        }
    }

    func completeMinigame() {
        uiState.phase = .pouring
    }

    func placeCherry() {
        let currentCfg = uiState.targetConfig
        let updatedCfg = LevelConfig(
            levelNumber: currentCfg.levelNumber,
            hasMinigame: currentCfg.hasMinigame,
            availableFlavors: currentCfg.availableFlavors,
            targetLayers: currentCfg.targetLayers,
            sprinklesCount: currentCfg.sprinklesCount,
            hasCherry: true,
            extraCones: currentCfg.extraCones,
            customer: currentCfg.customer
        )
        uiState.targetConfig = updatedCfg
    }

    func addSprinkles(count: Int = 15) {
        let currentCfg = uiState.targetConfig
        let updatedCfg = LevelConfig(
            levelNumber: currentCfg.levelNumber,
            hasMinigame: currentCfg.hasMinigame,
            availableFlavors: currentCfg.availableFlavors,
            targetLayers: currentCfg.targetLayers,
            sprinklesCount: currentCfg.sprinklesCount + count,
            hasCherry: currentCfg.hasCherry,
            extraCones: currentCfg.extraCones,
            customer: currentCfg.customer
        )
        uiState.targetConfig = updatedCfg
    }

    func addCoins(_ amount: Int) {
        let updated = uiState.coins + amount
        uiState.coins = updated
        repository.saveCoins(updated)
    }

    func openSettings() {
        uiState.showSettingsDialog = true
    }

    func closeSettings() {
        uiState.showSettingsDialog = false
    }

    func toggleSound() {
        let newVal = !uiState.isSoundEnabled
        uiState.isSoundEnabled = newVal
        soundHapticManager.isSoundEnabled = newVal
        repository.saveSoundEnabled(newVal)
    }

    func toggleHaptics() {
        let newVal = !uiState.isHapticsEnabled
        uiState.isHapticsEnabled = newVal
        soundHapticManager.isHapticsEnabled = newVal
        repository.saveHapticsEnabled(newVal)
    }

    func resetProgress() {
        repository.resetProgress()
        uiState.level = 1
        uiState.coins = 250
        uiState.showSettingsDialog = false
        startGame()
    }

    deinit {
        pouringTimer?.invalidate()
        soundHapticManager.release()
    }
}
