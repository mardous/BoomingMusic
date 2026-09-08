package com.mardous.booming.core.appwidgets.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import com.mardous.booming.extensions.dp
import com.mardous.booming.core.appwidgets.state.PlaybackState
import com.mardous.booming.core.appwidgets.WidgetColors

private val TRACK_HEIGHT = 18.dp

private const val TICKS_PER_SONG = 90
private const val MIN_TICK_INTERVAL = 2_000L
private const val MAX_TICK_INTERVAL = 10_000L

// Scale updates with song length so the handle moves visibly without excessive redraws
fun progressTickInterval(durationMs: Long): Long =
    if (durationMs <= 0) MAX_TICK_INTERVAL
    else (durationMs / TICKS_PER_SONG).coerceIn(MIN_TICK_INTERVAL, MAX_TICK_INTERVAL)

/** Rasterized because Glance cannot draw the wavy track. Not that clean but looks cool */
@Composable
fun WavyProgress(
    playbackState: PlaybackState,
    colors: WidgetColors,
    trackWidth: Dp,
    modifier: GlanceModifier = GlanceModifier
) {
    val resources = LocalContext.current.resources
    val density = resources.displayMetrics.density
    val widthPx = trackWidth.value.dp(resources)
    val heightPx = TRACK_HEIGHT.value.dp(resources)

    Box(
        modifier = modifier.fillMaxWidth().height(TRACK_HEIGHT),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(
                WaveRenderer.remaining(widthPx, heightPx, playbackState.progress, density)
            ),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(colors.trackRemaining),
            modifier = GlanceModifier.fillMaxSize()
        )

        Image(
            provider = ImageProvider(
                WaveRenderer.played(
                    widthPx = widthPx,
                    heightPx = heightPx,
                    progress = playbackState.progress,
                    isPlaying = playbackState.isPlaying,
                    density = density
                )
            ),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(colors.accent),
            modifier = GlanceModifier.fillMaxSize()
        )
    }
}
