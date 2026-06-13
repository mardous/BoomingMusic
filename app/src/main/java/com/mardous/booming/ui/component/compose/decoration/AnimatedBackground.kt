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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.isActive

@Composable
fun Modifier.animatedGradient(
    colors: List<Color>,
    animating: Boolean
): Modifier = composed {
    val time = remember { Animatable(0f) }

    LaunchedEffect(animating) {
        if (animating) {
            val period = (2 * Math.PI * 10).toFloat() // 20 * PI
            val speed = period / 120000.0 // units per millisecond
            val startTime = System.currentTimeMillis()
            val startVal = time.value
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                val currentVal = (startVal + elapsed * speed) % period
                time.snapTo(currentVal.toFloat())
                withFrameMillis { }
            }
        }
    }

    val safeSize = colors.size.coerceAtLeast(1)
    val rawBase = colors.getOrNull(0) ?: Color.Transparent

    val targetBase = remember(colors, rawBase) {
        if (rawBase != Color.Transparent) adjustColorForBackground(rawBase) else rawBase
    }
    val target1 = remember(colors, safeSize, targetBase) {
        val c = colors.getOrElse(1 % safeSize) { targetBase }
        if (c != Color.Transparent) adjustColorForBackground(c) else c
    }
    val target2 = remember(colors, safeSize, targetBase) {
        val c = colors.getOrElse(2 % safeSize) { targetBase }
        if (c != Color.Transparent) adjustColorForBackground(c) else c
    }
    val target3 = remember(colors, safeSize, targetBase) {
        val c = colors.getOrElse(3 % safeSize) { targetBase }
        if (c != Color.Transparent) adjustColorForBackground(c) else c
    }

    val baseColor by animateColorAsState(targetBase, tween(1500), label = "baseColor")
    val color1 by animateColorAsState(target1, tween(1500), label = "color1")
    val color2 by animateColorAsState(target2, tween(1500), label = "color2")
    val color3 by animateColorAsState(target3, tween(1500), label = "color3")

    this.drawBehind {
        if (baseColor == Color.Transparent && color1 == Color.Transparent && 
            color2 == Color.Transparent && color3 == Color.Transparent) return@drawBehind

        val t = time.value
        drawRect(baseColor)

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

private fun adjustColorForBackground(color: Color): Color {
    val r = color.red
    val g = color.green
    val b = color.blue

    val max = maxOf(r, maxOf(g, b))
    val min = minOf(r, minOf(g, b))
    val delta = max - min

    var h = 0f
    var s = 0f
    val l = (max + min) / 2f

    if (max != min) {
        s = if (l < 0.5f) delta / (max + min) else delta / (2f - max - min)
        h = when (max) {
            r -> (g - b) / delta + (if (g < b) 6f else 0f)
            g -> (b - r) / delta + 2f
            else -> (r - g) / delta + 4f
        }
        h *= 60f
    }

    // Clamp saturation to avoid oversaturation (like harsh purple)
    val targetS = s.coerceIn(0.15f, 0.45f)

    // Adjust lightness to be in a pleasant visible-but-dark range
    // Under dark mode, we want a soft glow, so lightness around 0.18f to 0.28f is ideal.
    val targetL = l.coerceIn(0.18f, 0.28f)

    return hslToColor(h, targetS, targetL, color.alpha)
}

private fun hslToColor(h: Float, s: Float, l: Float, a: Float): Color {
    if (s == 0f) {
        return Color(red = l, green = l, blue = l, alpha = a)
    }

    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q

    val hueNormalized = h / 360f
    val r = hueToRgbComponent(p, q, hueNormalized + 1f / 3f)
    val g = hueToRgbComponent(p, q, hueNormalized)
    val b = hueToRgbComponent(p, q, hueNormalized - 1f / 3f)

    return Color(red = r, green = g, blue = b, alpha = a)
}

private fun hueToRgbComponent(p: Float, q: Float, t: Float): Float {
    var tempT = t
    if (tempT < 0f) tempT += 1f
    if (tempT > 1f) tempT -= 1f
    return when {
        tempT < 1f / 6f -> p + (q - p) * 6f * tempT
        tempT < 1f / 2f -> q
        tempT < 2f / 3f -> p + (q - p) * (2f / 3f - tempT) * 6f
        else -> p
    }
}
