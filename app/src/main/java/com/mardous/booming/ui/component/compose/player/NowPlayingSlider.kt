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

package com.mardous.booming.ui.component.compose.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

enum class TrackStyle { Straight, Wavy }
enum class ThumbStyle { Straight, Ball }

@Composable
fun NowPlayingSlider(
    sliderState: SliderState,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    trackStyle: TrackStyle = TrackStyle.Wavy,
    thumbStyle: ThumbStyle = ThumbStyle.Straight,
    colors: SliderColors = SliderDefaults.colors(),
    enabled: Boolean = true,
) {
    Slider(
        state = sliderState,
        colors = colors,
        thumb = { sliderState ->
            when (thumbStyle) {
                ThumbStyle.Straight -> StraightThumb(sliderState.isDragging)
                ThumbStyle.Ball -> BallThumb(sliderState.isDragging)
            }
        },
        track = { sliderState ->
            when (trackStyle) {
                TrackStyle.Straight -> StraightTrack(sliderState)
                TrackStyle.Wavy -> WavyTrack(isPlaying, sliderState)
            }
        },
        enabled = enabled,
        modifier = modifier
    )
}

@Composable
fun StraightThumb(isDragging: Boolean) {
    val animatedHeight by animateDpAsState(
        targetValue = if (isDragging) 40.dp else 35.dp
    )

    val animatedWidth by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 6.dp
    )

    SliderDefaults.Thumb(
        interactionSource = rememberInteractionSource(),
        thumbSize = DpSize(animatedWidth, animatedHeight)
    )
}

@Composable
fun BallThumb(isDragging: Boolean) {
    val width by animateDpAsState(
        targetValue = if (isDragging) 28.dp else 20.dp
    )
    SliderDefaults.Thumb(
        interactionSource = rememberInteractionSource(),
        thumbSize = DpSize(
            width = width,
            height = 20.dp
        )
    )
}

@Composable
fun WavyTrack(
    isPlaying: Boolean,
    sliderState: SliderState
) {
    val animatedHeight by animateDpAsState(
        if (sliderState.isDragging) 7.dp else 4.dp
    )
    val trackStroke = Stroke(
        width =
            with(LocalDensity.current) {
                animatedHeight.toPx()
            },
        cap = StrokeCap.Round,
    )

    LinearWavyProgressIndicator(
        modifier = Modifier.fillMaxWidth(),
        progress = {
            val rangeLength = sliderState.valueRange.endInclusive - sliderState.valueRange.start
            if (rangeLength > 0f) {
                (sliderState.value - sliderState.valueRange.start) / rangeLength
            } else 0f
        },
        stopSize = 0.dp,
        trackStroke = trackStroke,
        amplitude = { if (isPlaying && !sliderState.isDragging) 1f else 0f }
    )
}

@Composable
fun StraightTrack(sliderState: SliderState) {
    SliderDefaults.Track(
        sliderState = sliderState,
        drawStopIndicator = null,
        modifier = Modifier.height(8.dp)
    )
}

@Composable
private fun rememberInteractionSource(): MutableInteractionSource {
    return remember { MutableInteractionSource() }
}