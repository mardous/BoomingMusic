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

package com.mardous.booming.ui.component.compose.decoration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Modifier.animatedGradient(
    colors: List<Color>,
    animating: Boolean
): Modifier = if (colors.isNotEmpty()) {
    composed {
        val time = remember { Animatable(0f) }

        LaunchedEffect(animating) {
            if (animating) {
                time.animateTo(
                    targetValue = time.value + 100f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(120000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )
            } else {
                time.stop()
            }
        }

        this.blur(80.dp)
            .drawBehind {
                val t = time.value
                val baseColor = colors.first()

                drawRect(baseColor)

                val color1 = colors.getOrElse(1 % colors.size) { baseColor }
                val color2 = colors.getOrElse(2 % colors.size) { baseColor }
                val color3 = colors.getOrElse(3 % colors.size) { baseColor }

                val x1 = (0.5f + 0.35f * sin(t * 0.5f)).coerceIn(0f, 1f)
                val y1 = (0.5f + 0.35f * cos(t * 0.3f)).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color1.copy(alpha = 0.8f), Color.Transparent),
                        center = Offset(x1 * size.width, y1 * size.height),
                        radius = size.minDimension * 0.9f
                    ),
                    center = Offset(x1 * size.width, y1 * size.height),
                    radius = size.minDimension * 0.9f
                )

                val x2 = (0.5f + 0.4f * cos(t * 0.2f)).coerceIn(0f, 1f)
                val y2 = (0.5f + 0.4f * sin(t * 0.4f)).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color2.copy(alpha = 0.7f), Color.Transparent),
                        center = Offset(x2 * size.width, y2 * size.height),
                        radius = size.minDimension * 1.1f
                    ),
                    center = Offset(x2 * size.width, y2 * size.height),
                    radius = size.minDimension * 1.1f
                )

                val x3 = (0.5f + 0.3f * sin(t * 0.7f + 2f)).coerceIn(0f, 1f)
                val y3 = (0.5f + 0.3f * cos(t * 0.6f + 1f)).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color3.copy(alpha = 0.6f), Color.Transparent),
                        center = Offset(x3 * size.width, y3 * size.height),
                        radius = size.minDimension * 0.8f
                    ),
                    center = Offset(x3 * size.width, y3 * size.height),
                    radius = size.minDimension * 0.8f
                )
            }
    }
} else this
