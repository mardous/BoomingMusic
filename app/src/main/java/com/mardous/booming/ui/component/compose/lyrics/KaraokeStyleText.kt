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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
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

private val LayerPaint = Paint()

@Composable
fun KaraokeLineView(
    selectedLine: Boolean,
    shadowEffect: Boolean,
    currentMillis: Long,
    syllables: List<SyncedLyrics.Word>,
    contentColor: Color,
    style: TextStyle,
    align: TextAlign,
    modifier: Modifier = Modifier
) {
    val isRtlContent = remember(syllables) { syllables.any { it.content.isRtl() } }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidthPx = with(density) { maxWidth.toPx() }

        val spaceWidth = remember(textMeasurer, style) {
            textMeasurer.measure(" ", style).size.width.toFloat()
        }

        val initialLayouts = remember(syllables, style, spaceWidth) {
            measureSyllablesAndDetermineAnimation(
                syllables = syllables,
                textMeasurer = textMeasurer,
                style = style,
                spaceWidth = spaceWidth
            )
        }

        val wrappedLines = remember(initialLayouts, availableWidthPx) {
            calculateGreedyWrappedLines(
                syllableLayouts = initialLayouts,
                availableWidthPx = availableWidthPx,
                textMeasurer = textMeasurer,
                style = style
            )
        }

        val lineHeight = remember(style) {
            textMeasurer.measure("M", style).size.height.toFloat()
        }

        val finalLineLayouts = remember(wrappedLines, availableWidthPx, lineHeight, align, isRtlContent) {
            calculateStaticLineLayout(
                wrappedLines = wrappedLines,
                align = align,
                canvasWidth = availableWidthPx,
                lineHeight = lineHeight,
                isRtl = isRtlContent
            )
        }

        val rowRenderData = remember(finalLineLayouts, density) {
            calculateRowRenderData(finalLineLayouts, density.density)
        }

        val totalHeight = remember(wrappedLines, lineHeight) {
            lineHeight * wrappedLines.size
        }

        val lineEnd = rowRenderData.last().lastWordEnd
        val lineHasAlreadyPassed = currentMillis >= lineEnd && !selectedLine
        val animatedAlpha by animateFloatAsState(
            targetValue = if (lineHasAlreadyPassed) .4f else 1f,
            animationSpec = tween(400)
        )

        Canvas(modifier = Modifier.size(maxWidth, (totalHeight.toInt() + 8).toDp())) {
            drawLyricsLine(
                selectedLine = selectedLine,
                lineHasAlreadyPassed = lineHasAlreadyPassed,
                rowRenderData = rowRenderData,
                currentTimeMs = currentMillis,
                contentColor = contentColor,
                animatedAlpha = animatedAlpha,
                shadowEffect = shadowEffect,
                rtlContent = isRtlContent
            )
        }
    }
}

private fun groupIntoWords(syllables: List<SyncedLyrics.Word>): List<List<SyncedLyrics.Word>> {
    if (syllables.isEmpty()) return emptyList()
    val words = mutableListOf<List<SyncedLyrics.Word>>()
    var currentWord = mutableListOf<SyncedLyrics.Word>()
    syllables.forEach { syllable ->
        currentWord.add(syllable)
        if (syllable.content.trimEnd().length < syllable.content.length) {
            words.add(currentWord.toList())
            currentWord = mutableListOf()
        }
    }
    if (currentWord.isNotEmpty()) {
        words.add(currentWord.toList())
    }
    return words
}

private fun measureSyllablesAndDetermineAnimation(
    syllables: List<SyncedLyrics.Word>,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    spaceWidth: Float
): List<SyllableLayout> {
    val words = groupIntoWords(syllables)

    return words.flatMapIndexed { wordIndex, word ->
        word.map { syllable ->
            val layoutResult = textMeasurer.measure(syllable.content, style)

            var layoutWidth = layoutResult.size.width.toFloat()
            if (syllable.content.endsWith(" ")) {
                val trimmedWidth = textMeasurer.measure(syllable.content.trimEnd(), style).size.width.toFloat()
                if (layoutWidth <= trimmedWidth) {
                    val spaceCount = syllable.content.length - syllable.content.trimEnd().length
                    layoutWidth = trimmedWidth + (spaceWidth * spaceCount)
                }
            }

            SyllableLayout(
                word = syllable,
                textLayoutResult = layoutResult,
                wordId = wordIndex,
                width = layoutWidth,
                firstBaseline = layoutResult.firstBaseline
            )
        }
    }
}

