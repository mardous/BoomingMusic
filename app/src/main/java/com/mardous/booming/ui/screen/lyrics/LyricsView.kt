package com.mardous.booming.ui.screen.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.mardous.booming.data.model.lyrics.Lyrics
import com.mardous.booming.data.model.lyrics.LyricsActor
import com.mardous.booming.ui.component.compose.decoration.FadingEdges
import com.mardous.booming.ui.component.compose.decoration.fadingEdges
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val TextStyle.minimumPadding: Dp
    get() = (lineHeight.value * 8).dp

@Composable
fun LyricsView(
    state: LyricsViewState,
    settings: LyricsViewSettings,
    fadingEdges: FadingEdges,
    isPowerSaveMode: Boolean,
    hasGradientBackground: Boolean,
    modifier: Modifier = Modifier,
    onLineClick: (Lyrics.Line) -> Unit
) {
    val density = LocalDensity.current
    val textStyle = settings.syncedStyle

    val listState = rememberLazyListState()
    val isScrollInProgress = listState.isScrollInProgress
    val isInDragGesture by listState.interactionSource.collectIsDraggedAsState()

    val disableAdvancedEffects = isPowerSaveMode || hasGradientBackground.not()
    var disableBlurEffect by remember { mutableStateOf(disableAdvancedEffects) }
    if (isInDragGesture) {
        disableBlurEffect = true
    }

    LaunchedEffect(state.currentLineIndex) {
        if (state.currentLineIndex >= 0) {
            if (!isInDragGesture && !isScrollInProgress) {
                val layoutInfo = listState.layoutInfo
                val viewportHeight = with(layoutInfo) { viewportEndOffset - viewportStartOffset }
                val bottomPadding = with(density) { settings.contentPadding.calculateBottomPadding().toPx() }
                val activeItem = layoutInfo.visibleItemsInfo.find { it.index == state.currentLineIndex }
                if (activeItem != null) {
                    val itemSize = activeItem.size
                    val targetOffset = if (settings.isCenterCurrentLine) {
                        (viewportHeight / 2) - (itemSize / 2) - bottomPadding
                    } else {
                        0f
                    }
                    listState.animateScrollBy(
                        value = activeItem.offset - targetOffset,
                        animationSpec = tween(
                            run {
                                (state.lyrics?.lines?.getOrNull(state.currentLineIndex + 1)?.startAt ?: 0) -
                                        (state.lyrics?.lines?.getOrNull(state.currentLineIndex)?.startAt ?: 0)
                            }.let {
                                (it / 2).coerceIn(100, 1000).toInt()
                            }
                        )
                    )
                } else {
                    val fontSize = with(density) { textStyle.fontSize.toPx() * 2 }
                    val targetOffset = if (settings.isCenterCurrentLine) {
                        (viewportHeight / 2) - fontSize - bottomPadding
                    } else {
                        0
                    }
                    listState.animateScrollToItem(
                        index = state.currentLineIndex,
                        scrollOffset = -targetOffset.toInt()
                    )
                }
                disableBlurEffect = disableAdvancedEffects
            }
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = settings.contentPadding,
        modifier = modifier
            .nestedScroll(rememberNestedScrollInteropConnection())
            .fadingEdges(edges = fadingEdges)
            .fillMaxSize()
    ) {
        val lines = state.lyrics?.lines ?: emptyList()
        itemsIndexed(lines, key = { _, line -> line.id }) { index, line ->
            LyricsLineView(
                index = index,
                selectedIndex = state.currentLineIndex,
                selectedLine = index == state.currentLineIndex,
                enableSyllable = settings.enableSyllableLyrics && isPowerSaveMode.not(),
                progressiveColoring = settings.progressiveColoring && isPowerSaveMode.not(),
                enableBlurEffect = settings.blurEffect && disableBlurEffect.not(),
                enableShadowEffect = settings.shadowEffect && disableAdvancedEffects.not(),
                position = state.position,
                line = line,
                textStyle = textStyle,
                modifier = Modifier
                    .animateItem(placementSpec = tween(durationMillis = 1000)),
                onClick = { onLineClick(line) }
            )
        }
    }
}

@Composable
private fun LyricsLineView(
    index: Int,
    selectedIndex: Int,
    selectedLine: Boolean,
    enableSyllable: Boolean,
    progressiveColoring: Boolean,
    enableBlurEffect: Boolean,
    enableShadowEffect: Boolean,
    position: Long,
    line: Lyrics.Line,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val localContentColor = LocalContentColor.current

    val color = if (selectedLine) {
        localContentColor
    } else {
        localContentColor.copy(alpha = 0.5f)
    }

    val scale by animateFloatAsState(
        targetValue = if (selectedLine) 1.1f else 1f,
        animationSpec = tween(durationMillis = 700),
        label = "current-line-scale-animation"
    )

    val textAlign = when (line.actor) {
        LyricsActor.Voice2,
        LyricsActor.Voice2Background -> TextAlign.End

        LyricsActor.Group,
        LyricsActor.GroupBackground,
        LyricsActor.Duet,
        LyricsActor.DuetBackground -> TextAlign.Center

        else -> TextAlign.Start
    }

    LyricsLineBox(
        style = textStyle,
        align = textAlign,
        onClick = onClick
    ) { transformOrigin ->
        if (line.isEmpty) {
            BubblesLine(
                selectedLine = selectedLine,
                fontSize = textStyle.fontSize,
                position = position,
                startMillis = line.startAt,
                endMillis = line.end,
                modifier = Modifier.align(
                    when (textAlign) {
                        TextAlign.End -> Alignment.CenterEnd
                        TextAlign.Center -> Alignment.Center
                        else -> Alignment.CenterStart
                    }
                )
            )
        } else {
            Column(
                horizontalAlignment = when (textAlign) {
                    TextAlign.End -> Alignment.End
                    TextAlign.Center -> Alignment.CenterHorizontally
                    else -> Alignment.Start
                },
                verticalArrangement = Arrangement.spacedBy(textStyle.minimumPadding - 4.dp),
                modifier = modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        this.transformOrigin = transformOrigin
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                LyricsLineContentView(
                    index = index,
                    selectedIndex = selectedIndex,
                    content = line.content,
                    translatedContent = line.translation,
                    backgroundContent = false,
                    enableSyllable = enableSyllable,
                    progressiveColoring = progressiveColoring,
                    enableBlurEffect = enableBlurEffect,
                    enableShadowEffect = enableShadowEffect,
                    selectedLine = selectedLine,
                    contentColor = color,
                    position = position,
                    startMillis = line.startAt,
                    endMillis = line.end,
                    style = textStyle,
                    align = textAlign
                )

                if (line.content.hasBackgroundVocals) {
                    if (line.translation?.isEmpty == false) {
                        Spacer(modifier = Modifier.height(textStyle.minimumPadding * 2))
                    }
                    LyricsLineContentView(
                        index = index,
                        selectedIndex = selectedIndex,
                        content = line.content,
                        translatedContent = line.translation,
                        backgroundContent = true,
                        enableSyllable = enableSyllable,
                        progressiveColoring = progressiveColoring,
                        enableBlurEffect = enableBlurEffect,
                        enableShadowEffect = enableShadowEffect,
                        selectedLine = selectedLine,
                        contentColor = color,
                        position = position,
                        startMillis = line.startAt,
                        endMillis = line.end,
                        style = textStyle.copy(
                            fontSize = textStyle.fontSize / 1.40f,
                            fontWeight = FontWeight.Normal
                        ),
                        align = textAlign
                    )
                }
            }
        }
    }
}

@Composable
fun LyricsLineContentView(
    index: Int,
    selectedIndex: Int,
    content: Lyrics.TextContent,
    translatedContent: Lyrics.TextContent?,
    enableSyllable: Boolean,
    backgroundContent: Boolean,
    progressiveColoring: Boolean,
    enableBlurEffect: Boolean,
    enableShadowEffect: Boolean,
    selectedLine: Boolean,
    contentColor: Color,
    position: Long,
    startMillis: Long,
    endMillis: Long,
    style: TextStyle,
    align: TextAlign,
    modifier: Modifier = Modifier
) {
    val effectDuration = ((endMillis - startMillis) / 2).coerceAtMost(500).toInt()
    val blurRadius by animateFloatAsState(
        targetValue = if (index == selectedIndex) 0f else
                (abs(index - selectedIndex).toFloat() + 1.5f).coerceIn(0f, 10f),
        animationSpec = tween(effectDuration)
    )

    val blurEffect = remember(enableBlurEffect, blurRadius) {
        if (enableBlurEffect && blurRadius > 0f) {
            BlurEffect(
                radiusX = blurRadius,
                radiusY = blurRadius,
                edgeTreatment = TileMode.Clamp
            )
        } else null
    }

    val shadowRadius by animateFloatAsState(
        targetValue = if (selectedLine) 10f else 0f,
        animationSpec = tween(effectDuration)
    )
    val shadow = if (enableShadowEffect && selectedLine) {
        Shadow(
            color = contentColor.copy(alpha = .5f),
            blurRadius = shadowRadius
        )
    } else {
        Shadow.None
    }

    val mainVocals = content.getVocals(backgroundContent)
    val mainText = content.getText(backgroundContent)

    val mainProgressFraction = calculateLineProgress(mainVocals, position, startMillis, endMillis)
        .coerceIn(0f, 1f)

    if (enableSyllable && mainVocals.isNotEmpty()) {
        SyllableText(
            selectedLine = selectedLine,
            progressFraction = mainProgressFraction,
            content = mainText,
            contentColor = contentColor.copy(alpha = 1f),
            style = style.copy(shadow = shadow),
            align = align,
            modifier = modifier.graphicsLayer {
                renderEffect = blurEffect
            }
        )
    } else {
        LyricsTextView(
            selectedLine = selectedLine,
            progressiveColoring = progressiveColoring,
            progressFraction = mainProgressFraction,
            content = mainText,
            color = contentColor,
            style = style.copy(shadow = shadow),
            align = align,
            modifier = modifier.graphicsLayer {
                renderEffect = blurEffect
            }
        )
    }
    if (translatedContent != null && !translatedContent.isEmpty) {
        val translatedVocals = translatedContent.getVocals(backgroundContent)
        val translatedText = translatedContent.getText(backgroundContent)
        if (enableSyllable && translatedVocals.isNotEmpty()) {
            SyllableText(
                selectedLine = selectedLine,
                progressFraction = calculateLineProgress(translatedVocals, position, startMillis, endMillis)
                    .coerceIn(0f, 1f),
                content = translatedText,
                contentColor = contentColor.copy(alpha = 1f),
                style = style.copy(
                    fontSize = style.fontSize / 1.40,
                    fontWeight = FontWeight.Normal,
                    shadow = shadow
                ),
                align = align,
                modifier = modifier.graphicsLayer {
                    renderEffect = blurEffect
                }
            )
        } else {
            LyricsTextView(
                selectedLine = selectedLine,
                progressiveColoring = progressiveColoring && mainVocals.isEmpty(),
                progressFraction = mainProgressFraction,
                content = translatedText,
                color = contentColor,
                style = style.copy(
                    fontSize = style.fontSize / 1.40,
                    fontWeight = FontWeight.Normal,
                    shadow = shadow
                ),
                align = align,
                modifier = modifier.graphicsLayer {
                    renderEffect = blurEffect
                }
            )
        }
    }
}

@Composable
private fun LyricsTextView(
    selectedLine: Boolean,
    progressiveColoring: Boolean,
    progressFraction: Float,
    content: String,
    color: Color,
    style: TextStyle,
    align: TextAlign,
    modifier: Modifier = Modifier
) {
    var textHeight by remember { mutableFloatStateOf(0f) }

    val animatedOrigin by animateFloatAsState(
        targetValue = if (selectedLine) progressFraction * textHeight else 0f,
        label = "line-gradient-origin"
    )

    val textStyle by remember(selectedLine, progressiveColoring, animatedOrigin) {
        derivedStateOf {
            if (progressiveColoring) {
                style.copy(
                    brush = Brush.verticalGradient(
                        colors = listOf(color, color.copy(alpha = color.alpha / 2)),
                        startY = animatedOrigin - 10f,
                        endY = animatedOrigin + 10f
                    )
                )
            } else {
                style.copy(color = color)
            }
        }
    }

    Text(
        text = content,
        style = textStyle,
        textAlign = align,
        modifier = modifier
            .onGloballyPositioned {
                textHeight = it.size.height.toFloat()
            }
    )
}

@Composable
private fun SyllableText(
    selectedLine: Boolean,
    progressFraction: Float,
    content: String,
    contentColor: Color,
    style: TextStyle,
    align: TextAlign,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    var textWidth by remember { mutableIntStateOf(0) }

    val textLayout = remember(textWidth) {
        textMeasurer.measure(
            text = content,
            style = style,
            constraints = Constraints(maxWidth = textWidth)
        )
    }

    HorizontalReveal(
        selectedLine = selectedLine,
        textLayout = textLayout,
        textAlign = align,
        textWidth = textWidth,
        progress = progressFraction
    ) {
        Text(
            text = content,
            style = style,
            color = contentColor,
            textAlign = align,
            modifier = modifier
                .onGloballyPositioned {
                    textWidth = it.size.width
                }
        )
    }
}

@Composable
private fun HorizontalReveal(
    textLayout: TextLayoutResult,
    textAlign: TextAlign,
    textWidth: Int,
    selectedLine: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
    rtl: Boolean = false,
    content: @Composable () -> Unit
) {
    val lineRect = rememberLineRect(textLayout)

    Box(modifier = modifier) {
        Box(modifier = modifier.alpha(.5f)) {
            content()
        }

        if (selectedLine || progress > 0f && progress < 1f) {
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(durationMillis = 100, easing = LinearEasing)
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        clip = true
                        shape = RectangleShape
                    }
                    .drawWithCache {
                        onDrawWithContent {
                            val lineCount = textLayout.lineCount
                            val scaledProgress = animatedProgress * lineCount
                            val currentLine = scaledProgress.toInt()
                                .coerceAtMost(lineCount - 1)

                            fun Rect.getActualLeft() = when (textAlign) {
                                TextAlign.Center -> (textWidth - width) / 2
                                TextAlign.End -> (textWidth - width)
                                else -> left
                            }

                            fun Rect.getActualRight() = when (textAlign) {
                                TextAlign.Center,
                                TextAlign.End -> (getActualLeft() + width)

                                else -> width
                            }

                            for (i in 0 until currentLine) {
                                val rect = lineRect[i]
                                clipRect(
                                    rect.getActualLeft(),
                                    rect.top,
                                    rect.getActualRight(),
                                    rect.bottom
                                ) {
                                    this@onDrawWithContent.drawContent()
                                }
                            }

                            val currentRect = lineRect[currentLine]
                            val currentLeft = currentRect.getActualLeft()
                            val currentRight = currentRect.getActualRight()

                            val fraction = scaledProgress - currentLine
                            val clipWidth = currentRect.width * fraction.coerceIn(0f, 1f)

                            val left = if (!rtl) currentLeft else currentRight - clipWidth
                            val right = if (!rtl) currentLeft + clipWidth else currentRight

                            clipRect(left, currentRect.top, right, currentRect.bottom) {
                                this@onDrawWithContent.drawContent()
                            }
                        }
                    }
            ) {
                content()
            }
        }
    }
}

