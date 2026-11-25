package com.icecream.diy.domain.engine

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sqrt

enum class MinigameIngredient(
    val index: Int,
    val label: String,
    val rewardEmoji: String,
    val boardX: Float,
    val boardZ: Float,
    val targetMinX: Float,
    val targetMaxX: Float,
    val targetMinZ: Float,
    val targetMaxZ: Float,
    val targetCenterX: Float,
    val targetCenterZ: Float
) {
    // Board: Left=Blueberry, Center=Mint, Right=Mango
    // Box Compartments: Left=Mint, Center=Mango, Right=Blueberry (each compartment shows a 3D sample hint!)
    BLUEBERRY(
        index = 0,
        label = "BERRY",
        rewardEmoji = "😎",
        boardX = -0.58f,
        boardZ = -0.78f,
        targetMinX = 0.41f,
        targetMaxX = 1.24f,
        targetMinZ = 0.16f,
        targetMaxZ = 1.48f,
        targetCenterX = 0.82f,
        targetCenterZ = 0.82f
    ),
    MINT(
        index = 1,
        label = "MINT",
        rewardEmoji = "😍",
        boardX = 0.12f,
        boardZ = -0.78f,
        targetMinX = -1.24f,
        targetMaxX = -0.41f,
        targetMinZ = 0.16f,
        targetMaxZ = 1.48f,
        targetCenterX = -0.82f,
        targetCenterZ = 0.82f
    ),
    MANGO(
        index = 2,
        label = "MANGO",
        rewardEmoji = "😊",
        boardX = 0.82f,
        boardZ = -0.78f,
        targetMinX = -0.40f,
        targetMaxX = 0.40f,
        targetMinZ = 0.16f,
        targetMaxZ = 1.48f,
        targetCenterX = 0.00f,
        targetCenterZ = 0.82f
    );
}

object MinigameEngine {

    /**
     * Hit-tests a 3D world point (worldX, worldZ) on the sorting box plane
     * and returns which compartment was hit, or null if outside the box.
     */
    fun hitTestCompartment(worldX: Float, worldZ: Float): MinigameIngredient? {
        val entries = MinigameIngredient.values()
        for (i in entries.indices) {
            val item = entries[i]
            if (worldX >= item.targetMinX && worldX <= item.targetMaxX &&
                worldZ >= item.targetMinZ && worldZ <= item.targetMaxZ
            ) {
                return item
            }
        }
        return null
    }

    /**
     * Hit-tests a 3D world point (worldX, worldZ) on the cutting board plane
     * against an ingredient currently at (posX, posZ).
     */
    fun hitTestIngredient(
        worldX: Float,
        worldZ: Float,
        posX: Float,
        posZ: Float,
        hitRadius: Float = 0.36f
    ): Boolean {
        val dx = worldX - posX
        val dz = worldZ - posZ
        return sqrt(dx * dx + dz * dz) <= hitRadius
    }

    /**
     * Damped harmonic oscillator for elastic snap bounce:
     * Starts at 1.38x scale on impact and oscillates smoothly to 1.0x.
     */
    fun elasticBounceScale(elapsedSec: Float): Float {
        if (elapsedSec <= 0f) return 1.38f
        if (elapsedSec >= 0.85f) return 1.0f
        return 1.0f + 0.38f * exp(-6.2f * elapsedSec) * cos(18.5f * elapsedSec)
    }

    /**
     * Milestone coin reward for sorted ingredient count (1..3):
     * - 2nd sorted ingredient -> +5 coins
     * - 3rd sorted ingredient -> +10 coins
     */
    fun milestoneCoinsForCount(sortedCount: Int): Int = when (sortedCount) {
        2 -> 5
        3 -> 10
        else -> 0
    }
}
