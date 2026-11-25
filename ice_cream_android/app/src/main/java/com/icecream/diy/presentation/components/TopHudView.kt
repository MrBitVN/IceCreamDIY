package com.icecream.diy.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icecream.diy.presentation.theme.StarFilled
import com.icecream.diy.presentation.theme.TextDark

@Composable
fun TopHudView(
    level: Int,
    coins: Int,
    stars: Int,
    onSettingsClick: () -> Unit,
    onRestartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Left column: Settings button & Coin pill
        Column(
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

            CoinPillView(coins = coins)
        }

        // Center: 3 Stars and LEVEL capsule
        Column(
            modifier = Modifier.align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StarIcon(isFilled = stars >= 1, angle = -8f, offsetY = 0)
                StarIcon(isFilled = stars >= 2, angle = 0f, offsetY = -4)
                StarIcon(isFilled = stars >= 3, angle = 8f, offsetY = 0)
            }

            val badgeBgColor = if (stars > 0) Color(0xFF76D828) else Color.White
            val badgeTextColor = if (stars > 0) Color.White else TextDark

            Box(
                modifier = Modifier
                    .shadow(3.dp, RoundedCornerShape(50))
                    .background(badgeBgColor, RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LEVEL $level",
                    color = badgeTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
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
}

@Composable
private fun StarIcon(
    isFilled: Boolean,
    angle: Float,
    offsetY: Int
) {
    Icon(
        imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.Star,
        contentDescription = null,
        tint = if (isFilled) StarFilled else Color.White,
        modifier = Modifier
            .size(30.dp)
            .offset(y = offsetY.dp)
            .rotate(angle)
    )
}