@Composable
private fun LyricsLineBox(
    style: TextStyle,
    align: TextAlign,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(transformOrigin: TransformOrigin) -> Unit
) {
    val transformOrigin = when (align) {
        TextAlign.End -> TransformOrigin(1f, 1f)
        TextAlign.Start -> TransformOrigin(0f, 1f)
        else -> TransformOrigin.Center
    }

    val verticalPadding = style.minimumPadding * 2
    val paddingValues = when (align) {
        TextAlign.End -> PaddingValues(
            start = 32.dp,
            top = verticalPadding,
            end = 8.dp,
            bottom = verticalPadding
        )

        TextAlign.Start -> PaddingValues(
            start = 8.dp,
            top = verticalPadding,
            end = 32.dp,
            bottom = verticalPadding
        )

        else -> PaddingValues(horizontal = 32.dp, vertical = verticalPadding)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = null,
                onClick = onClick
            )
            .padding(paddingValues)
    ) {
        content(transformOrigin)
    }
}

/**
 * From [Lotus music player](https://github.com/dn0ne/lotus)
 */
@Composable
private fun BubblesLine(
    selectedLine: Boolean,
    fontSize: TextUnit,
    position: Long,
    startMillis: Long,
    endMillis: Long,
    modifier: Modifier = Modifier
) {
    var bubblesContainerHeight by remember {
        mutableFloatStateOf(0f)
    }

    val progressFraction by remember(position) {
        derivedStateOf {
            ((position.toFloat() - startMillis) / (endMillis - startMillis))
                .coerceIn(0f, 1f)
        }
    }

    val density = LocalDensity.current
    val height = with(density) { (fontSize / 1.35).toDp() }

    val infiniteTransition = rememberInfiniteTransition(
        label = "bubbles-transition"
    )

    val firstBubbleProgress by remember(progressFraction) {
        derivedStateOf {
            (progressFraction / .33f).coerceIn(0f, 1f)
        }
    }

    val firstBubbleTranslationX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "first-bubble-translation-x"
    )

    val secondBubbleProgress by remember(progressFraction) {
        derivedStateOf {
            ((progressFraction - .33f) / .33f).coerceIn(0f, 1f)
        }
    }

    val secondBubbleTranslationX by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(
                offsetMillis = 500,
                offsetType = StartOffsetType.FastForward
            )
        ),
        label = "first-bubble-translation-x"
    )

    val thirdBubbleProgress by remember(progressFraction) {
        derivedStateOf {
            ((progressFraction - .33f * 2) / .33f).coerceIn(0f, 1f)
        }
    }

    val thirdBubbleTranslationX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(
                offsetMillis = 1000,
                offsetType = StartOffsetType.FastForward
            )
        ),
        label = "first-bubble-translation-x"
    )

    val scale by animateFloatAsState(
        targetValue = if (progressFraction < .97f) 1f else 1.2f,
        label = "bubbles-scale-before-next-line"
    )

    Box(
        modifier = modifier
            .padding(vertical = 4.dp)
            .height(height)
            .onGloballyPositioned {
                bubblesContainerHeight = it.size.height.toFloat()
            },
    ) {
        AnimatedVisibility(
            visible = selectedLine,
            enter = scaleIn(),
            exit = scaleOut(),
            modifier = Modifier
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Bubble(
                    bubbleHeight = height,
                    containerHeight = bubblesContainerHeight,
                    animationProgress = firstBubbleProgress,
                    translationX = firstBubbleTranslationX,
                    translationOffset = secondBubbleTranslationX
                )

                Bubble(
                    bubbleHeight = height,
                    containerHeight = bubblesContainerHeight,
                    animationProgress = secondBubbleProgress,
                    translationX = secondBubbleTranslationX,
                    translationOffset = thirdBubbleTranslationX
                )

                Bubble(
                    bubbleHeight = height,
                    containerHeight = bubblesContainerHeight,
                    animationProgress = thirdBubbleProgress,
                    translationX = thirdBubbleTranslationX,
                    translationOffset = firstBubbleTranslationX
                )
            }
        }
    }
}

