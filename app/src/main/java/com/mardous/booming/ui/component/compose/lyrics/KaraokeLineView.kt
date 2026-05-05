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
import com.mardous.booming.data.model.lyrics.Lyrics
import com.mardous.booming.extensions.utilities.isRtl

@Composable
fun KaraokeLineView(
    shadowEffect: Boolean,
    position: Long,
    syllables: List<Lyrics.Word>,
    contentColor: Color,
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

        val isRightAligned = align == TextAlign.End || align == TextAlign.Right

        val finalLineLayouts = remember(wrappedLines, availableWidthPx, lineHeight, isRightAligned) {
            calculateStaticLineLayout(
                wrappedLines = wrappedLines,
                isLineRightAligned = isRightAligned,
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
                rowRenderData = rowRenderData,
                currentTimeMs = position,
                contentColor = contentColor,
                shadowEffect = shadowEffect,
                isRtl = isLineRtl
            )
        }
    }
}

private fun measureSyllables(
    syllables: List<Lyrics.Word>,
    textMeasurer: TextMeasurer,
    style: TextStyle
): List<SyllableLayout> {
    return syllables.map { word ->
        val layoutResult = textMeasurer.measure(word.content, style)
        SyllableLayout(
            word = word,
            textLayoutResult = layoutResult,
            width = layoutResult.size.width.toFloat(),
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
    isLineRightAligned: Boolean,
    canvasWidth: Float,
    lineHeight: Float
): List<List<SyllableLayout>> {
    return wrappedLines.mapIndexed { lineIndex, wrappedLine ->
        val maxBaselineInLine = wrappedLine.syllables.maxOfOrNull { it.firstBaseline } ?: 0f
        val rowTopY = lineIndex * lineHeight

        val startX = if (isLineRightAligned) {
            canvasWidth - wrappedLine.totalWidth
        } else {
            0f
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

        val verticalPadding = (totalHeight * 0.1f) * density
        val horizontalPadding = (totalWidth * 0.1f) * density

        RowRenderData(
            rowLayouts = rowLayouts,
            totalMinX = totalMinX,
            totalMaxX = totalMaxX,
            totalWidth = totalWidth,
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
    rowRenderData: List<RowRenderData>,
    currentTimeMs: Long,
    contentColor: Color,
    shadowEffect: Boolean,
    isRtl: Boolean
) {
    rowRenderData.forEach { rowData ->
        if (currentTimeMs >= rowData.lastWordEnd) {
            drawRowText(rowData.rowLayouts, contentColor, currentTimeMs, shadowEffect)
            return@forEach
        }

        drawIntoCanvas { canvas ->
            val layerBounds = rowData.layerBounds
            canvas.saveLayer(layerBounds, Paint())

            drawRowText(rowData.rowLayouts, contentColor, currentTimeMs, shadowEffect)

            val progressBrush = createLineGradientBrush(
                rowData = rowData,
                currentTimeMs = currentTimeMs,
                isRtl = isRtl,
                activeColor = Color.White,
                inactiveColor = Color.White.copy(alpha = 0.3f)
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
    drawColor: Color,
    currentTimeMs: Long,
    shadowEffect: Boolean
) {
    rowLayouts.forEach { syllableLayout ->
        val word = syllableLayout.word
        val duration = word.durationMillis

        val useAwesomeAnimation = duration >= 1000
        
        var offsetY = 0f
        var blurRadius = 4f

        if (useAwesomeAnimation &&
            currentTimeMs >= (word.startMillis - 200) && currentTimeMs <= (word.endMillis + 200)) {
            val progress = ((currentTimeMs - word.startMillis).toFloat() / duration).coerceIn(0f, 1f)

            offsetY = dipAndRiseCurve(progress) * 4f
            blurRadius = 4f + bounceCurve(progress) * 6f
        } else {
            val timeSinceStart = currentTimeMs - word.startMillis
            if (timeSinceStart in 0..700) {
                val animProgress = (1f - (timeSinceStart / 700f)).coerceIn(0f, 1f)
                offsetY = -animProgress * 4f
            }
        }

        val shadow = if (shadowEffect) Shadow(
            color = Color.Black.copy(alpha = 0.5f),
            offset = Offset(0f, 2f),
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

private fun dipAndRiseCurve(t: Float): Float = (4 * (t - 0.5f).let { it * it } - 1)
private fun bounceCurve(t: Float): Float = if (t < 0.7f) t / 0.7f else (1f - t) / 0.3f

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
    val word: Lyrics.Word,
    val textLayoutResult: TextLayoutResult,
    val width: Float,
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
    val firstWordStart: Long,
    val lastWordEnd: Long,
    val layerBounds: Rect
)