private fun calculateGreedyWrappedLines(
    syllableLayouts: List<SyllableLayout>,
    availableWidthPx: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle
): List<WrappedLine> {
    val lines = mutableListOf<WrappedLine>()
    val currentLine = mutableListOf<SyllableLayout>()
    var currentLineWidth = 0f

    val wordGroups = mutableListOf<List<SyllableLayout>>()
    if (syllableLayouts.isNotEmpty()) {
        var currentWordGroup = mutableListOf<SyllableLayout>()
        var currentWordId = syllableLayouts.first().wordId

        syllableLayouts.forEach { layout ->
            if (layout.wordId != currentWordId) {
                wordGroups.add(currentWordGroup)
                currentWordGroup = mutableListOf()
                currentWordId = layout.wordId
            }
            currentWordGroup.add(layout)
        }
        wordGroups.add(currentWordGroup)
    }

    wordGroups.forEach { wordSyllables ->
        val wordWidth = wordSyllables.sumOf { it.width.toDouble() }.toFloat()

        if (currentLineWidth + wordWidth <= availableWidthPx) {
            currentLine.addAll(wordSyllables)
            currentLineWidth += wordWidth
        } else {
            if (currentLine.isNotEmpty()) {
                val trimmedDisplayLine = trimDisplayLineTrailingSpaces(currentLine, textMeasurer, style)
                if (trimmedDisplayLine.syllables.isNotEmpty()) {
                    lines.add(trimmedDisplayLine)
                }
                currentLine.clear()
                currentLineWidth = 0f
            }

            if (wordWidth <= availableWidthPx) {
                currentLine.addAll(wordSyllables)
                currentLineWidth += wordWidth
            } else {
                wordSyllables.forEach { syllable ->
                    if (currentLineWidth + syllable.width > availableWidthPx && currentLine.isNotEmpty()) {
                        val trimmedLine = trimDisplayLineTrailingSpaces(currentLine, textMeasurer, style)
                        if (trimmedLine.syllables.isNotEmpty()) lines.add(trimmedLine)
                        currentLine.clear()
                        currentLineWidth = 0f
                    }
                    currentLine.add(syllable)
                    currentLineWidth += syllable.width
                }
            }
        }
    }

    if (currentLine.isNotEmpty()) {
        val trimmedDisplayLine = trimDisplayLineTrailingSpaces(currentLine, textMeasurer, style)
        if (trimmedDisplayLine.syllables.isNotEmpty()) {
            lines.add(trimmedDisplayLine)
        }
    }
    return lines
}

private fun trimDisplayLineTrailingSpaces(
    displayLineSyllables: List<SyllableLayout>, textMeasurer: TextMeasurer, style: TextStyle
): WrappedLine {
    if (displayLineSyllables.isEmpty()) {
        return WrappedLine(emptyList(), 0f)
    }

    val processedSyllables = displayLineSyllables.toMutableList()
    var lastIndex = processedSyllables.lastIndex

    while (lastIndex >= 0 && processedSyllables[lastIndex].word.content.isBlank()) {
        processedSyllables.removeAt(lastIndex)
        lastIndex--
    }

    if (processedSyllables.isEmpty()) {
        return WrappedLine(emptyList(), 0f)
    }

    val lastLayout = processedSyllables.last()
    val originalContent = lastLayout.word.content
    val trimmedContent = originalContent.trimEnd()

    if (trimmedContent.length < originalContent.length) {
        if (trimmedContent.isNotEmpty()) {
            val trimmedLayoutResult = textMeasurer.measure(trimmedContent, style)
            val trimmedLayout = lastLayout.copy(
                word = lastLayout.word.copy(content = trimmedContent),
                textLayoutResult = trimmedLayoutResult,
                width = trimmedLayoutResult.size.width.toFloat()
            )
            processedSyllables[processedSyllables.lastIndex] = trimmedLayout
        } else {
            processedSyllables.removeAt(processedSyllables.lastIndex)
        }
    }

    val totalWidth = processedSyllables.sumOf { it.width.toDouble() }.toFloat()
    return WrappedLine(processedSyllables, totalWidth)
}

