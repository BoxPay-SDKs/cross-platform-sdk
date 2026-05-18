package com.crossplatform.sdk.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// --- Shimmer Effect Modifier ---

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        Color(0xFFE0E0E0),
        Color(0xFFF5F5F5),
        Color(0xFFE0E0E0),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim, 0f)
    )

    background(brush)
}

// --- Shimmer Item Data ---

data class ShimmerItem(val height: Dp, val marginTop: Dp, val cornerRadius: Dp)

private val shimmerItems = listOf(
    ShimmerItem(height = 90.dp, marginTop = 10.dp, cornerRadius = 0.dp),
    ShimmerItem(height = 50.dp, marginTop = 30.dp, cornerRadius = 10.dp),
    ShimmerItem(height = 50.dp, marginTop = 25.dp, cornerRadius = 10.dp),
    ShimmerItem(height = 50.dp, marginTop = 25.dp, cornerRadius = 10.dp),
    ShimmerItem(height = 50.dp, marginTop = 25.dp, cornerRadius = 10.dp),
)

// --- ShimmerView Composable ---

@Composable
fun ShimmerView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        shimmerItems.forEach { item ->
            Spacer(modifier = Modifier.height(item.marginTop))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(item.height)
                    .clip(RoundedCornerShape(item.cornerRadius))
                    .shimmerEffect()
            )
        }
    }
}