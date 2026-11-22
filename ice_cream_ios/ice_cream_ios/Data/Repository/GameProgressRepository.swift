import Foundation

protocol GameProgressRepository {
    func loadLevel() -> Int
    func saveLevel(_ level: Int)
    func loadCoins() -> Int
    func saveCoins(_ coins: Int)
    func loadSoundEnabled() -> Bool
    func saveSoundEnabled(_ enabled: Bool)
    func loadHapticsEnabled() -> Bool
    func saveHapticsEnabled(_ enabled: Bool)
    func resetProgress()
}

final class UserDefaultsProgressRepository: GameProgressRepository {
    private let userDefaults = UserDefaults.standard
    private let levelKey = "ice_cream_saved_level"
    private let coinsKey = "ice_cream_saved_coins"
    private let soundKey = "ice_cream_sound_enabled"
    private let hapticsKey = "ice_cream_haptics_enabled"

    func loadLevel() -> Int {
        let level = userDefaults.integer(forKey: levelKey)
        return level > 0 ? level : 1
    }

    func saveLevel(_ level: Int) {
        userDefaults.set(level, forKey: levelKey)
    }

    func loadCoins() -> Int {
        if userDefaults.object(forKey: coinsKey) == nil {
            return 250
        }
        return userDefaults.integer(forKey: coinsKey)
    }

    func saveCoins(_ coins: Int) {
        userDefaults.set(coins, forKey: coinsKey)
    }

    func loadSoundEnabled() -> Bool {
        if userDefaults.object(forKey: soundKey) == nil {
            return true
        }
        return userDefaults.bool(forKey: soundKey)
    }

    func saveSoundEnabled(_ enabled: Bool) {
        userDefaults.set(enabled, forKey: soundKey)
    }

    func loadHapticsEnabled() -> Bool {
        if userDefaults.object(forKey: hapticsKey) == nil {
            return true
        }
        return userDefaults.bool(forKey: hapticsKey)
    }

    func saveHapticsEnabled(_ enabled: Bool) {
        userDefaults.set(enabled, forKey: hapticsKey)
    }

    func resetProgress() {
        saveLevel(1)
        saveCoins(250)
    }
}
