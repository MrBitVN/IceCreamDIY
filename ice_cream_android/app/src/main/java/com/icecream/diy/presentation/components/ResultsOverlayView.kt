package com.icecream.diy.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icecream.diy.data.model.Flavor
import com.icecream.diy.data.model.TargetLayer
import com.icecream.diy.domain.scoring.UserPouredSegment
import kotlinx.coroutines.delay

@Composable
fun ResultsOverlayView(
    stars: Int,
    matchScore: Int,
    earnedCoins: Int,
    targetLayers: List<TargetLayer> = emptyList(),
    userSegments: List<UserPouredSegment> = emptyList(),
    hasCherry: Boolean = false,
    isRewardDoubled: Boolean = false,
    onDoubleReward: () -> Unit = {},
    onNextLevel: () -> Unit,
    onReplayLevel: () -> Unit
) {
    val starScale1 = remember { Animatable(0f) }
    val starScale2 = remember { Animatable(0f) }
    val starScale3 = remember { Animatable(0f) }
    val animatedCoins = remember { Animatable(0f) }

    LaunchedEffect(stars) {
        if (stars >= 1) {
            starScale1.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        if (stars >= 2) {
            delay(140)
            starScale2.animateTo(1.2f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        if (stars >= 3) {
            delay(140)
            starScale3.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    LaunchedEffect(earnedCoins) {
        animatedCoins.animateTo(
            targetValue = earnedCoins.toFloat(),
            animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.38f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .shadow(24.dp, RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White)
                .border(4.dp, Color(0xFFFF94A8), RoundedCornerShape(32.dp))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val titleText = when (stars) {
                    3 -> "PERFECT!"
                    2 -> "GREAT JOB!"
                    1 -> "GOOD TRY!"
                    else -> "TRY AGAIN!"
                }
                Text(
                    text = titleText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF406E)
                )

                Text(
                    text = "$matchScore% MATCH",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF777777)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3 Stars Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier
                            .size(46.dp)
                            .scale(if (stars >= 1) starScale1.value else 0.85f),
                        tint = if (stars >= 1) Color(0xFFFFC107) else Color(0xFFDDDDDD)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier
                            .size(58.dp)
                            .offset(y = (-6).dp)
                            .scale(if (stars >= 2) starScale2.value else 0.85f),
                        tint = if (stars >= 2) Color(0xFFFFC107) else Color(0xFFDDDDDD)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier
                            .size(46.dp)
                            .scale(if (stars >= 3) starScale3.value else 0.85f),
                        tint = if (stars >= 3) Color(0xFFFFC107) else Color(0xFFDDDDDD)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Side-by-side 2 Ice Creams Comparison (TARGET vs YOURS)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFFFF4F7))
                        .border(2.dp, Color(0xFFFFD5E0), RoundedCornerShape(18.dp))
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniComparisonCone(
                        label = "TARGET",
                        layers = targetLayers.map { it.flavors to it.proportion },
                        hasCherry = hasCherry
                    )

                    Text(
                        text = "VS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF7D9B)
                    )

                    val userLayers = summarizeUserSegments(userSegments)
                    MiniComparisonCone(
                        label = "YOURS",
                        layers = userLayers,
                        hasCherry = hasCherry && stars > 0
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Earned Coins Count-Up Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFFF5E1))
                        .border(2.dp, Color(0xFFFFB300), RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFB300)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+${animatedCoins.value.toInt()} COINS",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF8C00)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Double Reward Placeholder Button (2X COINS)
                if (!isRewardDoubled && earnedCoins > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .shadow(5.dp, RoundedCornerShape(23.dp))
                            .clip(RoundedCornerShape(23.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFFC107), Color(0xFFFF9800))
                                )
                            )
                            .clickable { onDoubleReward() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "2X COINS (+${earnedCoins})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Next Level Button (Green Glossy) when stars >= 1
                if (stars >= 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(8.dp, RoundedCornerShape(26.dp))
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF4CAF50), Color(0xFF388E3C))
                                )
                            )
                            .clickable { onNextLevel() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NEXT LEVEL",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Replay Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFFEEEEEE))
                        .clickable { onReplayLevel() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Replay",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Replay",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniComparisonCone(
    label: String,
    layers: List<Pair<List<Flavor>, Float>>,
    hasCherry: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(width = 64.dp, height = 84.dp)) {
            val w = size.width
            val h = size.height

            // Mini waffle cone
            val conePath = Path().apply {
                moveTo(w * 0.25f, h * 0.62f)
                lineTo(w * 0.75f, h * 0.62f)
                lineTo(w * 0.50f, h * 0.96f)
                close()
            }
            drawPath(conePath, color = Color(0xFFEBBE7D))

            // Ice cream swirl layers stacked bottom-to-top
            val totalRatio = layers.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(0.01f)
            var currentBottom = h * 0.62f
            val maxSwirlH = h * 0.44f

            layers.forEachIndexed { idx, (flavors, ratio) ->
                val layerH = (ratio / totalRatio) * maxSwirlH
                val topY = currentBottom - layerH
                val widthFrac = 0.48f - (idx.toFloat() / (layers.size + 1)) * 0.22f
                val primary = flavors.firstOrNull()?.color ?: Color(0xFFF3E5AB)

                drawCircle(
                    color = primary,
                    radius = w * widthFrac * 0.5f,
                    center = Offset(w * 0.5f, (currentBottom + topY) * 0.5f)
                )
                if (flavors.size >= 2) {
                    drawCircle(
                        color = flavors[1].color,
                        radius = w * widthFrac * 0.26f,
                        center = Offset(w * 0.54f, (currentBottom + topY) * 0.5f)
                    )
                }
                currentBottom = topY
            }

            if (hasCherry) {
                drawCircle(
                    color = Color(0xFFEB1428),
                    radius = w * 0.08f,
                    center = Offset(w * 0.5f, (currentBottom - 4f).coerceAtLeast(h * 0.12f))
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF777777)
        )
    }
}

private fun summarizeUserSegments(segments: List<UserPouredSegment>): List<Pair<List<Flavor>, Float>> {
    if (segments.isEmpty()) return emptyList()
    val grouped = mutableListOf<Pair<List<Flavor>, Float>>()
    var currentFlavors = segments.first().flavors.toList().sortedBy { it.name }
    var currentHeight = 0f

    for (seg in segments) {
        val fList = seg.flavors.toList().sortedBy { it.name }
        val h = (seg.heightEnd - seg.heightStart).coerceAtLeast(0f)
        if (fList == currentFlavors) {
            currentHeight += h
        } else {
            if (currentHeight > 0f) grouped.add(currentFlavors to currentHeight)
            currentFlavors = fList
            currentHeight = h
        }
    }
    if (currentHeight > 0f) grouped.add(currentFlavors to currentHeight)
    return grouped
}
