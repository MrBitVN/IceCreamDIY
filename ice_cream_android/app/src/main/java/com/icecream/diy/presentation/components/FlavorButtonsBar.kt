package com.icecream.diy.presentation.components

import android.view.MotionEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icecream.diy.data.model.Flavor

@Composable
fun FlavorButtonsBar(
    flavors: List<Flavor>,
    activeFlavors: Set<Flavor>,
    showTutorial: Boolean = false,
    onFlavorPressed: (Flavor) -> Unit,
    onFlavorReleased: (Flavor) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        flavors.forEachIndexed { index, flavor ->
            Box(contentAlignment = Alignment.Center) {
                FlavorButtonItem(
                    flavor = flavor,
                    isPressed = activeFlavors.contains(flavor),
                    onPress = { onFlavorPressed(flavor) },
                    onRelease = { onFlavorReleased(flavor) }
                )

                // Tutorial hand pointing at first button on Level 1
                if (showTutorial && index == 0 && activeFlavors.isEmpty()) {
                    TutorialHandView(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 38.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FlavorButtonItem(
    flavor: Flavor,
    isPressed: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        label = "flavorScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .scale(scale)
                .shadow(6.dp, CircleShape)
                .background(Color.White, CircleShape)
                .padding(5.dp)
                .pointerInteropFilter { motionEvent ->
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> {
                            onPress()
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            onRelease()
                            true
                        }
                        else -> false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(flavor.color, CircleShape)
            )
        }

        Text(
            text = flavor.displayName,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
