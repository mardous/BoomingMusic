package com.mardous.booming.ui.screen.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.mardous.booming.data.model.lyrics.Lyrics
import com.mardous.booming.data.model.lyrics.LyricsActor
import com.mardous.booming.extensions.hasS
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
    contentColor: Color,
    isPowerSaveMode: Boolean,
    hasBackgroundEffects: Boolean,
    modifier: Modifier = Modifier,
    onLineClick: (Lyrics.Line) -> Unit
) {
    val density = LocalDensity.current
    val textStyle = settings.syncedStyle

    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    val listState = rememberLazyListState()
    val isScrollInProgress = listState.isScrollInProgress
    val isInDragGesture by listState.interactionSource.collectIsDraggedAsState()

    val disableAdvancedEffects = isPowerSaveMode || hasBackgroundEffects.not()
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
                            durationMillis = run {
                                (state.lyrics?.lines?.getOrNull(state.currentLineIndex + 1)?.startAt ?: 0) -
                                        (state.lyrics?.lines?.getOrNull(state.currentLineIndex)?.startAt ?: 0)
                            }.let {
                                (it / 2).coerceIn(100, 1000).toInt()
                            },
                            easing = FastOutSlowInEasing
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
                contentColor = contentColor,
                position = state.position,
                line = line,
                textStyle = textStyle,
                rtl = isRtl,
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
    contentColor: Color,
    position: Long,
    line: Lyrics.Line,
    textStyle: TextStyle,
    rtl: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (selectedLine) 1f else .5f,
        animationSpec = tween(durationMillis = 400),
        label = "current-line-alpha-animation"
    )

    val color = if (selectedLine) {
        contentColor.copy(alpha = animatedAlpha)
    } else {
        contentColor.copy(alpha = 0.5f)
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
        rtl = rtl,
        onClick = onClick
    ) { transformOrigin ->
        if (line.isEmpty) {
            BubblesLine(
                selectedLine = selectedLine,
                color = color,
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
    val progressFraction = when {
        position < startMillis -> 0f
        position > endMillis -> 1f
        else -> ((position - startMillis).toFloat() / (endMillis - startMillis).toFloat()).coerceIn(0f, 1f)
    }

    val effectDuration = ((endMillis - startMillis) / 2).coerceAtMost(500).toInt()
    val blurRadius by animateFloatAsState(
        targetValue = if (index == selectedIndex) 0f else
                (abs(index - selectedIndex).toFloat() + 1.5f).coerceIn(0f, 10f),
        animationSpec = tween(effectDuration)
    )

    val blurEffect = remember(enableBlurEffect, blurRadius) {
        if (hasS() && enableBlurEffect && blurRadius > 0f) {
            BlurEffect(
                radiusX = blurRadius,
                radiusY = blurRadius,
                edgeTreatment = TileMode.Clamp
            )
        } else null
    }

    val mainVocals = content.getVocals(backgroundContent)
    val mainText = content.getText(backgroundContent)

    if (enableSyllable && mainVocals.isNotEmpty()) {
        SyllableText(
            selectedLine = selectedLine,
            shadowEffect = enableShadowEffect,
            position = position,
            words = mainVocals,
            contentColor = contentColor,
            style = style,
            align = align,
            modifier = modifier.graphicsLayer {
                renderEffect = blurEffect
            }
        )
    } else {
        LyricsTextView(
            selectedLine = selectedLine,
            progressiveColoring = progressiveColoring,
            shadowEffect = enableShadowEffect,
            effectDuration = effectDuration,
            progressFraction = progressFraction,
            content = mainText,
            color = contentColor,
            style = style,
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
                shadowEffect = enableShadowEffect,
                position = position,
                words = translatedVocals,
                contentColor = contentColor,
                style = style.copy(
                    fontSize = style.fontSize / 1.40,
                    fontWeight = FontWeight.Normal
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
                shadowEffect = enableShadowEffect,
                effectDuration = effectDuration,
                progressFraction = progressFraction,
                content = translatedText,
                color = contentColor,
                style = style.copy(
                    fontSize = style.fontSize / 1.40,
                    fontWeight = FontWeight.Normal
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
    shadowEffect: Boolean,
    effectDuration: Int,
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

    val shadowRadius by animateFloatAsState(
        targetValue = if (selectedLine) 10f * progressFraction else 0f,
        animationSpec = tween(effectDuration)
    )

    val shadow = if (shadowEffect && selectedLine) {
        Shadow(
            color = color.copy(alpha = .5f),
            blurRadius = shadowRadius
        )
    } else {
        Shadow.None
    }

    val textStyle by remember(color, selectedLine, progressiveColoring, animatedOrigin) {
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
        style = textStyle.copy(
            fontWeight = if (selectedLine) FontWeight.ExtraBold else FontWeight.Medium,
            shadow = shadow
        ),
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
    shadowEffect: Boolean,
    position: Long,
    words: List<Lyrics.Word>,
    contentColor: Color,
    style: TextStyle,
    align: TextAlign,
    modifier: Modifier = Modifier
) {
    // Original code from Metrolist (https://github.com/mostafaalagamy/Metrolist)
    val styledText = buildAnnotatedString {
        words.forEach { word ->
            val wordStartMs = word.startMillis
            val wordEndMs = word.endMillis
            val wordDuration = word.durationMillis

            val isWordActive = selectedLine && position >= wordStartMs && position <= wordEndMs
            val hasWordPassed = selectedLine && position > wordEndMs

            val fadeProgress = if (isWordActive && wordDuration > 0) {
                val timeElapsed = position - wordStartMs
                val linear = (timeElapsed.toFloat() / wordDuration.toFloat()).coerceIn(0f, 1f)
                // Smooth cubic easing
                linear * linear * (3f - 2f * linear)
            } else if (hasWordPassed) 1f else 0f

            val wordAlpha = when {
                !selectedLine -> 0.5f
                hasWordPassed -> 1f
                isWordActive -> 0.4f + (0.6f * fadeProgress)
                else -> 0.4f
            }
            val wordWeight = when {
                !selectedLine -> FontWeight.Medium
                hasWordPassed -> FontWeight.Bold
                isWordActive -> FontWeight.ExtraBold
                else -> FontWeight.Medium
            }
            val wordShadow = when {
                shadowEffect && isWordActive && fadeProgress > 0.2f -> Shadow(
                    color = contentColor.copy(alpha = 0.35f * fadeProgress),
                    offset = Offset.Zero,
                    blurRadius = 10f * fadeProgress
                )
                shadowEffect && hasWordPassed -> Shadow(
                    color = contentColor.copy(alpha = 0.15f),
                    offset = Offset.Zero,
                    blurRadius = 6f
                )
                else -> null
            }
            val wordColor = contentColor.copy(alpha = wordAlpha)

            withStyle(
                style = SpanStyle(
                    color = wordColor,
                    fontWeight = wordWeight,
                    shadow = wordShadow
                )
            ) {
                append(word.content)
            }
        }
    }
    Text(
        text = styledText,
        style = style,
        textAlign = align,
        modifier = modifier
    )
}

@Composable
private fun LyricsLineBox(
    style: TextStyle,
    align: TextAlign,
    rtl: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(transformOrigin: TransformOrigin) -> Unit
) {
    val transformOrigin = when (align) {
        TextAlign.End -> if (rtl) TransformOrigin(0f, 1f) else TransformOrigin(1f, 1f)
        TextAlign.Start -> if (rtl) TransformOrigin(1f, 1f) else TransformOrigin(0f, 1f)
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
    color: Color,
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
                    translationOffset = secondBubbleTranslationX,
                    color = color
                )

                Bubble(
                    bubbleHeight = height,
                    containerHeight = bubblesContainerHeight,
                    animationProgress = secondBubbleProgress,
                    translationX = secondBubbleTranslationX,
                    translationOffset = thirdBubbleTranslationX,
                    color = color
                )

                Bubble(
                    bubbleHeight = height,
                    containerHeight = bubblesContainerHeight,
                    animationProgress = thirdBubbleProgress,
                    translationX = thirdBubbleTranslationX,
                    translationOffset = firstBubbleTranslationX,
                    color = color
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
    color: Color,
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