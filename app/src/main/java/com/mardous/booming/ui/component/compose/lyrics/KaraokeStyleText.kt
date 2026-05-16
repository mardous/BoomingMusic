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

package com.mardous.booming.ui.component.compose.lyrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.mardous.booming.data.model.lyrics.SyncedLyrics
import com.mardous.booming.extensions.utilities.isRtl

@Composable
fun KaraokeLineView(
    selectedLine: Boolean,
    currentMillis: Long,
    syllables: List<SyncedLyrics.Word>,
    contentColor: Color,
    shadowEffect: Boolean,
    style: TextStyle,
    align: TextAlign,
    modifier: Modifier = Modifier
) {
    val isLineRtl = remember(syllables) { syllables.any { it.content.isRtl() } }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidthPx = with(density) { maxWidth.toPx() }

        val initialLayouts = remember(syllables, style) {
            measureSyllables(syllables, textMeasurer, style)
        }

        val wrappedLines = remember(initialLayouts, availableWidthPx) {
            calculateGreedyWrappedLines(initialLayouts, availableWidthPx)
        }

        val lineHeight = remember(style) {
            textMeasurer.measure("M", style).size.height.toFloat()
        }

        val finalLineLayouts = remember(wrappedLines, availableWidthPx, lineHeight, align) {
            calculateStaticLineLayout(
                wrappedLines = wrappedLines,
                align = align,
                canvasWidth = availableWidthPx,
                lineHeight = lineHeight
            )
        }

        val rowRenderData = remember(finalLineLayouts, density) {
            calculateRowRenderData(finalLineLayouts, density.density)
        }

        val totalHeight = remember(wrappedLines, lineHeight) {
            lineHeight * wrappedLines.size
        }

        Canvas(modifier = Modifier.size(maxWidth, (totalHeight.toInt() + 8).toDp())) {
            drawLyricsLine(
                selectedLine = selectedLine,
                rowRenderData = rowRenderData,
                currentTimeMs = currentMillis,
                contentColor = contentColor,
                shadowEffect = shadowEffect,
                isRtl = isLineRtl
            )
        }
    }
}

private fun measureSyllables(
    syllables: List<SyncedLyrics.Word>,
    textMeasurer: TextMeasurer,
    style: TextStyle
): List<SyllableLayout> {
    return syllables.map { word ->
        val layoutResult = textMeasurer.measure(word.content, style)
        val visualLayoutResult = textMeasurer.measure(word.content.trimEnd(), style)
        SyllableLayout(
            word = word,
            textLayoutResult = layoutResult,
            width = layoutResult.size.width.toFloat(),
            visualWidth = visualLayoutResult.size.width.toFloat(),
            firstBaseline = layoutResult.firstBaseline
        )
    }
}

private fun calculateGreedyWrappedLines(
    syllableLayouts: List<SyllableLayout>,
    availableWidthPx: Float
): List<WrappedLine> {
    val lines = mutableListOf<WrappedLine>()
    val currentLine = mutableListOf<SyllableLayout>()
    var currentLineWidth = 0f

    syllableLayouts.forEach { layout ->
        if (currentLineWidth + layout.width <= availableWidthPx) {
            currentLine.add(layout)
            currentLineWidth += layout.width
        } else {
            if (currentLine.isNotEmpty()) {
                lines.add(WrappedLine(currentLine.toList(), currentLineWidth))
                currentLine.clear()
                currentLineWidth = 0f
            }
            currentLine.add(layout)
            currentLineWidth += layout.width
        }
    }

    if (currentLine.isNotEmpty()) {
        lines.add(WrappedLine(currentLine.toList(), currentLineWidth))
    }
    return lines
}

