package com.icecream.diy.data.model

enum class AppScreen {
    SPLASH,
    HOME,
    GAME
}

enum class GamePhase {
    PREPARE_MINIGAME,
    POURING,
    DECORATING,
    EVALUATING,
    CUSTOMER_REACTION
}

data class PouredLayer(
    val flavors: List<Flavor>,
    val height: Float,
    val yPosition: Float
)

data class GameState(
    val screen: AppScreen = AppScreen.SPLASH,
    val phase: GamePhase = GamePhase.POURING,
    val level: Int = 1,
    val coins: Int = 250,
    val stars: Int = 0,
    val activeFlavors: Set<Flavor> = emptySet(),
    val pourProgress: Float = 0.0f,
    val coneRotationAngle: Float = 0.0f,
    val pouredLayers: List<PouredLayer> = emptyList()
)
