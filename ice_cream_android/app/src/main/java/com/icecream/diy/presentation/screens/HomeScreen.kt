package com.icecream.diy.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icecream.diy.presentation.components.CoinPillView
import com.icecream.diy.presentation.theme.BgGradientBottom
import com.icecream.diy.presentation.theme.BgGradientTop
import com.icecream.diy.presentation.theme.TextDark
import com.icecream.diy.scene3d.HomeCustomerSceneView

@Composable
fun HomeScreen(
    level: Int,
    coins: Int,
    onPlayClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BgGradientTop, BgGradientBottom)
                )
            )
    ) {
        // Top status row: Coin pill on left, Level badge in center/right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoinPillView(coins = coins)

            Box(
                modifier = Modifier
                    .shadow(3.dp, RoundedCornerShape(50))
                    .background(Color.White, RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "LEVEL $level",
                    color = TextDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Center Area: Procedural Waving Customer 3D & Title
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ICE CREAM DIY",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3D Procedural Waving Customer Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                HomeCustomerSceneView(
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Big Green Glossy START Button
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(72.dp)
                    .shadow(12.dp, RoundedCornerShape(36.dp))
                    .clip(RoundedCornerShape(36.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF8CE838), Color(0xFF56B816))
                        )
                    )
                    .border(3.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(36.dp))
                    .clickable { onPlayClick() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Start Game",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        // Right side icon column: VIP, Shop, Spin (Placeholders)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HomeSideIconButton(
                icon = Icons.Filled.Star,
                label = "VIP",
                bgColor = Color(0xFFFFB300),
                onClick = {
                    Toast.makeText(context, "VIP Club coming soon!", Toast.LENGTH_SHORT).show()
                }
            )

            HomeSideIconButton(
                icon = Icons.Filled.ShoppingCart,
                label = "Shop",
                bgColor = Color(0xFF42A5F5),
                onClick = {
                    Toast.makeText(context, "Shop coming soon!", Toast.LENGTH_SHORT).show()
                }
            )

            HomeSideIconButton(
                icon = Icons.Filled.CardGiftcard,
                label = "Spin",
                bgColor = Color(0xFFAB47BC),
                onClick = {
                    Toast.makeText(context, "Lucky Spin coming soon!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun HomeSideIconButton(
    icon: ImageVector,
    label: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(bgColor)
                .border(2.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
