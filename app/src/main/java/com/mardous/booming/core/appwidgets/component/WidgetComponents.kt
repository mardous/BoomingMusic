package com.mardous.booming.core.appwidgets.component

import android.view.KeyEvent
import androidx.media3.common.Player
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.mardous.booming.R
import com.mardous.booming.core.appwidgets.config.WidgetAction
import com.mardous.booming.core.appwidgets.cycleRepeatAction
import com.mardous.booming.core.appwidgets.playbackAction
import com.mardous.booming.core.appwidgets.toggleFavoriteAction
import com.mardous.booming.core.appwidgets.toggleShuffleAction
import com.mardous.booming.core.appwidgets.state.PlaybackState
import com.mardous.booming.core.appwidgets.WidgetColors

/** Each drawable reads the same dimen as [radiusRes] */
enum class WidgetShape(@DrawableRes val drawable: Int, @DimenRes val radiusRes: Int) {
    Stadium(R.drawable.widget_shape_stadium, R.dimen.widget_shape_stadium_radius),
    Button(R.drawable.widget_shape_button, R.dimen.widget_shape_button_radius),
    Scallop(R.drawable.widget_shape_scallop, R.dimen.glance_widget_background_radius),
    Card(R.drawable.widget_shape_card, R.dimen.glance_widget_background_radius)
}

fun GlanceModifier.fillBackground(color: ColorProvider, shape: WidgetShape): GlanceModifier =
    background(ImageProvider(shape.drawable), ContentScale.FillBounds, ColorFilter.tint(color))

/** Positions an overlay control; Glance has no offset modifier */
@Composable
fun Satellite(alignment: Alignment, content: @Composable () -> Unit) {
    Box(
        modifier = GlanceModifier.fillMaxSize().padding(SATELLITE_INSET),
        contentAlignment = alignment
    ) { content() }
}

private val ICON_SIZE = 24.dp

@Composable
fun WidgetIconButton(
    modifier: GlanceModifier,
    action: Action,
    backgroundColor: ColorProvider,
    iconColor: ColorProvider,
    @DrawableRes icon: Int,
    contentDescription: String,
    shape: WidgetShape
) {
    Box(
        modifier = modifier
            .fillBackground(backgroundColor, shape)
            .clickable(action),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(ICON_SIZE),
            colorFilter = ColorFilter.tint(iconColor)
        )
    }
}

/** Shows one skip button when both cannot fit */
@Composable
fun SatelliteControls(
    action: WidgetAction,
    playbackState: PlaybackState,
    colors: WidgetColors,
    available: Dp
) {
    val shape = WidgetShape.Stadium
    val button = GlanceModifier.size(SATELLITE_SKIP)
    if (action == WidgetAction.Skip) {
        if (available >= SATELLITE_SKIP * 2 + SATELLITE_GAP + SATELLITE_INSET * 2) {
            Row(modifier = GlanceModifier.height(SATELLITE_SKIP)) {
                PreviousButton(button, colors, shape)
                Spacer(GlanceModifier.width(SATELLITE_GAP))
                NextButton(button, colors, shape)
            }
        } else {
            NextButton(button, colors, shape)
        }
    } else {
        ActionButton(action, button, playbackState, colors, shape)
    }
}

@Composable
fun ActionButton(
    action: WidgetAction,
    modifier: GlanceModifier,
    playbackState: PlaybackState,
    colors: WidgetColors,
    shape: WidgetShape
) {
    when (action) {
        WidgetAction.Previous -> PreviousButton(modifier, colors, shape)
        // Two buttons, so only SatelliteControls can place it; anywhere else it is simply nothing
        WidgetAction.Skip -> Unit
        WidgetAction.Next -> NextButton(modifier, colors, shape)
        WidgetAction.Favourite -> FavouriteButton(modifier, playbackState, colors, shape)
        WidgetAction.Shuffle -> ShuffleButton(modifier, playbackState, colors, shape)
        WidgetAction.Repeat -> RepeatButton(modifier, playbackState, colors, shape)
        WidgetAction.None -> Unit
    }
}

@Composable
fun PreviousButton(modifier: GlanceModifier, colors: WidgetColors, shape: WidgetShape) {
    val context = LocalContext.current
    WidgetIconButton(
        modifier = modifier,
        action = playbackAction(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS),
        backgroundColor = colors.control,
        iconColor = colors.onControl,
        icon = R.drawable.ic_previous_m3_24dp,
        contentDescription = context.getString(R.string.action_previous),
        shape = shape
    )
}

@Composable
fun NextButton(modifier: GlanceModifier, colors: WidgetColors, shape: WidgetShape) {
    val context = LocalContext.current
    WidgetIconButton(
        modifier = modifier,
        action = playbackAction(context, KeyEvent.KEYCODE_MEDIA_NEXT),
        backgroundColor = colors.control,
        iconColor = colors.onControl,
        icon = R.drawable.ic_next_m3_24dp,
        contentDescription = context.getString(R.string.action_next),
        shape = shape
    )
}

@Composable
fun FavouriteButton(modifier: GlanceModifier, playbackState: PlaybackState, colors: WidgetColors, shape: WidgetShape) {
    val context = LocalContext.current
    val isFavorite = playbackState.isFavorite
    WidgetIconButton(
        modifier = modifier,
        action = toggleFavoriteAction(context),
        backgroundColor = colors.control,
        iconColor = colors.onControl,
        icon = if (isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp,
        contentDescription = context.getString(
            if (isFavorite) R.string.action_remove_from_favorites else R.string.action_add_to_favorites
        ),
        shape = shape
    )
}

@Composable
fun ShuffleButton(modifier: GlanceModifier, playbackState: PlaybackState, colors: WidgetColors, shape: WidgetShape) {
    val context = LocalContext.current
    WidgetIconButton(
        modifier = modifier,
        action = toggleShuffleAction(context),
        backgroundColor = colors.control,
        iconColor = colors.onControl,
        icon = if (playbackState.isShuffleMode) R.drawable.ic_shuffle_on_24dp else R.drawable.ic_shuffle_24dp,
        contentDescription = context.getString(R.string.action_toggle_shuffle),
        shape = shape
    )
}

@Composable
fun RepeatButton(modifier: GlanceModifier, playbackState: PlaybackState, colors: WidgetColors, shape: WidgetShape) {
    val context = LocalContext.current
    WidgetIconButton(
        modifier = modifier,
        action = cycleRepeatAction(context),
        backgroundColor = colors.control,
        iconColor = colors.onControl,
        icon = when (playbackState.repeatMode) {
            Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one_on_24dp
            Player.REPEAT_MODE_ALL -> R.drawable.ic_repeat_on_24dp
            else -> R.drawable.ic_repeat_24dp
        },
        contentDescription = context.getString(R.string.repeat_mode),
        shape = shape
    )
}

@Composable
fun PlayPauseButton(
    modifier: GlanceModifier,
    playbackState: PlaybackState,
    colors: WidgetColors,
    shape: WidgetShape
) {
    val context = LocalContext.current
    WidgetIconButton(
        modifier = modifier,
        action = playbackAction(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE),
        backgroundColor = colors.accent,
        iconColor = colors.onAccent,
        icon = if (playbackState.isPlaying) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp,
        contentDescription = context.getString(R.string.action_play_pause),
        shape = shape
    )
}
