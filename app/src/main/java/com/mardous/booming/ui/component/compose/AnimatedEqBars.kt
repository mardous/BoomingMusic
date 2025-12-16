package com.mardous.booming.ui.component.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun AnimatedEqBars(
    color: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 3,
    barWidth: Dp = 4.dp,
    gap: Dp = 3.dp,
    minHeightFraction: Float = 0.10f,
    maxHeightFraction: Float = 0.95f,
    basePeriodMillis: Int = 1800
) {
    val density = LocalDensity.current
    val barWidthPx = with(density) { barWidth.toPx() }
    val gapPx = with(density) { gap.toPx() }

    val transition = rememberInfiniteTransition(label = "eq")

    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(basePeriodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "time"
    )

    val activity by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "activity"
    )

    Canvas(modifier = modifier) {
        val h = size.height
        val totalWidth = barCount * barWidthPx + (barCount - 1) * gapPx

        var x = (size.width - totalWidth) / 2f

        repeat(barCount) { i ->
            val phase = t * (1f + i * 0.35f) + i * 0.8f
            val wave = (sin(phase) + 1f) * 0.5f
            val shaped = wave * wave

            val frac = minHeightFraction + (maxHeightFraction - minHeightFraction) *
                    shaped * activity + minHeightFraction * (1f - activity)

            val barH = h * frac
            val y = (h - barH) / 2f

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidthPx, barH),
                cornerRadius = CornerRadius(barWidthPx / 2f)
            )

            x += barWidthPx + gapPx
        }
    }
}
