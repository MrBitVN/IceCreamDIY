package com.icecream.diy.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.icecream.diy.data.local.LevelDataProvider
import com.icecream.diy.data.model.AppScreen
import com.icecream.diy.data.model.Flavor
import com.icecream.diy.data.model.GamePhase
import com.icecream.diy.data.repository.GameProgressRepository
import com.icecream.diy.domain.engine.SoundHapticManager
import com.icecream.diy.domain.scoring.IceCreamMatcher
import com.icecream.diy.domain.scoring.UserPouredSegment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel(
    private val context: Context,
    private val repository: GameProgressRepository,
    private val soundHapticManager: SoundHapticManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var pouringJob: Job? = null
    val maxPourProgress = 1.0f
    private val pourSpeed = 0.28f

    // Poured segments tracking
    private val recordedSegments = mutableListOf<UserPouredSegment>()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            repository.getLevel().collect { savedLevel ->
                val config = LevelDataProvider.getLevel(context, savedLevel)
                _uiState.update { current ->
                    current.copy(
                        level = savedLevel,
                        targetConfig = config,
                        showTutorial = savedLevel == 1,
                        customerVariant = (savedLevel - 1) % 5
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.getCoins().collect { savedCoins ->
                _uiState.update { current ->
                    current.copy(coins = savedCoins)
                }
            }
        }
        viewModelScope.launch {
            repository.getSoundEnabled().collect { enabled ->
                soundHapticManager?.isSoundEnabled = enabled
                _uiState.update { it.copy(isSoundEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            repository.getHapticsEnabled().collect { enabled ->
                soundHapticManager?.isHapticsEnabled = enabled
                _uiState.update { it.copy(isHapticsEnabled = enabled) }
            }
        }
    }

    fun navigateToHome() {
        _uiState.update { it.copy(screen = AppScreen.HOME) }
    }

    fun startGame() {
        stopPouring()
        recordedSegments.clear()
        val currentLvl = _uiState.value.level
        val config = LevelDataProvider.getLevel(context, currentLvl)
        _uiState.update {
            it.copy(
                screen = AppScreen.GAME,
                phase = config.initialPhase(),
                targetConfig = config,
                activeFlavors = emptySet(),
                pourProgress = 0.0f,
                stars = 0,
                matchScore = 0,
                matchResult = null,
                userPouredSegments = emptyList(),
                showTutorial = currentLvl == 1,
                customerVariant = (currentLvl - 1) % 5,
                showResultsOverlay = false,
                isDecorated = false,
                showMysteryCone = false,
                isRewardDoubled = false,
                earnedCoins = 0,
                sessionNonce = it.sessionNonce + 1
            )
        }
    }

    fun restartLevel() {
        startGame()
    }

    fun onRequestNextLevel() {
        val currentLvl = _uiState.value.level
        if (currentLvl % 2 == 0 && _uiState.value.stars >= 1) {
            _uiState.update {
                it.copy(
                    showResultsOverlay = false,
                    showMysteryCone = true
                )
            }
        } else {
            nextLevel()
        }
    }

    fun onDismissMysteryCone() {
        _uiState.update { it.copy(showMysteryCone = false) }
        nextLevel()
    }

    fun nextLevel() {
        stopPouring()
        val rawNext = _uiState.value.level + 1
        val nextLvl = if (rawNext > 20) 1 else rawNext
        viewModelScope.launch {
            repository.saveLevel(nextLvl)
        }
        val config = LevelDataProvider.getLevel(context, nextLvl)
        _uiState.update {
            it.copy(
                level = nextLvl,
                targetConfig = config,
                showTutorial = false,
                customerVariant = (nextLvl - 1) % 5,
                showResultsOverlay = false,
                showMysteryCone = false
            )
        }
        startGame()
    }

    fun onFlavorPressed(flavor: Flavor) {
        if (_uiState.value.pourProgress >= maxPourProgress) return
        if (_uiState.value.phase != GamePhase.POURING) return

        _uiState.update {
            it.copy(
                activeFlavors = it.activeFlavors + flavor,
                showTutorial = false
            )
        }
        startPouring()
    }

    fun onFlavorReleased(flavor: Flavor) {
        _uiState.update {
            it.copy(activeFlavors = it.activeFlavors - flavor)
        }
        if (_uiState.value.activeFlavors.isEmpty()) {
            stopPouring()
        }
    }

    private fun startPouring() {
        if (pouringJob?.isActive == true) return

        pouringJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (isActive && _uiState.value.activeFlavors.isNotEmpty()) {
                val now = System.currentTimeMillis()
                val dt = ((now - lastTime) / 1000f).coerceIn(0.001f, 0.05f)
                lastTime = now

                soundHapticManager?.startPouringFeedback()

                val currentProgress = _uiState.value.pourProgress
                val addedFraction = pourSpeed * dt
                val newProgress = (currentProgress + addedFraction).coerceAtMost(maxPourProgress)

                // Record segment for scoring evaluation (flavorSet, heightStart, heightEnd)
                recordedSegments.add(
                    UserPouredSegment(
                        flavors = _uiState.value.activeFlavors,
                        heightStart = currentProgress,
                        heightEnd = newProgress,
                        durationSec = dt
                    )
                )

                _uiState.update {
                    it.copy(
                        pourProgress = newProgress,
                        userPouredSegments = recordedSegments.toList()
                    )
                }

                if (newProgress >= maxPourProgress) {
                    stopPouring()
                    val cfg = _uiState.value.targetConfig
                    if (cfg.sprinklesCount > 0 || cfg.hasCherry) {
                        _uiState.update {
                            it.copy(
                                activeFlavors = emptySet(),
                                phase = GamePhase.DECORATING
                            )
                        }
                    } else {
                        _uiState.update { it.copy(activeFlavors = emptySet()) }
                        evaluateScore(newProgress)
                    }
                    break
                }

                delay(16) // ~60 FPS
            }
        }
    }

    private fun stopPouring() {
        pouringJob?.cancel()
        pouringJob = null
        soundHapticManager?.stopPouringFeedback()
    }

    fun onAppBackgrounded() {
        stopPouring()
        if (_uiState.value.activeFlavors.isNotEmpty()) {
            _uiState.update { it.copy(activeFlavors = emptySet()) }
        }
    }

    fun evaluateScore(finalProgress: Float = _uiState.value.pourProgress) {
        val result = IceCreamMatcher.evaluate(
            userSegments = recordedSegments,
            targetLayers = _uiState.value.targetConfig.targetLayers,
            finalPourProgress = finalProgress
        )

        val reward = when (result.stars) {
            3 -> 100
            2 -> 60
            else -> 30
        }

        _uiState.update {
            it.copy(
                matchScore = result.totalScore,
                stars = result.stars,
                matchResult = result,
                phase = GamePhase.CUSTOMER_REACTION,
                earnedCoins = reward
            )
        }

        // Trigger customer reaction sequence, then reveal results popup
        viewModelScope.launch {
            delay(1200)
            soundHapticManager?.playSuccessFeedback()
            addCoins(reward)
            _uiState.update { it.copy(showResultsOverlay = true) }
        }
    }

    fun onDecorateClicked() {
        if (_uiState.value.phase != GamePhase.DECORATING || _uiState.value.isDecorated) return
        val bonus = _uiState.value.decorationBonusCoins
        soundHapticManager?.playSuccessFeedback()
        addCoins(bonus)
        _uiState.update { it.copy(isDecorated = true) }
        viewModelScope.launch {
            delay(1100)
            evaluateScore(_uiState.value.pourProgress)
        }
    }

    fun onDoubleRewardClicked() {
        if (_uiState.value.isRewardDoubled) return
        val currentEarned = _uiState.value.earnedCoins
        soundHapticManager?.playSuccessFeedback()
        addCoins(currentEarned)
        _uiState.update {
            it.copy(
                earnedCoins = currentEarned * 2,
                isRewardDoubled = true
            )
        }
    }

    fun completeMinigame() {
        val nextPhase = _uiState.value.targetConfig.phaseAfterSlicing()
        _uiState.update { it.copy(phase = nextPhase) }
    }

    fun completeBlender() {
        soundHapticManager?.stopPouringFeedback()
        _uiState.update { it.copy(phase = GamePhase.POURING) }
    }

    fun triggerMinigamePulse() {
        soundHapticManager?.triggerHapticPulse()
    }

    fun triggerBlenderTick() {
        soundHapticManager?.startPouringFeedback()
    }

    fun triggerAwesomeFeedback() {
        soundHapticManager?.stopPouringFeedback()
        soundHapticManager?.playSuccessFeedback()
    }

    fun placeCherry() {
        _uiState.update {
            it.copy(targetConfig = it.targetConfig.copy(hasCherry = true))
        }
    }

    fun addSprinkles(count: Int = 15) {
        _uiState.update {
            it.copy(targetConfig = it.targetConfig.copy(sprinklesCount = it.targetConfig.sprinklesCount + count))
        }
    }

    fun addCoins(amount: Int) {
        val newCoins = _uiState.value.coins + amount
        viewModelScope.launch {
            repository.saveCoins(newCoins)
        }
        _uiState.update { it.copy(coins = newCoins) }
    }

    // Settings actions
    fun openSettings() {
        _uiState.update { it.copy(showSettingsDialog = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }

    fun toggleSound() {
        val newVal = !_uiState.value.isSoundEnabled
        soundHapticManager?.isSoundEnabled = newVal
        _uiState.update { it.copy(isSoundEnabled = newVal) }
        viewModelScope.launch {
            repository.saveSoundEnabled(newVal)
        }
    }

    fun toggleHaptics() {
        val newVal = !_uiState.value.isHapticsEnabled
        soundHapticManager?.isHapticsEnabled = newVal
        _uiState.update { it.copy(isHapticsEnabled = newVal) }
        viewModelScope.launch {
            repository.saveHapticsEnabled(newVal)
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            repository.resetProgress()
            _uiState.update {
                it.copy(
                    level = 1,
                    coins = 250,
                    showSettingsDialog = false
                )
            }
            startGame()
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundHapticManager?.release()
    }
}
