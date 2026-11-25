package com.icecream.diy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.icecream.diy.data.model.AppScreen
import com.icecream.diy.data.repository.DataStoreProgressRepository
import com.icecream.diy.domain.engine.SoundHapticManager
import com.icecream.diy.presentation.screens.GameScreen
import com.icecream.diy.presentation.screens.HomeScreen
import com.icecream.diy.presentation.screens.SplashScreen
import com.icecream.diy.presentation.theme.IceCreamTheme
import com.icecream.diy.presentation.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: GameViewModel
    private lateinit var soundHapticManager: SoundHapticManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = DataStoreProgressRepository(applicationContext)
        soundHapticManager = SoundHapticManager(applicationContext)
        viewModel = GameViewModel(applicationContext, repository, soundHapticManager)

        setContent {
            IceCreamTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val uiState by viewModel.uiState.collectAsState()

                    when (uiState.screen) {
                        AppScreen.SPLASH -> {
                            SplashScreen(
                                onSplashFinished = {
                                    viewModel.navigateToHome()
                                }
                            )
                        }
                        AppScreen.HOME -> {
                            HomeScreen(
                                level = uiState.level,
                                coins = uiState.coins,
                                onPlayClick = {
                                    viewModel.startGame()
                                }
                            )
                        }
                        AppScreen.GAME -> {
                            GameScreen(
                                uiState = uiState,
                                onFlavorPressed = { flavor ->
                                    viewModel.onFlavorPressed(flavor)
                                },
                                onFlavorReleased = { flavor ->
                                    viewModel.onFlavorReleased(flavor)
                                },
                                onRestartClick = {
                                    viewModel.restartLevel()
                                },
                                onSettingsClick = {
                                    viewModel.openSettings()
                                },
                                onCloseSettings = {
                                    viewModel.closeSettings()
                                },
                                onToggleSound = {
                                    viewModel.toggleSound()
                                },
                                onToggleHaptics = {
                                    viewModel.toggleHaptics()
                                },
                                onResetProgress = {
                                    viewModel.resetProgress()
                                },
                                onNextLevel = {
                                    viewModel.nextLevel()
                                },
                                onReplayLevel = {
                                    viewModel.restartLevel()
                                },
                                onMinigameComplete = {
                                    viewModel.completeMinigame()
                                },
                                onBlenderComplete = {
                                    viewModel.completeBlender()
                                },
                                onMinigamePulse = {
                                    viewModel.triggerMinigamePulse()
                                },
                                onBlenderTick = {
                                    viewModel.triggerBlenderTick()
                                },
                                onAwesomeFeedback = {
                                    viewModel.triggerAwesomeFeedback()
                                },
                                onAddCoins = { amount ->
                                    viewModel.addCoins(amount)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundHapticManager.release()
    }
}
