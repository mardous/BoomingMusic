package com.mardous.booming.ui.component.compose.decoration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.sqrt

// Based on: https://gist.github.com/KlassenKonstantin/d5f6ed1d74b3ddbdca699d66c6b9a3b2
@Composable
fun Modifier.animatedGradient(
    colors: List<Color>,
    animating: Boolean
): Modifier = if (colors.isNotEmpty()) {
    composed {
        val rotation = remember { Animatable(0f) }

        LaunchedEffect(rotation, animating) {
            if (!animating) return@LaunchedEffect
            val target = rotation.value + 360f
            rotation.animateTo(
                targetValue = target,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 30_000,
                        easing = LinearEasing,
                    ),
                ),
            )
        }

        drawWithCache {
            val rectSize = sqrt(size.width * size.width + size.height * size.height)
            val topLeft = Offset(
                x = -(rectSize - size.width) / 2,
                y = -(rectSize - size.height) / 2,
            )

            val brush1 = Brush.linearGradient(
                0f to colors[0],
                1f to colors[1],
                start = topLeft,
                end = Offset(rectSize * 0.7f, rectSize * 0.7f),
            )

            val brush2 = Brush.linearGradient(
                0f to Color(0xFF1E1D1D),
                1f to Color(0xFF8D938F),
                start = Offset(rectSize, 0f),
                end = Offset(0f, rectSize),
            )

            val maskBrush = Brush.linearGradient(
                0f to Color.White,
                1f to Color.Transparent,
                start = Offset(rectSize / 2f, 0f),
                end = Offset(rectSize / 2f, rectSize),
            )

            onDrawBehind {
                val value = rotation.value

                withTransform(transformBlock = { rotate(value) }) {
                    drawRect(
                        brush = brush1,
                        topLeft = topLeft,
                        size = Size(rectSize, rectSize),
                    )
                }

                withTransform(transformBlock = { rotate(-value) }) {
                    drawRect(
                        brush = maskBrush,
                        topLeft = topLeft,
                        size = Size(rectSize, rectSize),
                        blendMode = BlendMode.DstOut,
                    )
                }

                withTransform(transformBlock = { rotate(value) }) {
                    drawRect(
                        brush = brush2,
                        topLeft = topLeft,
                        size = Size(rectSize, rectSize),
                        blendMode = BlendMode.DstAtop,
                    )
                }
            }
        }
    }
} else this