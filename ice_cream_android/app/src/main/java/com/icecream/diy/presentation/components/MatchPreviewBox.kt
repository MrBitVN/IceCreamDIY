package com.icecream.diy.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icecream.diy.data.model.TargetLayer
import com.icecream.diy.presentation.theme.WaffleConeColor

@Composable
fun MatchPreviewBox(
    targetLayers: List<TargetLayer>,
    hasCherry: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(84.dp)
                .height(112.dp)
                .shadow(4.dp, RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .border(1.5.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val midX = size.width / 2f
                val coneTopY = size.height * 0.60f
                val coneBottomY = size.height * 0.95f
                val coneWidthHalf = size.width * 0.28f

                // 1. Draw Waffle Cone
                val conePath = Path().apply {
                    moveTo(midX - coneWidthHalf, coneTopY)
                    lineTo(midX + coneWidthHalf, coneTopY)
                    lineTo(midX, coneBottomY)
                    close()
                }
                drawPath(conePath, color = WaffleConeColor)

                // Waffle grid lines
                val waffleLineColor = Color(0xFFC79E62)
                for (step in -2..2) {
                    val xOffset = step * 10f
                    drawLine(
                        color = waffleLineColor,
                        start = Offset(midX - coneWidthHalf + xOffset, coneTopY),
                        end = Offset(midX + xOffset, coneBottomY),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = waffleLineColor,
                        start = Offset(midX + coneWidthHalf - xOffset, coneTopY),
                        end = Offset(midX - xOffset, coneBottomY),
                        strokeWidth = 1.5f
                    )
                }

                // 2. Render each Target Layer procedural swirl rings
                var currentBaseY = coneTopY
                val totalSwirlHeight = size.height * 0.45f

                targetLayers.forEachIndexed { index, layer ->
                    val layerHeight = totalSwirlHeight * layer.proportion
                    val layerTopY = currentBaseY - layerHeight
                    val layerMidY = (currentBaseY + layerTopY) / 2f

                    // Tapering width as it ascends
                    val progressBottom = (coneTopY - currentBaseY) / totalSwirlHeight
                    val progressTop = (coneTopY - layerTopY) / totalSwirlHeight
                    val widthBottom = (coneWidthHalf * 2.2f) * (1.0f - 0.55f * progressBottom)
                    val widthTop = (coneWidthHalf * 2.2f) * (1.0f - 0.55f * progressTop)
                    val avgWidth = (widthBottom + widthTop) / 2f

                    if (layer.flavors.size <= 1) {
                        // Single flavor layer
                        val color = layer.flavors.firstOrNull()?.color ?: Color.White
                        drawOval(
                            color = color,
                            topLeft = Offset(midX - avgWidth / 2f, layerTopY),
                            size = Size(avgWidth, layerHeight * 1.15f)
                        )
                    } else {
                        // Dual / Multi-flavor swirl: draw interleaved slices/arcs
                        val numFlavors = layer.flavors.size
                        val arcSliceDeg = 360f / numFlavors

                        for (fIdx in 0 until numFlavors) {
                            val fColor = layer.flavors[fIdx].color
                            drawArc(
                                color = fColor,
                                startAngle = fIdx * arcSliceDeg + (index * 45f),
                                sweepAngle = arcSliceDeg,
                                useCenter = true,
                                topLeft = Offset(midX - avgWidth / 2f, layerTopY),
                                size = Size(avgWidth, layerHeight * 1.15f)
                            )
                        }
                    }

                    currentBaseY = layerTopY
                }

                // 3. Cherry on top if specified
                if (hasCherry) {
                    val cherryRadius = 5.5f
                    drawCircle(
                        color = Color(0xFFE51A2E),
                        radius = cherryRadius,
                        center = Offset(midX, currentBaseY - cherryRadius + 2f)
                    )
                    // Cherry stem
                    drawLine(
                        color = Color(0xFF683818),
                        start = Offset(midX, currentBaseY - cherryRadius + 2f),
                        end = Offset(midX + 4f, currentBaseY - cherryRadius - 7f),
                        strokeWidth = 1.5f
                    )
                }
            }
        }

        Text(
            text = "MATCH",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black
        )
    }
}
