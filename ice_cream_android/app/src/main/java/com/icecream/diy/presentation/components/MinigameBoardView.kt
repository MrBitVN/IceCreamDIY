package com.icecream.diy.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icecream.diy.domain.engine.MinigameEngine
import com.icecream.diy.domain.engine.MinigameIngredient
import com.icecream.diy.presentation.theme.BgGradientBottom
import com.icecream.diy.presentation.theme.BgGradientTop
import com.icecream.diy.presentation.theme.ProgressGreen
import com.icecream.diy.presentation.theme.TextDark
import com.icecream.diy.scene3d.SlicingMinigameSceneView
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

data class FloatingEmojiEffect(
    val id: Long,
    val emoji: String,
    val startX: Float,
    val startY: Float
)

data class FlyingCoinEffect(
    val id: Long,
    val amountText: String,
    val startX: Float,
    val startY: Float,
    val delayMs: Long = 0L
)

private data class SlashPoint(
    val x: Float,
    val y: Float,
    val timestampMs: Long
)

@Composable
fun MinigameBoardView(
    level: Int,
    coins: Int,
    onMinigameComplete: () -> Unit,
    onAddCoins: (Int) -> Unit,
    onActionFeedback: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onRestartClick: () -> Unit
) {
    var slicedSet by remember { mutableStateOf(setOf<MinigameIngredient>()) }
    var sortedSet by remember { mutableStateOf(setOf<MinigameIngredient>()) }
    var coinWalletCenterPx by remember { mutableStateOf(Offset(140f, 130f)) }

    var floatingEmojis by remember { mutableStateOf(listOf<FloatingEmojiEffect>()) }
    var flyingCoins by remember { mutableStateOf(listOf<FlyingCoinEffect>()) }
    var slashPoints by remember { mutableStateOf(listOf<SlashPoint>()) }
    var effectCounter by remember { mutableLongStateOf(1L) }

    // Progress combines slicing (up to 0.30) + sorting into compartments (up to 1.0)
    val rawProgress = ((slicedSet.size * 0.08f) + (sortedSet.size * 0.26f)).coerceIn(0f, 1f).let {
        if (sortedSet.size == 3) 1.0f else if (sortedSet.size == 2) 0.67f else it
    }

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "slicingProgress"
    )

    // Fade slash trail automatically
    LaunchedEffect(slashPoints.size) {
        if (slashPoints.isNotEmpty()) {
            delay(180)
            val cutoff = System.currentTimeMillis() - 160
            slashPoints = slashPoints.filter { it.timestampMs >= cutoff }
        }
    }

    // Auto-transition once all 3 ingredients have snapped into their compartments
    LaunchedEffect(sortedSet.size) {
        if (sortedSet.size == 3) {
            delay(1150)
            onMinigameComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BgGradientTop, BgGradientBottom)
                )
            )
    ) {
        // 1. 3D Procedural Scene (OpenGL ES 3.0): Pink Cutting Board, 3D Blueberry/Mint/Mango, 3D Knife, 3-Compartment Box
        SlicingMinigameSceneView(
            onSwipeTrail = { sx, sy ->
                val now = System.currentTimeMillis()
                slashPoints = (slashPoints.filter { now - it.timestampMs < 150 } + SlashPoint(sx, sy, now)).takeLast(14)
            },
            onKnifeSlice = { ingredient, sx, sy ->
                slicedSet = slicedSet + ingredient
                onActionFeedback()
                val id = effectCounter++
                floatingEmojis = (floatingEmojis + FloatingEmojiEffect(id, "😊", sx - 24f, sy - 48f)).takeLast(8)
            },
            onIngredientSorted = { ingredient, sx, sy ->
                if (!sortedSet.contains(ingredient)) {
                    val updated = sortedSet + ingredient
                    sortedSet = updated
                    onActionFeedback()

                    val eid = effectCounter++
                    floatingEmojis = (floatingEmojis + FloatingEmojiEffect(eid, ingredient.rewardEmoji, sx - 28f, sy - 54f)).takeLast(8)

                    val bonusCoins = MinigameEngine.milestoneCoinsForCount(updated.size)
                    if (bonusCoins > 0) {
                        onAddCoins(bonusCoins)
                        val cid1 = effectCounter++
                        val cid2 = effectCounter++
                        flyingCoins = (flyingCoins + listOf(
                            FlyingCoinEffect(cid1, "+$bonusCoins", sx, sy, 0L),
                            FlyingCoinEffect(cid2, "$", sx + 28f, sy - 16f, 110L)
                        )).takeLast(8)
                    }
                }
            },
            onWrongCompartment = {
                onActionFeedback()
            }
        )

        // 2. Knife Swipe Slash Trail Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (slashPoints.size >= 2) {
                for (i in 1 until slashPoints.size) {
                    val p0 = slashPoints[i - 1]
                    val p1 = slashPoints[i]
                    val frac = i.toFloat() / slashPoints.size.toFloat()
                    drawLine(
                        color = Color.White.copy(alpha = 0.85f * frac),
                        start = Offset(p0.x, p0.y),
                        end = Offset(p1.x, p1.y),
                        strokeWidth = 10f * frac,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // 3. 2D HUD Overlay matching reference photo 4
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Left: Settings button & Coin Pill
                Column(
                    modifier = Modifier.align(Alignment.TopStart),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(3.dp, CircleShape)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape)
                            .clickable { onSettingsClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.Gray,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier.onGloballyPositioned { coords ->
                            coinWalletCenterPx = coords.boundsInRoot().center
                        }
                    ) {
                        CoinPillView(coins = coins)
                    }
                }

                // Center: LEVEL N capsule + Milestone Progress Bar (+5 and +10 with gold coin icons)
                Column(
                    modifier = Modifier.align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(3.dp, RoundedCornerShape(50))
                            .background(Color.White, RoundedCornerShape(50))
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "LEVEL $level",
                            color = TextDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    MilestoneProgressBar(progress = animatedProgress)
                }

                // Right: Restart button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .align(Alignment.TopEnd)
                        .shadow(3.dp, CircleShape)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                        .clickable { onRestartClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart",
                        tint = Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Helpful dynamic instruction pill
            val instructionText = when {
                sortedSet.size == 3 -> "GREAT JOB!"
                slicedSet.isEmpty() -> "SWIPE ACROSS FRUIT TO SLICE WITH KNIFE!"
                else -> "SLICE & DRAG INTO MATCHING BOX COMPARTMENT!"
            }

            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.18f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = instructionText,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Compartment labels along the bottom matching Left=MINT, Center=MANGO, Right=BERRY
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompartmentBadge("MINT", sortedSet.contains(MinigameIngredient.MINT))
                CompartmentBadge("MANGO", sortedSet.contains(MinigameIngredient.MANGO))
                CompartmentBadge("BERRY", sortedSet.contains(MinigameIngredient.BLUEBERRY))
            }
        }

        // 4. Floating Emojis (😎 😍 😊)
        floatingEmojis.forEach { effect ->
            key(effect.id) {
                FloatingEmojiOverlay(effect = effect)
            }
        }

        // 5. Flying Coins into Wallet (+5, +10)
        flyingCoins.forEach { coin ->
            key(coin.id) {
                FlyingCoinOverlay(coin = coin, walletCenterPx = coinWalletCenterPx)
            }
        }
    }
}

