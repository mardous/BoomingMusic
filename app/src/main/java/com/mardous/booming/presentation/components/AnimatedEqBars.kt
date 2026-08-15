/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
