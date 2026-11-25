package com.icecream.diy.data.model

data class TargetLayer(
    val flavors: List<Flavor>,
    val proportion: Float
)

data class CustomerConfig(
    val id: String = "customer_1",
    val name: String = "Guest",
    val gender: String = "female"
)

data class MinigamesConfig(
    val slicing: Boolean = false,
    val blender: Boolean = false
)

data class LevelConfig(
    val levelNumber: Int,
    val hasMinigame: Boolean = false,
    val minigames: MinigamesConfig = MinigamesConfig(slicing = hasMinigame, blender = hasMinigame),
    val availableFlavors: List<Flavor> = listOf(Flavor.VANILLA, Flavor.STRAWBERRY, Flavor.CHOCOLATE),
    val targetLayers: List<TargetLayer> = listOf(TargetLayer(listOf(Flavor.VANILLA), 1.0f)),
    val sprinklesCount: Int = 0,
    val hasCherry: Boolean = false,
    val extraCones: Int = 1,
    val customer: CustomerConfig = CustomerConfig()
) {
    fun initialPhase(): GamePhase = when {
        minigames.slicing -> GamePhase.PREPARE_MINIGAME
        minigames.blender -> GamePhase.BLENDER_MINIGAME
        else -> GamePhase.POURING
    }

    fun phaseAfterSlicing(): GamePhase = if (minigames.blender) {
        GamePhase.BLENDER_MINIGAME
    } else {
        GamePhase.POURING
    }
}
