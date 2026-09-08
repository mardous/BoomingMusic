package com.mardous.booming.core.appwidgets.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import com.mardous.booming.core.appwidgets.config.WidgetAction
import com.mardous.booming.core.appwidgets.state.PlaybackState
import com.mardous.booming.core.appwidgets.WidgetColors

private val SATELLITE_PLAY = 52.dp
internal val SATELLITE_SKIP = 42.dp
internal val SATELLITE_GAP = 6.dp

internal val SATELLITE_INSET = 6.dp

@Composable
fun RoundWidget(
    playbackState: PlaybackState,
    colors: WidgetColors,
    showControls: Boolean,
    satellite: WidgetAction,
    controlsAt: Alignment,
    playAt: Alignment,
    playShape: WidgetShape,
    surface: @Composable (diameter: Dp) -> Unit
) {
    val size = LocalSize.current
    val diameter = min(size.width, size.height)

    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(modifier = GlanceModifier.size(diameter)) {
            surface(diameter)

            if (showControls) {
                Satellite(controlsAt) {
                    SatelliteControls(
                        action = satellite,
                        playbackState = playbackState,
                        colors = colors,
                        available = diameter
                    )
                }
                Satellite(playAt) {
                    PlayPauseButton(
                        modifier = GlanceModifier.size(SATELLITE_PLAY),
                        playbackState = playbackState,
                        colors = colors,
                        shape = playShape
                    )
                }
            }
        }
    }
}
