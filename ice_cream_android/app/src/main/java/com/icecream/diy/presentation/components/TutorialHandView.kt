package com.icecream.diy.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun TutorialHandView(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "handBounce")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "handOffsetY"
    )

    Box(
        modifier = modifier
            .size(48.dp, 56.dp)
            .offset(y = offsetY.dp)
    ) {
        Canvas(modifier = Modifier.size(48.dp, 56.dp)) {
            val yellow = Color(0xFFFFCC00)
            val strokeColor = Color(0xFF333333)

            // Index finger pointing up
            val fingerPath = Path().apply {
                moveTo(20f, 32f)
                lineTo(20f, 6f)
                // Finger rounded tip
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(14f, 0f, 26f, 12f),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = -180f,
                    forceMoveTo = false
                )
                lineTo(14f, 32f)
                close()
            }
            drawPath(fingerPath, color = yellow)
            drawPath(fingerPath, color = strokeColor, style = Stroke(width = 2.5f))

            // Palm and other folded fingers
            val palmPath = Path().apply {
                moveTo(14f, 28f)
                lineTo(38f, 28f)
                // Curved right palm edge
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(26f, 28f, 42f, 52f),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(16f, 52f)
                // Curved bottom left wrist
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(10f, 40f, 24f, 54f),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                close()
            }
            drawPath(palmPath, color = yellow)
            drawPath(palmPath, color = strokeColor, style = Stroke(width = 2.5f))

            // Folded finger knuckles lines
            drawLine(
                color = strokeColor,
                start = Offset(26f, 34f),
                end = Offset(36f, 34f),
                strokeWidth = 2f
            )
            drawLine(
                color = strokeColor,
                start = Offset(26f, 41f),
                end = Offset(35f, 41f),
                strokeWidth = 2f
            )
        }
    }
}