private fun calculateStaticLineLayout(
    wrappedLines: List<WrappedLine>,
    align: TextAlign,
    canvasWidth: Float,
    lineHeight: Float,
    isRtl: Boolean
): List<List<SyllableLayout>> {
    return wrappedLines.mapIndexed { lineIndex, wrappedLine ->
        val maxBaselineInLine = wrappedLine.syllables.maxOfOrNull { it.firstBaseline } ?: 0f
        val rowTopY = lineIndex * lineHeight

        val startX = when (align) {
            TextAlign.Start -> if (isRtl) canvasWidth - wrappedLine.totalWidth else 0f
            TextAlign.End -> if (isRtl) 0f else canvasWidth - wrappedLine.totalWidth
            TextAlign.Right -> canvasWidth - wrappedLine.totalWidth
            TextAlign.Center -> (canvasWidth - wrappedLine.totalWidth) / 2f
            else -> 0f
        }

        var currentX = if (isRtl) startX + wrappedLine.totalWidth else startX

        wrappedLine.syllables.map { initialLayout ->
            val positionX = if (isRtl) {
                currentX - initialLayout.width
            } else {
                currentX
            }
            val verticalOffset = maxBaselineInLine - initialLayout.firstBaseline
            val positionY = rowTopY + verticalOffset
            val positionedLayout = initialLayout.copy(position = Offset(positionX, positionY))

            if (isRtl) {
                currentX -= positionedLayout.width
            } else {
                currentX += positionedLayout.width
            }

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

        val verticalPadding = totalHeight * 0.1f
        val horizontalPadding = totalWidth * 0.1f
        val edgePaddingPx = 8f * density

        RowRenderData(
            rowLayouts = rowLayouts,
            totalMinX = totalMinX,
            totalMaxX = totalMaxX,
            totalWidth = totalWidth,
            firstWordStart = rowLayouts.first().word.start,
            lastWordEnd = rowLayouts.last().word.end,
            layerBounds = Rect(
                left = totalMinX - horizontalPadding,
                top = minY - verticalPadding - edgePaddingPx,
                right = totalMaxX + horizontalPadding,
                bottom = minY + totalHeight + verticalPadding + edgePaddingPx
            )
        )
    }
}

private fun DrawScope.drawLyricsLine(
    selectedLine: Boolean,
    lineHasAlreadyPassed: Boolean,
    rowRenderData: List<RowRenderData>,
    currentTimeMs: Long,
    contentColor: Color,
    animatedAlpha: Float,
    shadowEffect: Boolean,
    rtlContent: Boolean
) {
    val overallFirstWordStart = rowRenderData.firstOrNull()?.firstWordStart ?: 0L
    val overallLastWordEnd = rowRenderData.lastOrNull()?.lastWordEnd ?: 0L
    val isLineStillPlaying = currentTimeMs in overallFirstWordStart..<overallLastWordEnd
    val effectivelySelected = selectedLine || isLineStillPlaying

    rowRenderData.forEach { rowData ->
        if (lineHasAlreadyPassed && !effectivelySelected) {
            drawRowText(
                rowLayouts = rowData.rowLayouts,
                drawColor = contentColor.copy(alpha = animatedAlpha),
                shadowEffect = false
            )
            return@forEach
        }

        if (currentTimeMs >= rowData.lastWordEnd) {
            drawRowText(
                rowLayouts = rowData.rowLayouts,
                drawColor = contentColor,
                shadowEffect = shadowEffect && effectivelySelected
            )
            return@forEach
        }

        drawIntoCanvas { canvas ->
            val layerBounds = rowData.layerBounds
            canvas.saveLayer(layerBounds, LayerPaint)

            drawRowText(
                rowLayouts = rowData.rowLayouts,
                drawColor = contentColor,
                shadowEffect = shadowEffect && effectivelySelected
            )

            val progressBrush = createLineGradientBrush(
                rowData = rowData,
                currentTimeMs = currentTimeMs,
                rtlContent = rtlContent,
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
    drawColor: Color,
    shadowEffect: Boolean
) {
    rowLayouts.forEach { syllableLayout ->
        val shadow = if (shadowEffect) Shadow(
            color = drawColor.copy(alpha = 0.4f),
            offset = Offset(0f, 2f * density),
            blurRadius = 4f * density
        ) else Shadow.None

        drawText(
            textLayoutResult = syllableLayout.textLayoutResult,
            color = drawColor,
            topLeft = syllableLayout.position,
            shadow = shadow
        )
    }
}

private fun createLineGradientBrush(
    rowData: RowRenderData,
    currentTimeMs: Long,
    rtlContent: Boolean,
    activeColor: Color,
    inactiveColor: Color
): Brush {
    val minFadeWidth = 20f

    val lineLayout = rowData.rowLayouts
    val totalMinX = rowData.totalMinX
    val totalMaxX = rowData.totalMaxX
    val totalWidth = rowData.totalWidth

    if (totalWidth <= 0f) {
        val isFinished = currentTimeMs >= lineLayout.last().word.end
        return SolidColor(if (isFinished) activeColor else inactiveColor)
    }

    val lineProgress = run {
        if (currentTimeMs <= rowData.firstWordStart) return@run if (rtlContent) 1f else 0f
        if (currentTimeMs >= rowData.lastWordEnd) return@run if (rtlContent) 0f else 1f

        val activeWordLayout = lineLayout.find {
            currentTimeMs in it.word.start until it.word.end
        }

        val currentPixelPosition = when {
            activeWordLayout != null -> {
                val duration = activeWordLayout.word.duration
                val wordProgress = if (duration > 0) {
                    ((currentTimeMs - activeWordLayout.word.start).toFloat() / duration).coerceIn(0f, 1f)
                } else 1f

                if (rtlContent) {
                    activeWordLayout.position.x + activeWordLayout.width * (1f - wordProgress)
                } else {
                    activeWordLayout.position.x + activeWordLayout.width * wordProgress
                }
            }
            else -> {
                val lastFinished = lineLayout.lastOrNull { currentTimeMs >= it.word.end }
                if (rtlContent) {
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

    val colorStops = if (rtlContent) {
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
    val wordId: Int,
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
