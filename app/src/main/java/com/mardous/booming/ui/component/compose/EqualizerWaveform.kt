package com.mardous.booming.ui.component.compose

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun EqualizerWaveform(
    bands: List<Float>,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = color.copy(alpha = 0.2f),
    strokeWidth: Float = 3f,
    smoothingTension: Float = 0.2f
) {
    if (bands.isEmpty()) return

    val contentAlpha = if (enabled) 1f else 0.5f
    val actualColor = color.copy(alpha = color.alpha * contentAlpha)
    val actualFillColor = fillColor.copy(alpha = fillColor.alpha * contentAlpha)

    val minGain = valueRange.start
    val maxGain = valueRange.endInclusive
    val rangeSize = maxGain - minGain

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Draw baseline (0dB)
        if (0f in valueRange) {
            val baselineY = height - ((0f - minGain) / rangeSize) * height
            drawLine(
                color = actualColor.copy(alpha = 0.1f),
                start = Offset(0f, baselineY),
                end = Offset(width, baselineY),
                strokeWidth = 1f
            )
        }

        val points = bands.mapIndexed { index, bandValue ->
            val x = if (bands.size > 1) {
                (index.toFloat() / (bands.size - 1)) * width
            } else {
                width / 2f
            }
            val y = height - ((bandValue - minGain) / rangeSize) * height
            Offset(x, y)
        }

        if (points.size >= 2) {
            val strokePath = Path().apply {
                moveTo(points[0].x, points[0].y)
                
                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]

                    // Calculate control points based on neighbors
                    val prev = if (i > 0) points[i - 1] else p0
                    val next = if (i < points.size - 2) points[i + 2] else p1
                    
                    // Control Point 1 (near p0)
                    val cp1x = p0.x + (p1.x - prev.x) * smoothingTension
                    val cp1y = p0.y + (p1.y - prev.y) * smoothingTension
                    
                    // Control Point 2 (near p1)
                    val cp2x = p1.x - (next.x - p0.x) * smoothingTension
                    val cp2y = p1.y - (next.y - p0.y) * smoothingTension
                    
                    cubicTo(cp1x, cp1y, cp2x, cp2y, p1.x, p1.y)
                }
            }

            val fillPath = Path().apply {
                addPath(strokePath)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(actualFillColor, Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            drawPath(
                path = strokePath,
                color = actualColor,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            
            // Draw band points
            points.forEach { point ->
                drawCircle(
                    color = actualColor,
                    radius = strokeWidth * 1.5f,
                    center = point
                )
            }
        }
    }
}