@Composable
private fun MilestoneProgressBar(progress: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Gold coin icons positioned above +5 (64%) and +10 (96%)
        Box(modifier = Modifier.width(164.dp).height(18.dp)) {
            GoldMiniCoinBadge(modifier = Modifier.align(Alignment.CenterStart).offset(x = 94.dp))
            GoldMiniCoinBadge(modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-2).dp))
        }

        // Capsule progress bar
        Box(
            modifier = Modifier
                .width(164.dp)
                .height(22.dp)
                .shadow(3.dp, RoundedCornerShape(50))
                .background(Color.White, RoundedCornerShape(50))
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(ProgressGreen, RoundedCornerShape(50))
            )

            // Vertical green milestone divider at ~2/3 mark
            Box(
                modifier = Modifier
                    .offset(x = 100.dp)
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(ProgressGreen)
            )
        }

        // +5 and +10 labels below bar
        Box(modifier = Modifier.width(164.dp).padding(top = 2.dp)) {
            Text(
                text = "+5",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.CenterStart).offset(x = 92.dp)
            )
            Text(
                text = "+10",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun GoldMiniCoinBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(18.dp)
            .shadow(2.dp, CircleShape)
            .background(Color(0xFFFFCA28), CircleShape)
            .border(1.5.dp, Color(0xFFF57F17), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun CompartmentBadge(label: String, isDone: Boolean) {
    val bgColor = if (isDone) Color(0xFF76D828) else Color.White.copy(alpha = 0.85f)
    val txtColor = if (isDone) Color.White else TextDark
    Box(
        modifier = Modifier
            .shadow(3.dp, RoundedCornerShape(50))
            .background(bgColor, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        Text(
            text = if (isDone) "✓ $label" else label,
            color = txtColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun FloatingEmojiOverlay(effect: FloatingEmojiEffect) {
    var progress by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    LaunchedEffect(effect.id) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1050, easing = LinearOutSlowInEasing)
        ) { value, _ ->
            progress = value
        }
    }

    if (progress < 1f) {
        val risePx = -150f * progress
        val swayPx = sin(progress * PI.toFloat() * 2f) * 20f
        val alphaVal = (1f - progress).coerceIn(0f, 1f)
        val scaleVal = 0.85f + 0.35f * sin(progress.coerceAtMost(0.5f) * PI.toFloat())
        val fontSp = with(density) { 28.dp.toSp() }

        Text(
            text = effect.emoji,
            fontSize = fontSp,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (effect.startX + swayPx).roundToInt(),
                        (effect.startY + risePx).roundToInt()
                    )
                }
                .scale(scaleVal)
                .alpha(alphaVal)
        )
    }
}

@Composable
private fun FlyingCoinOverlay(coin: FlyingCoinEffect, walletCenterPx: Offset) {
    var progress by remember { mutableFloatStateOf(0f) }
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(coin.id) {
        if (coin.delayMs > 0) delay(coin.delayMs)
        started = true
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing)
        ) { value, _ ->
            progress = value
        }
    }

    if (started && progress < 1f) {
        val arcLift = -110f * sin(progress * PI.toFloat())
        val currX = coin.startX + (walletCenterPx.x - coin.startX) * progress
        val currY = coin.startY + (walletCenterPx.y - coin.startY) * progress + arcLift
        val scaleVal = 1.0f + 0.35f * sin(progress * PI.toFloat())
        val alphaVal = if (progress > 0.82f) ((1f - progress) / 0.18f).coerceIn(0f, 1f) else 1f

        Box(
            modifier = Modifier
                .offset { IntOffset((currX - 24f).roundToInt(), (currY - 14f).roundToInt()) }
                .scale(scaleVal)
                .alpha(alphaVal)
                .shadow(4.dp, RoundedCornerShape(50))
                .background(Color(0xFFFFCA28), RoundedCornerShape(50))
                .border(2.dp, Color(0xFFF57F17), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = coin.amountText,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
