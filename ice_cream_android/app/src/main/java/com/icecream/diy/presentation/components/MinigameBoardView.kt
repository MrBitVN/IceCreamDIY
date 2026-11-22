package com.icecream.diy.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icecream.diy.presentation.theme.BgGradientBottom
import com.icecream.diy.presentation.theme.BgGradientTop
import com.icecream.diy.presentation.theme.ProgressGreen
import com.icecream.diy.presentation.theme.TextDark
import kotlin.math.roundToInt

enum class IngredientType {
    CHOCOLATE,
    FLOWER,
    STRAWBERRY
}

@Composable
fun MinigameBoardView(
    level: Int,
    coins: Int,
    onMinigameComplete: () -> Unit,
    onAddCoins: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onRestartClick: () -> Unit
) {
    var sortedItems by remember { mutableStateOf(setOf<IngredientType>()) }
    var currentProgress by remember { mutableFloatStateOf(0f) }

    // Coordinates of box compartments
    var strawberryTargetCenter by remember { mutableStateOf(Offset.Zero) }
    var flowerTargetCenter by remember { mutableStateOf(Offset.Zero) }
    var chocolateTargetCenter by remember { mutableStateOf(Offset.Zero) }

    val animatedProgress by animateFloatAsState(
        targetValue = currentProgress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "miniProgress"
    )

    fun checkDrop(type: IngredientType, dropOffset: Offset): Boolean {
        val target = when (type) {
            IngredientType.STRAWBERRY -> strawberryTargetCenter
            IngredientType.FLOWER -> flowerTargetCenter
            IngredientType.CHOCOLATE -> chocolateTargetCenter
        }
        val distance = (dropOffset - target).getDistance()
        if (distance < 160f) {
            sortedItems = sortedItems + type
            val count = sortedItems.size
            currentProgress = count / 3.0f

            if (count == 2) onAddCoins(5)
            if (count == 3) {
                onAddCoins(10)
                onMinigameComplete()
            }
            return true
        }
        return false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(BgGradientTop, BgGradientBottom)
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top HUD Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(3.dp, CircleShape)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                    CoinPillView(coins = coins)
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .align(Alignment.TopEnd)
                        .shadow(3.dp, CircleShape)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }

            // Level Pill & Progress bar with milestone rewards
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .shadow(3.dp, RoundedCornerShape(50))
                        .background(Color.White, RoundedCornerShape(50))
                        .padding(horizontal = 24.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "LEVEL $level",
                        color = TextDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Minigame Milestone Progress Bar
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(24.dp)
                        .shadow(3.dp, RoundedCornerShape(50))
                        .background(Color.White, RoundedCornerShape(50))
                        .padding(3.dp)
                ) {
                    // Green fill
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .background(ProgressGreen, RoundedCornerShape(50))
                    )

                    // Milestone markers
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "+5", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        Text(text = "+10", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2. Wooden Cutting Board with Handle Hole
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(180.dp)
                    .shadow(8.dp, RoundedCornerShape(22.dp))
                    .background(Color(0xFFF9DCBA), RoundedCornerShape(22.dp))
                    .border(3.dp, Color(0xFFE2B78E), RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                // Handle hole on the left
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(44.dp)
                        .align(Alignment.CenterStart)
                        .background(BgGradientTop, RoundedCornerShape(9.dp))
                        .border(2.dp, Color(0xFFD0A57C), RoundedCornerShape(9.dp))
                )

                // Draggable ingredients
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!sortedItems.contains(IngredientType.CHOCOLATE)) {
                        DraggableItem(type = IngredientType.CHOCOLATE, onDropped = { checkDrop(IngredientType.CHOCOLATE, it) })
                    }
                    if (!sortedItems.contains(IngredientType.FLOWER)) {
                        DraggableItem(type = IngredientType.FLOWER, onDropped = { checkDrop(IngredientType.FLOWER, it) })
                    }
                    if (!sortedItems.contains(IngredientType.STRAWBERRY)) {
                        DraggableItem(type = IngredientType.STRAWBERRY, onDropped = { checkDrop(IngredientType.STRAWBERRY, it) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Wooden Sorting Box with Compartments
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(220.dp)
                    .shadow(10.dp, RoundedCornerShape(18.dp))
                    .background(Color(0xFFC49866), RoundedCornerShape(18.dp))
                    .border(4.dp, Color(0xFF9E703E), RoundedCornerShape(18.dp))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Top row: Strawberry slot (left) & Flower slot (right)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Strawberry compartment
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color(0xFFE8C8A2), RoundedCornerShape(12.dp))
                                .border(2.dp, Color(0xFFB5895B), RoundedCornerShape(12.dp))
                                .onGloballyPositioned { coords ->
                                    val bounds = coords.boundsInRoot()
                                    strawberryTargetCenter = bounds.center
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (sortedItems.contains(IngredientType.STRAWBERRY)) {
                                IngredientGraphic(type = IngredientType.STRAWBERRY, scale = 1.1f)
                            }
                        }

                        // Flower compartment
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color(0xFFE8C8A2), RoundedCornerShape(12.dp))
                                .border(2.dp, Color(0xFFB5895B), RoundedCornerShape(12.dp))
                                .onGloballyPositioned { coords ->
                                    val bounds = coords.boundsInRoot()
                                    flowerTargetCenter = bounds.center
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (sortedItems.contains(IngredientType.FLOWER)) {
                                IngredientGraphic(type = IngredientType.FLOWER, scale = 1.1f)
                            }
                        }
                    }

                    // Bottom row: Chocolate bar compartment
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFFE8C8A2), RoundedCornerShape(12.dp))
                            .border(2.dp, Color(0xFFB5895B), RoundedCornerShape(12.dp))
                            .onGloballyPositioned { coords ->
                                val bounds = coords.boundsInRoot()
                                chocolateTargetCenter = bounds.center
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (sortedItems.contains(IngredientType.CHOCOLATE)) {
                            IngredientGraphic(type = IngredientType.CHOCOLATE, scale = 1.05f)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DraggableItem(
    type: IngredientType,
    onDropped: (Offset) -> Boolean
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var rootPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .onGloballyPositioned { coords ->
                rootPosition = coords.boundsInRoot().topLeft
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                    },
                    onDragEnd = {
                        val finalCenter = rootPosition + dragOffset + Offset(35f, 35f)
                        val success = onDropped(finalCenter)
                        if (!success) {
                            dragOffset = Offset.Zero
                        }
                    }
                )
            }
    ) {
        IngredientGraphic(type = type, scale = 1.0f)
    }
}

