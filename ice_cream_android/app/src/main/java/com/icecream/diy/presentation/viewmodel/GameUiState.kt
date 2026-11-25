package com.icecream.diy.presentation.viewmodel

import com.icecream.diy.data.local.LevelDataProvider
import com.icecream.diy.data.model.AppScreen
import com.icecream.diy.data.model.Flavor
import com.icecream.diy.data.model.GamePhase
import com.icecream.diy.data.model.LevelConfig
import com.icecream.diy.domain.scoring.MatchResult
import com.icecream.diy.domain.scoring.UserPouredSegment

data class GameUiState(
    val screen: AppScreen = AppScreen.SPLASH,
    val phase: GamePhase = GamePhase.POURING,
    val level: Int = 1,
    val coins: Int = 250,
    val stars: Int = 0,
    val matchScore: Int = 0,
    val targetConfig: LevelConfig = LevelDataProvider.getLevel(null, 1),
    val activeFlavors: Set<Flavor> = emptySet(),
    val pourProgress: Float = 0.0f,
    val isRotating: Boolean = true,
    val userPouredSegments: List<UserPouredSegment> = emptyList(),
    val matchResult: MatchResult? = null,
    val showTutorial: Boolean = true,
    val customerVariant: Int = 0,
    val earnedCoins: Int = 0,
    val isSoundEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val showSettingsDialog: Boolean = false,
    val showResultsOverlay: Boolean = false,
    val isDecorated: Boolean = false,
    val decorationBonusCoins: Int = 15,
    val showMysteryCone: Boolean = false,
    val isRewardDoubled: Boolean = false,
    val sessionNonce: Int = 0
)
