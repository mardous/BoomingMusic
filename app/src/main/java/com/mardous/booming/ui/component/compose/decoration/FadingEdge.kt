package com.mardous.booming.ui.component.compose.decoration

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Immutable
data class FadingEdges(
    val start: Dp = 0.dp,
    val top: Dp = 0.dp,
    val end: Dp = 0.dp,
    val bottom: Dp = 0.dp,
) {
    companion object {
        val None = FadingEdges()
    }
}

fun Modifier.fadingEdges(
    edges: FadingEdges,
    gradientStartAlpha: Float = 1f,
    gradientEndAlpha: Float = 0f,
): Modifier = if (edges != FadingEdges.None) {
    this.fadingEdges(
        startEdgeLength = edges.start,
        topEdgeLength = edges.top,
        endEdgeLength = edges.end,
        bottomEdgeLength = edges.bottom,
        gradientStartAlpha = gradientStartAlpha,
        gradientEndAlpha = gradientEndAlpha,
    )
} else {
    this
}

fun Modifier.fadingEdges(
    startEdgeLength: Dp,
    topEdgeLength: Dp,
    endEdgeLength: Dp,
    bottomEdgeLength: Dp,
    gradientStartAlpha: Float = 1f,
    gradientEndAlpha: Float = 0f,
): Modifier = composed {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val density = LocalDensity.current
    val startEdgeLengthPx = with(density) { startEdgeLength.toPx() }
    val topEdgeLengthPx = with(density) { topEdgeLength.toPx() }
    val endEdgeLengthPx = with(density) { endEdgeLength.toPx() }
    val bottomEdgeLengthPx = with(density) { bottomEdgeLength.toPx() }

    graphicsLayer { alpha = 0.99f }
        .drawWithCache {
            // Transparent to black
            val colorsT2B = listOf(
                Color.Black.copy(alpha = gradientEndAlpha),
                Color.Black.copy(alpha = gradientStartAlpha),
            )

            // Black to transparent
            val colorsB2T = colorsT2B.reversed()

            val width = size.width
            val height = size.height
            val startBrush = if (isRtl) {
                Brush.horizontalGradient(
                    colors = colorsB2T,
                    startX = width - startEdgeLengthPx,
                    endX = width
                )
            } else {
                Brush.horizontalGradient(
                    colors = colorsT2B,
                    startX = 0f,
                    endX = startEdgeLengthPx
                )
            }
            val endBrush = if (isRtl) {
                Brush.horizontalGradient(
                    colors = colorsT2B,
                    startX = 0f,
                    endX = endEdgeLengthPx
                )
            } else {
                Brush.horizontalGradient(
                    colors = colorsB2T,
                    startX = width - endEdgeLengthPx,
                    endX = width
                )
            }
            val topBrush = Brush.verticalGradient(
                colors = colorsT2B,
                startY = 0f,
                endY = topEdgeLengthPx
            )
            val bottomBrush = Brush.verticalGradient(
                colors = colorsB2T,
                startY = height - bottomEdgeLengthPx,
                endY = height
            )
            onDrawWithContent {
                drawContent()
                // Start edge
                if (startEdgeLengthPx > 0) {
                    val size = Size(startEdgeLengthPx, height)
                    if (isRtl) {
                        drawRect(
                            startBrush,
                            topLeft = Offset(0f, width - startEdgeLengthPx),
                            size = size,
                            blendMode = BlendMode.DstIn
                        )
                    } else {
                        drawRect(
                            startBrush,
                            topLeft = Offset(0f, 0f),
                            size = size,
                            blendMode = BlendMode.DstIn
                        )
                    }
                }

                // End edge
                if (endEdgeLengthPx > 0) {
                    val size = Size(endEdgeLengthPx, height)
                    if (isRtl) {
                        drawRect(
                            brush = endBrush,
                            topLeft = Offset(0f, 0f),
                            size = size,
                            blendMode = BlendMode.DstIn
                        )
                    } else {
                        drawRect(
                            brush = endBrush,
                            topLeft = Offset(width - endEdgeLengthPx, 0f),
                            size = size,
                            blendMode = BlendMode.DstIn,
                        )
                    }
                }

                // Top edge
                if (topEdgeLengthPx > 0) {
                    drawRect(
                        brush = topBrush,
                        topLeft = Offset(0f, 0f),
                        size = Size(width, topEdgeLengthPx),
                        blendMode = BlendMode.DstIn
                    )
                }

                // Bottom edge
                if (bottomEdgeLengthPx > 0) {
                    drawRect(
                        brush = bottomBrush,
                        topLeft = Offset(0f, height - bottomEdgeLengthPx),
                        size = Size(width, bottomEdgeLengthPx),
                        blendMode = BlendMode.DstIn
                    )
                }
            }
        }
}