@Composable
private fun Bubble(
    bubbleHeight: Dp,
    containerHeight: Float,
    animationProgress: Float,
    translationX: Float,
    translationOffset: Float,
    color: Color = LocalContentColor.current,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(bubbleHeight * .7f)
            .graphicsLayer {
                this.translationY =
                    -containerHeight / 6 *
                            (sin(20 * (animationProgress - .25f) / PI.toFloat()) / 2 + .5f) +
                            translationX * translationOffset / 2

                this.translationX = translationX
                val scale = .5f + animationProgress / 2
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                drawCircle(
                    radius = size.width,
                    brush = Brush.radialGradient(
                        0f to Color.Transparent,
                        .5f to Color.Transparent,
                        .5f to color.copy(alpha = animationProgress / 2 - .25f),
                        .6f to color.copy(alpha = animationProgress / 3 - .25f),
                        .8f to Color.Transparent,
                        radius = size.width
                    )
                )

                drawCircle(
                    color = color.copy(
                        alpha = .25f + animationProgress
                    )
                )
            }
    )
}

@Composable
private fun rememberLineRect(textLayout: TextLayoutResult): List<Rect> {
    return remember(textLayout) {
        (0 until textLayout.lineCount).map { i ->
            Rect(
                offset = Offset(
                    x = textLayout.getLineLeft(i),
                    y = textLayout.getLineTop(i)
                ),
                size = Size(
                    width = textLayout.multiParagraph.getLineRight(i),
                    height = textLayout.multiParagraph.getLineHeight(i)
                )
            )
        }
    }
}

private fun calculateLineProgress(
    words: List<Lyrics.Word>,
    position: Long,
    startMillis: Long,
    endMillis: Long
): Float {
    // by line if no words are available
    if (words.isEmpty()) {
        return when {
            position < startMillis -> 0f
            position > endMillis - 200L -> 1f // add buffer so lyric line animation completes
            else -> (position - startMillis).toFloat() / (endMillis - startMillis).toFloat()
        }
    }

    var completedWords = 0
    var partialProgress = 0f
    return when {
        position < startMillis -> 0f
        position > endMillis - 200L -> 1f // add buffer so lyric line animation completes
        else -> {
            for (i in words.indices) {
                val word = words[i]
                val start = word.startMillis
                val end = word.endMillis
                if (position < start) {
                    break // we're before this word
                } else if (position in (start..end)) {
                    partialProgress = (position - start).toFloat() / word.durationMillis
                    completedWords = i
                    break
                } else {
                    completedWords++
                }
            }

            val totalWords = words.size.toFloat()
            var progress = (completedWords + partialProgress) / totalWords
            if (progress > 0.95f) {
                progress = 1f
            }
            progress.coerceIn(0f, 1f)
        }
    }
}