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

data class LevelConfig(
    val levelNumber: Int,
    val hasMinigame: Boolean = false,
    val availableFlavors: List<Flavor> = listOf(Flavor.VANILLA, Flavor.STRAWBERRY, Flavor.CHOCOLATE),
    val targetLayers: List<TargetLayer> = listOf(TargetLayer(listOf(Flavor.VANILLA), 1.0f)),
    val sprinklesCount: Int = 0,
    val hasCherry: Boolean = false,
    val extraCones: Int = 1,
    val customer: CustomerConfig = CustomerConfig()
)