@Composable
fun IngredientGraphic(type: IngredientType, scale: Float = 1.0f) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        when (type) {
            IngredientType.CHOCOLATE -> {
                Canvas(modifier = Modifier.size(72.dp, 36.dp)) {
                    val chocoColor = Color(0xFF6A3B18)
                    val grooveColor = Color(0xFF4C270E)
                    drawRoundRect(chocoColor, cornerRadius = CornerRadius(6f, 6f))

                    // Grid partition lines
                    for (x in 1..3) {
                        drawLine(grooveColor, Offset(x * 18f, 0f), Offset(x * 18f, size.height), strokeWidth = 2f)
                    }
                    drawLine(grooveColor, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = 2f)

                    // Happy yellow emoji on left
                    drawCircle(Color(0xFFFFD524), radius = 10f, center = Offset(14f, size.height / 2f))
                    drawCircle(Color(0xFF333333), radius = 1.5f, center = Offset(11f, size.height / 2f - 2f))
                    drawCircle(Color(0xFF333333), radius = 1.5f, center = Offset(17f, size.height / 2f - 2f))
                }
            }
            IngredientType.FLOWER -> {
                Canvas(modifier = Modifier.size(54.dp)) {
                    val petalColor = Color(0xFFFAF7F2)
                    val centerColor = Color(0xFFFFD524)
                    val mid = Offset(size.width / 2f, size.height / 2f)
                    for (i in 0 until 5) {
                        val angle = (i * 72f) * Math.PI.toFloat() / 180f
                        val pOffset = Offset(mid.x + 14f * kotlin.math.cos(angle), mid.y + 14f * kotlin.math.sin(angle))
                        drawCircle(petalColor, radius = 11f, center = pOffset)
                    }
                    drawCircle(centerColor, radius = 7f, center = mid)
                }
            }
            IngredientType.STRAWBERRY -> {
                Canvas(modifier = Modifier.size(52.dp)) {
                    val strawberryPath = Path().apply {
                        moveTo(size.width / 2f, size.height * 0.95f)
                        cubicTo(size.width * 0.1f, size.height * 0.6f, size.width * 0.05f, size.height * 0.25f, size.width * 0.35f, size.height * 0.2f)
                        cubicTo(size.width * 0.5f, size.height * 0.25f, size.width * 0.65f, size.height * 0.2f, size.width * 0.95f, size.height * 0.25f)
                        cubicTo(size.width * 0.9f, size.height * 0.6f, size.width * 0.9f, size.height * 0.7f, size.width / 2f, size.height * 0.95f)
                        close()
                    }
                    drawPath(strawberryPath, color = Color(0xFFE82845))

                    // Green leaves
                    val leafColor = Color(0xFF4DB028)
                    drawCircle(leafColor, radius = 6f, center = Offset(size.width / 2f - 7f, size.height * 0.18f))
                    drawCircle(leafColor, radius = 7f, center = Offset(size.width / 2f, size.height * 0.14f))
                    drawCircle(leafColor, radius = 6f, center = Offset(size.width / 2f + 7f, size.height * 0.18f))

                    // White seeds
                    drawCircle(Color.White, radius = 1.2f, center = Offset(size.width * 0.35f, size.height * 0.45f))
                    drawCircle(Color.White, radius = 1.2f, center = Offset(size.width * 0.65f, size.height * 0.45f))
                    drawCircle(Color.White, radius = 1.2f, center = Offset(size.width * 0.50f, size.height * 0.65f))
                }
            }
        }
    }
}