private fun calculateStaticLineLayout(
    wrappedLines: List<WrappedLine>,
    align: TextAlign,
    canvasWidth: Float,
    lineHeight: Float
): List<List<SyllableLayout>> {
    return wrappedLines.mapIndexed { lineIndex, wrappedLine ->
        val maxBaselineInLine = wrappedLine.syllables.maxOfOrNull { it.firstBaseline } ?: 0f
        val rowTopY = lineIndex * lineHeight

        val lastSyllable = wrappedLine.syllables.lastOrNull()
        val trailingSpaceWidth = lastSyllable?.let { it.width - it.visualWidth } ?: 0f
        val visualLineWidth = wrappedLine.totalWidth - trailingSpaceWidth

        val startX = when (align) {
            TextAlign.End, TextAlign.Right -> canvasWidth - visualLineWidth
            TextAlign.Center -> (canvasWidth - visualLineWidth) / 2f
            else -> 0f
        }

        var currentX = startX

        wrappedLine.syllables.map { initialLayout ->
            val positionX = currentX
            val verticalOffset = maxBaselineInLine - initialLayout.firstBaseline
            val positionY = rowTopY + verticalOffset
            val positionedLayout = initialLayout.copy(position = Offset(positionX, positionY))
            currentX += positionedLayout.width
            positionedLayout
        }
    }
}

private fun calculateRowRenderData(
    lineLayouts: List<List<SyllableLayout>>,
    density: Float
): List<RowRenderData> {
    return lineLayouts.mapNotNull { rowLayouts ->
        if (rowLayouts.isEmpty()) return@mapNotNull null

        val totalMinX = rowLayouts.minOf { it.position.x }
        val totalMaxX = rowLayouts.maxOf { it.position.x + it.width }
        val totalWidth = totalMaxX - totalMinX
        val minY = rowLayouts.minOf { it.position.y }
        val totalHeight = rowLayouts.maxOf { it.textLayoutResult.size.height }.toFloat()

        val verticalPadding = 12f * density
        val horizontalPadding = 8f * density

        RowRenderData(
            rowLayouts = rowLayouts,
            totalMinX = totalMinX,
            totalMaxX = totalMaxX,
            totalWidth = totalWidth,
            totalHeight = totalHeight,
            firstWordStart = rowLayouts.first().word.startMillis,
            lastWordEnd = rowLayouts.last().word.endMillis,
            layerBounds = Rect(
                left = totalMinX - horizontalPadding,
                top = minY - verticalPadding,
                right = totalMaxX + horizontalPadding,
                bottom = minY + totalHeight + verticalPadding
            )
        )
    }
}

private fun DrawScope.drawLyricsLine(
    selectedLine: Boolean,
    rowRenderData: List<RowRenderData>,
    currentTimeMs: Long,
    contentColor: Color,
    shadowEffect: Boolean,
    isRtl: Boolean
) {
    rowRenderData.forEach { rowData ->
        if (!selectedLine) {
            drawRowText(
                rowLayouts = rowData.rowLayouts,
                rowHeight = rowData.totalHeight,
                drawColor = contentColor.copy(alpha = .4f),
                currentTimeMs = currentTimeMs,
                shadowEffect = false
            )
            return@forEach
        }

        drawIntoCanvas { canvas ->
            val layerBounds = rowData.layerBounds
            canvas.saveLayer(layerBounds, Paint())

            drawRowText(
                rowLayouts = rowData.rowLayouts,
                rowHeight = rowData.totalHeight,
                drawColor = contentColor,
                currentTimeMs = currentTimeMs,
                shadowEffect = shadowEffect
            )

            val progressBrush = createLineGradientBrush(
                rowData = rowData,
                currentTimeMs = currentTimeMs,
                isRtl = isRtl,
                activeColor = contentColor,
                inactiveColor = contentColor.copy(alpha = 0.4f)
            )

            drawRect(
                brush = progressBrush,
                topLeft = layerBounds.topLeft,
                size = layerBounds.size,
                blendMode = BlendMode.DstIn
            )
            canvas.restore()
        }
    }
}

private fun DrawScope.drawRowText(
    rowLayouts: List<SyllableLayout>,
    rowHeight: Float,
    drawColor: Color,
    currentTimeMs: Long,
    shadowEffect: Boolean
) {
    rowLayouts.forEach { syllableLayout ->
        val word = syllableLayout.word

        val animationDuration = word.durationMillis.coerceIn(400L, 1000L).toFloat()

        val startTime = word.startMillis
        val timeSinceStart = (currentTimeMs - startTime).toFloat()

        val maxElevation = (rowHeight * 0.02f) * density

        val progress = (timeSinceStart / animationDuration).coerceIn(0f, 1f)
        val smoothProgress = progress * progress * (3 - 2 * progress)
        val offsetY = smoothProgress * -maxElevation
        val blurRadius = (4f + smoothProgress * 6f) * density

        val shadow = if (shadowEffect) Shadow(
            color = drawColor.copy(alpha = 0.4f),
            offset = Offset(0f, 2f * density),
            blurRadius = blurRadius
        ) else null

        drawText(
            textLayoutResult = syllableLayout.textLayoutResult,
            color = drawColor,
            topLeft = syllableLayout.position.copy(y = syllableLayout.position.y + offsetY),
            shadow = shadow
        )
    }
}

