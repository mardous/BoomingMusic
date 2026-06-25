package com.mardous.booming.ui.component.compose.lyrics

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.mardous.booming.R
import com.mardous.booming.core.model.lyrics.LyricsViewSettings
import com.mardous.booming.core.model.lyrics.LyricsViewState
import com.mardous.booming.data.model.lyrics.LyricsActor
import com.mardous.booming.data.model.lyrics.SyncedLyrics
import com.mardous.booming.extensions.hasS
import com.mardous.booming.extensions.utilities.isRtl
import com.mardous.booming.ui.component.compose.decoration.FadingEdges
import com.mardous.booming.ui.component.compose.decoration.fadingEdges
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun LyricsView(
    state: LyricsViewState,
    settings: LyricsViewSettings,
    fadingEdges: FadingEdges,
    contentColor: Color,
    isPowerSaveMode: Boolean,
    hasBackgroundEffects: Boolean,
    modifier: Modifier = Modifier,
    onLineClick: (SyncedLyrics.Line) -> Unit
) {
    val density = LocalDensity.current
    val textStyle = settings.syncedStyle

    val listState = rememberLazyListState()
    val isScrollInProgress = listState.isScrollInProgress
    val isInDragGesture by listState.interactionSource.collectIsDraggedAsState()

    val lineSpacing = settings.lineSpacing.dp

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
                                (state.lyrics?.lines?.getOrNull(state.currentLineIndex + 1)?.start ?: 0) -
                                        (state.lyrics?.lines?.getOrNull(state.currentLineIndex)?.start ?: 0)
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
        verticalArrangement = Arrangement.spacedBy(settings.lineSpacing.dp),
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
                isCenterHorizontally = settings.isCenterHorizontally,
                enableSyllable = settings.enableSyllableLyrics && isPowerSaveMode.not(),
                enableKaraokeStyle = settings.enableKaraokeStyle,
                progressiveColoring = settings.progressiveColoring && isPowerSaveMode.not(),
                enableBlurEffect = settings.blurEffect && disableBlurEffect.not(),
                enableShadowEffect = settings.shadowEffect && disableAdvancedEffects.not(),
                contentColor = contentColor,
                progressMillis = state.position,
                line = line,
                textStyle = textStyle,
                lineSpacing = lineSpacing,
                modifier = Modifier
                    .animateItem(placementSpec = tween(durationMillis = 500)),
                onClick = { onLineClick(line) }
            )
        }

        val provider = state.lyrics?.provider
        if (settings.mode == LyricsViewSettings.Mode.Full && !provider.isNullOrEmpty()) {
            item("LyricsProvider") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .padding(top = 56.dp)
                ) {
                    Text(
                        text = stringResource(R.string.lyrics_by_x, provider),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = .5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsLineView(
    index: Int,
    selectedIndex: Int,
    selectedLine: Boolean,
    isCenterHorizontally: Boolean,
    enableSyllable: Boolean,
    enableKaraokeStyle: Boolean,
    progressiveColoring: Boolean,
    enableBlurEffect: Boolean,
    enableShadowEffect: Boolean,
    contentColor: Color,
    progressMillis: Long,
    line: SyncedLyrics.Line,
    textStyle: TextStyle,
    lineSpacing: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selectedLine) 1.1f else 1f,
        animationSpec = tween(durationMillis = 700),
        label = "current-line-scale-animation"
    )

    val isRtl = line.content.content.isRtl()
    val textAlign = if (isCenterHorizontally) {
        TextAlign.Center
    } else {
        when (line.actor) {
            LyricsActor.Voice2,
            LyricsActor.Voice2Background -> TextAlign.End

            LyricsActor.Group,
            LyricsActor.GroupBackground,
            LyricsActor.Duet,
            LyricsActor.DuetBackground -> TextAlign.Center

            else -> TextAlign.Start
        }
    }

    val transformOrigin = when (textAlign) {
        TextAlign.End -> if (isRtl) TransformOrigin(0f, 1f) else TransformOrigin(1f, 1f)
        TextAlign.Start -> if (isRtl) TransformOrigin(1f, 1f) else TransformOrigin(0f, 1f)
        else -> TransformOrigin.Center
    }

    val paddingValues = when (textAlign) {
        TextAlign.End -> PaddingValues(start = 32.dp, end = 8.dp)
        TextAlign.Start -> PaddingValues(start = 8.dp, end = 32.dp)
        else -> PaddingValues(horizontal = 32.dp)
    }

    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
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
            if (line.isEmpty) {
                BubblesLine(
                    selectedLine = selectedLine,
                    color = contentColor,
                    fontSize = textStyle.fontSize,
                    progressMillis = progressMillis,
                    startMillis = line.start,
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
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
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
                        transliterationContent = line.transliteration,
                        backgroundContent = false,
                        enableSyllable = enableSyllable,
                        enableKaraokeStyle = enableKaraokeStyle,
                        progressiveColoring = progressiveColoring,
                        enableBlurEffect = enableBlurEffect,
                        enableShadowEffect = enableShadowEffect,
                        selectedLine = selectedLine,
                        contentColor = contentColor,
                        progressMillis = progressMillis,
                        startMillis = line.start,
                        endMillis = line.end,
                        style = textStyle,
                        align = textAlign
                    )

                    if (line.content.hasBackgroundSyllables) {
                        if (line.translation?.isEmpty == false) {
                            Spacer(modifier = Modifier.height(lineSpacing))
                        }
                        LyricsLineContentView(
                            index = index,
                            selectedIndex = selectedIndex,
                            content = line.content,
                            translatedContent = line.translation,
                            transliterationContent = line.transliteration,
                            backgroundContent = true,
                            enableSyllable = enableSyllable,
                            enableKaraokeStyle = enableKaraokeStyle,
                            progressiveColoring = progressiveColoring,
                            enableBlurEffect = enableBlurEffect,
                            enableShadowEffect = enableShadowEffect,
                            selectedLine = selectedLine,
                            contentColor = contentColor,
                            progressMillis = progressMillis,
                            startMillis = line.start,
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
}

@Composable
fun LyricsLineContentView(
    index: Int,
    selectedIndex: Int,
    content: SyncedLyrics.TextContent,
    translatedContent: SyncedLyrics.TextContent?,
    transliterationContent: SyncedLyrics.TextContent?,
    enableSyllable: Boolean,
    backgroundContent: Boolean,
    progressiveColoring: Boolean,
    enableKaraokeStyle: Boolean,
    enableBlurEffect: Boolean,
    enableShadowEffect: Boolean,
    selectedLine: Boolean,
    contentColor: Color,
    progressMillis: Long,
    startMillis: Long,
    endMillis: Long,
    style: TextStyle,
    align: TextAlign,
    modifier: Modifier = Modifier
) {
    val progressFraction = when {
        progressMillis < startMillis -> 0f
        progressMillis > endMillis -> 1f
        else -> ((progressMillis - startMillis).toFloat() / (endMillis - startMillis).toFloat()).coerceIn(0f, 1f)
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

    val mainSyllables = content.getSyllables(backgroundContent)
    val mainText = content.getText(backgroundContent)

    LineTextView(
        plainText = mainText,
        syllables = mainSyllables,
        enableSyllable = enableSyllable,
        enableKaraokeStyle = enableKaraokeStyle,
        enableShadowEffect = enableShadowEffect,
        progressiveColoring = progressiveColoring,
        selectedLine = selectedLine,
        contentColor = contentColor,
        effectDuration = effectDuration,
        progressFraction = progressFraction,
        progressMillis = progressMillis,
        style = style,
        align = align,
        modifier = modifier.graphicsLayer {
            renderEffect = blurEffect
        }
    )

    if (transliterationContent != null && !transliterationContent.isEmpty) {
        LineTextView(
            plainText = transliterationContent.getText(backgroundContent),
            syllables = transliterationContent.getSyllables(backgroundContent),
            enableSyllable = enableSyllable,
            enableKaraokeStyle = enableKaraokeStyle,
            enableShadowEffect = enableShadowEffect,
            progressiveColoring = progressiveColoring && mainSyllables.isEmpty(),
            selectedLine = selectedLine,
            contentColor = contentColor,
            effectDuration = effectDuration,
            progressFraction = progressFraction,
            progressMillis = progressMillis,
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

    if (translatedContent != null && !translatedContent.isEmpty) {
        val fontSizeDivider =
            if (transliterationContent != null && !transliterationContent.isEmpty) 1.60f else 1.40f

        LineTextView(
            plainText = translatedContent.getText(backgroundContent),
            syllables = translatedContent.getSyllables(backgroundContent),
            enableSyllable = enableSyllable,
            enableKaraokeStyle = enableKaraokeStyle,
            enableShadowEffect = enableShadowEffect,
            progressiveColoring = progressiveColoring && mainSyllables.isEmpty(),
            selectedLine = selectedLine,
            contentColor = contentColor,
            effectDuration = effectDuration,
            progressFraction = progressFraction,
            progressMillis = progressMillis,
            style = style.copy(
                fontSize = style.fontSize / fontSizeDivider,
                fontWeight = FontWeight.Normal
            ),
            align = align,
            modifier = modifier.graphicsLayer {
                renderEffect = blurEffect
            }
        )
    }
}

@Composable
private fun LineTextView(
    plainText: String,
    syllables: List<SyncedLyrics.Word>,
    enableSyllable: Boolean,
    enableKaraokeStyle: Boolean,
    enableShadowEffect: Boolean,
    progressiveColoring: Boolean,
    selectedLine: Boolean,
    contentColor: Color,
    effectDuration: Int,
    progressFraction: Float,
    progressMillis: Long,
    style: TextStyle,
    align: TextAlign,
    modifier: Modifier = Modifier
) {
    if (enableSyllable && syllables.isNotEmpty()) {
        WordSyncedText(
            karaokeStyle = enableKaraokeStyle,
            selectedLine = selectedLine,
            shadowEffect = enableShadowEffect,
            progress = progressMillis,
            syllables = syllables,
            contentColor = contentColor,
            style = style,
            align = align,
            modifier = modifier
        )
    } else {
        LineSyncedView(
            selectedLine = selectedLine,
            progressiveColoring = progressiveColoring,
            shadowEffect = enableShadowEffect,
            effectDuration = effectDuration,
            progressFraction = progressFraction,
            content = plainText,
            color = contentColor,
            style = style,
            align = align,
            modifier = modifier
        )
    }
}

@Composable
private fun LineSyncedView(
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

    val animatedAlpha by animateFloatAsState(
        targetValue = if (selectedLine) 1f else .4f,
        animationSpec = tween(400),
        label = "current-line-alpha-animation"
    )

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
                        colors = listOf(color, color.copy(alpha = .4f)),
                        startY = animatedOrigin - 10f,
                        endY = animatedOrigin + 10f
                    )
                )
            } else {
                style.copy(color = color.copy(alpha = animatedAlpha))
            }
        }
    }

    Text(
        text = content,
        style = textStyle.copy(shadow = shadow),
        textAlign = align,
        modifier = modifier
            .onGloballyPositioned {
                textHeight = it.size.height.toFloat()
            }
    )
}

@Composable
private fun WordSyncedText(
    karaokeStyle: Boolean,
    selectedLine: Boolean,
    shadowEffect: Boolean,
    progress: Long,
    syllables: List<SyncedLyrics.Word>,
    contentColor: Color,
    style: TextStyle,
    align: TextAlign,
    modifier: Modifier = Modifier
) {
    if (karaokeStyle) {
        KaraokeLineView(
            selectedLine = selectedLine,
            shadowEffect = shadowEffect,
            currentMillis = progress,
            syllables = syllables,
            contentColor = contentColor,
            style = style,
            align = align,
            modifier = modifier
        )
    } else {
        SpannedWordText(
            selectedLine = selectedLine,
            shadowEffect = shadowEffect,
            currentMillis = progress,
            syllables = syllables,
            contentColor = contentColor,
            style = style,
            align = align,
            modifier = modifier
        )
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
    progressMillis: Long,
    startMillis: Long,
    endMillis: Long,
    modifier: Modifier = Modifier
) {
    var bubblesContainerHeight by remember {
        mutableFloatStateOf(0f)
    }

    val progressFraction by remember(progressMillis) {
        derivedStateOf {
            ((progressMillis.toFloat() - startMillis) / (endMillis - startMillis))
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