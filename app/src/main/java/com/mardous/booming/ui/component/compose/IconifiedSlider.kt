package com.mardous.booming.ui.component.compose

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.mardous.booming.ui.theme.SliderTokens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IconifiedSliderTrack(
    state: SliderState,
    icon: Painter,
    modifier: Modifier = Modifier,
    disabledIcon: Painter = icon,
    colors: SliderColors = SliderDefaults.colors(),
    thumbTrackGapSize: Dp = SliderTokens.ThumbTrackGapSize,
    trackCornerSize: Dp = SliderTokens.TrackCornerSize,
    enabled: Boolean = true,
    iconPadding: Dp = 10.dp,
    iconSize: Dp = 24.dp
) {
    val iconDpSize = DpSize(iconSize, iconSize)
    val activeIconColor = colors.activeTickColor
    val inactiveIconColor = colors.inactiveTickColor

    SliderDefaults.Track(
        sliderState = state,
        modifier = modifier
            .drawWithContent {
                drawContent()
                val yOffset = size.height / 2 - iconDpSize.toSize().height / 2
                val fraction = state.coercedValueAsFraction
                val thumbGapPx = thumbTrackGapSize.toPx()
                val activeTrackEnd = size.width * fraction - thumbGapPx
                val inactiveTrackStart = activeTrackEnd + thumbGapPx * 2
                val inactiveTrackWidth = size.width - inactiveTrackStart

                drawTrackIcon(
                    icon = icon,
                    disabledIcon = disabledIcon,
                    iconSize = iconDpSize,
                    iconPadding = iconPadding,
                    yOffset = yOffset,
                    activeTrackWidth = activeTrackEnd,
                    inactiveTrackStart = inactiveTrackStart,
                    inactiveTrackWidth = inactiveTrackWidth,
                    activeIconColor = activeIconColor,
                    inactiveIconColor = inactiveIconColor
                )
            },
        colors = colors,
        enabled = enabled,
        thumbTrackGapSize = thumbTrackGapSize,
        trackCornerSize = trackCornerSize
    )
}

private fun DrawScope.drawTrackIcon(
    icon: Painter,
    disabledIcon: Painter,
    iconSize: DpSize,
    iconPadding: Dp,
    yOffset: Float,
    activeTrackWidth: Float,
    inactiveTrackStart: Float,
    inactiveTrackWidth: Float,
    activeIconColor: Color,
    inactiveIconColor: Color
) {
    val iconSizePx = iconSize.toSize()
    val iconPaddingPx = iconPadding.toPx()
    val minSpaceForIcon = iconSizePx.width + iconPaddingPx * 2

    if (activeTrackWidth >= minSpaceForIcon) {
        translate(iconPaddingPx, yOffset) {
            with(icon) {
                draw(iconSizePx, colorFilter = ColorFilter.tint(activeIconColor))
            }
        }
    } else if (inactiveTrackWidth >= minSpaceForIcon) {
        translate(inactiveTrackStart + iconPaddingPx, yOffset) {
            with(disabledIcon) {
                draw(iconSizePx, colorFilter = ColorFilter.tint(inactiveIconColor))
            }
        }
    }
}