private fun createLineGradientBrush(
    rowData: RowRenderData,
    currentTimeMs: Long,
    isRtl: Boolean,
    activeColor: Color,
    inactiveColor: Color
): Brush {
    val minFadeWidth = 20f

    val lineLayout = rowData.rowLayouts
    val totalMinX = rowData.totalMinX
    val totalMaxX = rowData.totalMaxX
    val totalWidth = rowData.totalWidth

    if (totalWidth <= 0f) {
        val isFinished = currentTimeMs >= lineLayout.last().word.endMillis
        return SolidColor(if (isFinished) activeColor else inactiveColor)
    }

    val lineProgress = run {
        if (currentTimeMs <= rowData.firstWordStart) return@run 0f
        if (currentTimeMs >= rowData.lastWordEnd) return@run 1f

        val activeWordLayout = lineLayout.find {
            currentTimeMs in it.word.startMillis until it.word.endMillis
        }

        val currentPixelPosition = when {
            activeWordLayout != null -> {
                val duration = activeWordLayout.word.durationMillis
                val wordProgress = if (duration > 0) {
                    ((currentTimeMs - activeWordLayout.word.startMillis).toFloat() / duration).coerceIn(0f, 1f)
                } else 1f
                
                if (isRtl) {
                    activeWordLayout.position.x + activeWordLayout.width * (1f - wordProgress)
                } else {
                    activeWordLayout.position.x + activeWordLayout.width * wordProgress
                }
            }
            else -> {
                val lastFinished = lineLayout.lastOrNull { currentTimeMs >= it.word.endMillis }
                if (isRtl) {
                    lastFinished?.position?.x ?: totalMaxX
                } else {
                    lastFinished?.let { it.position.x + it.width } ?: totalMinX
                }
            }
        }
        ((currentPixelPosition - totalMinX) / totalWidth).coerceIn(0f, 1f)
    }

    val fadeRange = (minFadeWidth / totalWidth).coerceAtMost(1f)
    val fadeCenterStart = -fadeRange / 2f
    val fadeCenterEnd = 1f + fadeRange / 2f
    val fadeCenter = fadeCenterStart + (fadeCenterEnd - fadeCenterStart) * lineProgress
    val fadeStart = fadeCenter - fadeRange / 2f
    val fadeEnd = fadeCenter + fadeRange / 2f

    val colorStops = if (isRtl) {
        arrayOf(
            0.0f to inactiveColor,
            fadeStart.coerceIn(0f, 1f) to inactiveColor,
            fadeEnd.coerceIn(0f, 1f) to activeColor,
            1.0f to activeColor
        )
    } else {
        arrayOf(
            0.0f to activeColor,
            fadeStart.coerceIn(0f, 1f) to activeColor,
            fadeEnd.coerceIn(0f, 1f) to inactiveColor,
            1.0f to inactiveColor
        )
    }

    return Brush.horizontalGradient(
        colorStops = colorStops,
        startX = totalMinX,
        endX = totalMaxX
    )
}

@Composable
private fun Int.toDp(): Dp = with(LocalDensity.current) { this@toDp.toDp() }

@Stable
internal data class SyllableLayout(
    val word: SyncedLyrics.Word,
    val textLayoutResult: TextLayoutResult,
    val width: Float,
    val visualWidth: Float,
    val position: Offset = Offset.Zero,
    val firstBaseline: Float = 0f
)

@Stable
internal data class WrappedLine(
    val syllables: List<SyllableLayout>,
    val totalWidth: Float
)

@Stable
internal data class RowRenderData(
    val rowLayouts: List<SyllableLayout>,
    val totalMinX: Float,
    val totalMaxX: Float,
    val totalWidth: Float,
    val totalHeight: Float,
    val firstWordStart: Long,
    val lastWordEnd: Long,
    val layerBounds: Rect
)
