package com.mardous.booming.core.appwidgets.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.layout.fillMaxWidth
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.mardous.booming.R
import com.mardous.booming.core.appwidgets.state.PlaybackState
import com.mardous.booming.core.appwidgets.WidgetColors


@Composable
fun TrackText(
    playbackState: PlaybackState,
    colors: WidgetColors,
    titleSize: TextUnit,
    artistSize: TextUnit,
    align: TextAlign = TextAlign.Start,
    titleLines: Int = 1,
    showArtist: Boolean = true
) {
    TrackTitle(
        text = playbackState.titleOrIdle(),
        colors = colors,
        modifier = GlanceModifier.fillMaxWidth(),
        fontSize = titleSize,
        align = align,
        maxLines = titleLines
    )
    if (showArtist) {
        TrackArtist(
            text = playbackState.artistOrIdle(),
            colors = colors,
            modifier = GlanceModifier.fillMaxWidth(),
            fontSize = artistSize,
            align = align
        )
    }
}

@Composable
private fun PlaybackState.titleOrIdle(): String =
    currentTitle?.takeIf { it.isNotEmpty() }
        ?: LocalContext.current.getString(R.string.app_widget_nothing_playing)

@Composable
private fun PlaybackState.artistOrIdle(): String =
    currentArtist?.takeIf { it.isNotEmpty() }
        ?: LocalContext.current.getString(R.string.app_widget_tap_to_play)


@Composable
private fun TrackTitle(
    text: String,
    colors: WidgetColors,
    modifier: GlanceModifier,
    fontSize: TextUnit,
    align: TextAlign,
    maxLines: Int
) {
    Text(
        text = text,
        style = TextStyle(
            color = colors.onSurface,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = align
        ),
        maxLines = maxLines,
        modifier = modifier
    )
}

@Composable
private fun TrackArtist(
    text: String,
    colors: WidgetColors,
    modifier: GlanceModifier,
    fontSize: TextUnit,
    align: TextAlign
) {
    Text(
        text = text,
        style = TextStyle(color = colors.artist, fontSize = fontSize, textAlign = align),
        maxLines = 1,
        modifier = modifier
    )
}
