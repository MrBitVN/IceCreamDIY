package com.icecream.diy

import android.content.ComponentCallbacks2
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

        val appCtx = applicationContext
        val repository = DataStoreProgressRepository(appCtx)
        soundHapticManager = SoundHapticManager(appCtx)
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GameViewModel(appCtx, repository, soundHapticManager) as T
                }
            }
        )[GameViewModel::class.java]

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
                                onDecorateClick = {
                                    viewModel.onDecorateClicked()
                                },
                                onDoubleReward = {
                                    viewModel.onDoubleRewardClicked()
                                },
                                onDismissMysteryCone = {
                                    viewModel.onDismissMysteryCone()
                                },
                                onNextLevel = {
                                    viewModel.onRequestNextLevel()
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

    override fun onPause() {
        super.onPause()
        if (::viewModel.isInitialized) {
            viewModel.onAppBackgrounded()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW && ::viewModel.isInitialized) {
            viewModel.onAppBackgrounded()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            soundHapticManager.release()
        }
    }
}
