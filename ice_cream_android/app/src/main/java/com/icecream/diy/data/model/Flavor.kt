package com.icecream.diy.data.model

import androidx.compose.ui.graphics.Color

enum class Flavor(
    val displayName: String,
    val color: Color,
    val rgbFloat: FloatArray,
    val slotIndex: Int
) {
    VANILLA(
        displayName = "Vanilla",
        color = Color(0xFFFAF7F2),
        rgbFloat = floatArrayOf(0.98f, 0.97f, 0.95f),
        slotIndex = 0
    ),
    STRAWBERRY(
        displayName = "Strawberry",
        color = Color(0xFFFF84D6),
        rgbFloat = floatArrayOf(1.0f, 0.52f, 0.84f),
        slotIndex = 1
    ),
    CHOCOLATE(
        displayName = "Chocolate",
        color = Color(0xFF704328),
        rgbFloat = floatArrayOf(0.44f, 0.26f, 0.16f),
        slotIndex = 2
    ),
    PISTACHIO(
        displayName = "Pistachio",
        color = Color(0xFFA7E956),
        rgbFloat = floatArrayOf(0.65f, 0.91f, 0.34f),
        slotIndex = 3
    ),
    BLUEBERRY(
        displayName = "Blueberry",
        color = Color(0xFF5BC4F2),
        rgbFloat = floatArrayOf(0.36f, 0.77f, 0.95f),
        slotIndex = 4
    );
}
