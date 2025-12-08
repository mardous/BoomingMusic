package com.mardous.booming.core.appwidgets

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mardous.booming.R
import com.mardous.booming.core.appwidgets.state.PlaybackState
import com.mardous.booming.core.appwidgets.state.PlaybackStateDefinition
import com.mardous.booming.extensions.utilities.DEFAULT_INFO_DELIMITER
import com.mardous.booming.playback.PlaybackService
import com.mardous.booming.ui.screen.MainActivity

class BoomingGlanceWidget : GlanceAppWidget() {

    companion object {
        private val SMALL_LAYOUT_SIZE = DpSize(width = 120.dp, height = 60.dp)
        private val MEDIUM_LAYOUT_SIZE = DpSize(width = 240.dp, height = 120.dp)
        private val LARGE_LAYOUT_SIZE = DpSize(width = 360.dp, height = 240.dp)
        private val EXTRA_LARGE_LAYOUT_SIZE = DpSize(width = 360.dp, height = 300.dp)
    }

    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PlaybackStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playbackState = currentState<PlaybackState>()
            val currentSize = LocalSize.current

            GlanceTheme {
                when {
                    currentSize.height >= EXTRA_LARGE_LAYOUT_SIZE.height -> {
                        ExtraLargeWidget(context, playbackState)
                    }

                    currentSize.height >= LARGE_LAYOUT_SIZE.height -> {
                        LargeWidget(context, playbackState)
                    }

                    currentSize.height >= MEDIUM_LAYOUT_SIZE.height -> {
                        MediumWidget(context, playbackState)
                    }

                    else -> {
                        SmallWidget(context, playbackState)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallWidget(context: Context, playbackState: PlaybackState) {
    val surfaceColor = GlanceTheme.colors.surface
    val onSurfaceColor = GlanceTheme.colors.onSurface
    val onSurfaceVariantColor = GlanceTheme.colors.onSurfaceVariant

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable(actionStartActivity<MainActivity>())
            .background(surfaceColor),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtGlance(
            playbackState = playbackState,
            modifier = GlanceModifier
                .size(64.dp)
                .cornerRadius(8.dp)
        )

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                Text(
                    text = playbackState.currentTitle.orEmpty(),
                    style = TextStyle(color = onSurfaceColor, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                if (!playbackState.currentArtist.isNullOrEmpty()) {
                    Spacer(GlanceModifier.width(2.dp))

                    Text(
                        text = DEFAULT_INFO_DELIMITER,
                        style = TextStyle(color = onSurfaceColor),
                        maxLines = 1
                    )

                    Spacer(GlanceModifier.width(2.dp))

                    Text(
                        text = playbackState.currentArtist,
                        style = TextStyle(color = onSurfaceVariantColor),
                        maxLines = 1
                    )
                }
            }

            SmallControllerGlance(
                context = context,
                playbackState = playbackState,
                modifier = GlanceModifier.fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp)
            )
        }
    }
}

@Composable
private fun MediumWidget(context: Context, playbackState: PlaybackState) {
    val surfaceColor = GlanceTheme.colors.surface
    val onSurfaceColor = GlanceTheme.colors.onSurface
    val onSurfaceVariantColor = GlanceTheme.colors.onSurfaceVariant

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .padding(8.dp)
            .clickable(actionStartActivity<MainActivity>())
            .background(surfaceColor),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtGlance(
                playbackState = playbackState,
                modifier = GlanceModifier
                    .size(72.dp)
                    .cornerRadius(8.dp)
            )

            Column(
                modifier = GlanceModifier
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = playbackState.currentTitle.orEmpty(),
                    style = TextStyle(
                        color = onSurfaceColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    maxLines = 1
                )

                Text(
                    text = playbackState.currentArtist.orEmpty(),
                    style = TextStyle(
                        color = onSurfaceVariantColor,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
            }
        }

        MediumControllerGlance(
            context = context,
            playbackState = playbackState,
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = 4.dp)
        )
    }
}

@Composable
private fun LargeWidget(context: Context, playbackState: PlaybackState) {
    val surfaceColor = GlanceTheme.colors.surface
    val onSurfaceColor = GlanceTheme.colors.onSurface
    val onSurfaceVariantColor = GlanceTheme.colors.onSurfaceVariant

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>())
            .background(surfaceColor),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtGlance(
                playbackState = playbackState,
                modifier = GlanceModifier
                    .size(104.dp)
                    .cornerRadius(8.dp)
            )

            Column(
                modifier = GlanceModifier
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = playbackState.currentTitle.orEmpty(),
                    style = TextStyle(
                        color = onSurfaceColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    maxLines = 1
                )

                Text(
                    text = playbackState.currentArtist.orEmpty(),
                    style = TextStyle(
                        color = onSurfaceVariantColor,
                        fontSize = 16.sp
                    ),
                    maxLines = 1
                )
            }
        }

        MediumControllerGlance(
            context = context,
            playbackState = playbackState,
            playIconSize = 64.dp,
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp)
        )
    }
}

@Composable
private fun ExtraLargeWidget(context: Context, playbackState: PlaybackState) {
    val surfaceColor = GlanceTheme.colors.surface
    val onSurfaceColor = GlanceTheme.colors.onSurface
    val onSurfaceVariantColor = GlanceTheme.colors.onSurfaceVariant

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
            .cornerRadius(24.dp)
            .clickable(actionStartActivity<MainActivity>())
            .background(surfaceColor)
    ) {
        AlbumArtGlance(
            playbackState = playbackState,
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .cornerRadius(16.dp)
        )

        Spacer(GlanceModifier.height(24.dp))

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = playbackState.currentTitle.orEmpty(),
                style = TextStyle(
                    color = onSurfaceColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                maxLines = 1
            )

            Text(
                text = playbackState.currentArtist.orEmpty(),
                style = TextStyle(
                    color = onSurfaceVariantColor,
                    fontSize = 16.sp
                ),
                maxLines = 1
            )
        }

        Spacer(GlanceModifier.height(24.dp))

        MediumControllerGlance(
            context = context,
            playbackState = playbackState,
            playIconSize = 64.dp,
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp)
        )
    }
}

@Composable
private fun AlbumArtGlance(
    playbackState: PlaybackState,
    placeholderColor: ColorProvider = GlanceTheme.colors.tertiaryContainer,
    placeholderIconColor: ColorProvider = GlanceTheme.colors.onTertiaryContainer,
    modifier: GlanceModifier = GlanceModifier
) {
    val bitmap = playbackState.artworkData?.let {
        try {
            val options = BitmapFactory.Options()
            BitmapFactory.decodeByteArray(it, 0, it.size, options)
        } catch (t: Throwable) {
            Log.e("BoomingGlanceWidget", "Cannot decode artwork bitmap", t)
            null
        }
    }
    if (bitmap != null) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(placeholderColor),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_music_note_24dp),
                colorFilter = ColorFilter.tint(placeholderIconColor),
                contentDescription = "Album Art",
                modifier = GlanceModifier.wrapContentSize()
            )
        }
    }
}

@Composable
private fun SmallControllerGlance(
    context: Context,
    playbackState: PlaybackState,
    modifier: GlanceModifier = GlanceModifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (playbackState.isForeground) {
            // Previous
            ControlIconGlance(
                resId = R.drawable.ic_previous_24dp,
                tint = GlanceTheme.colors.onSurface,
                contentDescription = "Previous",
                modifier = GlanceModifier
                    .clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
            )
        }

        Spacer(GlanceModifier.defaultWeight())

        // Play/pause
        ControlIconGlance(
            resId = if (playbackState.isPlaying) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp,
            tint = GlanceTheme.colors.onSurface,
            contentDescription = "Play/Pause",
            modifier = GlanceModifier.clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        )

        Spacer(GlanceModifier.defaultWeight())

        if (playbackState.isForeground) {
            // Next
            ControlIconGlance(
                resId = R.drawable.ic_next_24dp,
                tint = GlanceTheme.colors.onSurface,
                contentDescription = "Next",
                modifier = GlanceModifier
                    .clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_NEXT))
            )
        }
    }
}

@Composable
private fun MediumControllerGlance(
    context: Context,
    playbackState: PlaybackState,
    playIconSize: Dp = 48.dp,
    modifier: GlanceModifier = GlanceModifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (playbackState.isForeground) {
            // Shuffle
            ControlIconGlance(
                resId = if (playbackState.isShuffleMode) R.drawable.ic_shuffle_on_24dp else R.drawable.ic_shuffle_24dp,
                tint = GlanceTheme.colors.onSurface,
                contentDescription = "Toggle shuffle mode",
                modifier = GlanceModifier
                    .size(28.dp)
                    .clickable(toggleShuffleAction(context))
            )

            Spacer(GlanceModifier.defaultWeight())

            // Previous
            ControlIconGlance(
                resId = R.drawable.ic_previous_24dp,
                tint = GlanceTheme.colors.onSurface,
                contentDescription = "Previous",
                modifier = GlanceModifier
                    .size(32.dp)
                    .clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
            )
        }

        Spacer(GlanceModifier.defaultWeight())

        // Play/pause
        CircularControlIconGlance(
            resId = if (playbackState.isPlaying) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp,
            size = playIconSize,
            iconTint = GlanceTheme.colors.onPrimaryContainer,
            backgroundTint = GlanceTheme.colors.primaryContainer,
            contentDescription = "Play/Pause",
            onClick = GlanceModifier.clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        )

        Spacer(GlanceModifier.defaultWeight())

        if (playbackState.isForeground) {
            // Next
            ControlIconGlance(
                resId = R.drawable.ic_next_24dp,
                tint = GlanceTheme.colors.onSurface,
                contentDescription = "Next",
                modifier = GlanceModifier
                    .size(32.dp)
                    .clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_NEXT))
            )

            Spacer(GlanceModifier.defaultWeight())

            // Favorite
            ControlIconGlance(
                resId = if (playbackState.isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp,
                tint = GlanceTheme.colors.onSurface,
                contentDescription = "Toggle favorite",
                modifier = GlanceModifier
                    .size(28.dp)
                    .clickable(toggleFavoriteAction(context))
            )
        }
    }
}

@Composable
private fun ControlIconGlance(
    resId: Int,
    tint: ColorProvider,
    contentDescription: String,
    modifier: GlanceModifier = GlanceModifier
) {
    Image(
        provider = ImageProvider(resId),
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = ColorFilter.tint(tint)
    )
}

@Composable
private fun CircularControlIconGlance(
    resId: Int,
    size: Dp,
    iconTint: ColorProvider,
    backgroundTint: ColorProvider,
    contentDescription: String,
    onClick: GlanceModifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .size(size)
            .cornerRadius(size / 2)
            .padding(size / 6)
            .background(backgroundTint)
    ) {
        Image(
            provider = ImageProvider(resId),
            contentDescription = contentDescription,
            modifier = GlanceModifier
                .fillMaxSize()
                .then(onClick),
            colorFilter = ColorFilter.tint(iconTint)
        )
    }
}

private fun playbackAction(context: Context, mediaKeyCode: Int): Action {
    val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
    intent.setComponent(ComponentName(context, PlaybackService::class.java))
    intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, mediaKeyCode))
    return actionStartService(intent, true)
}

private fun toggleShuffleAction(context: Context): Action {
    val intent = Intent(PlaybackService.ACTION_TOGGLE_SHUFFLE)
    intent.setComponent(ComponentName(context, PlaybackService::class.java))
    return actionStartService(intent)
}

private fun toggleFavoriteAction(context: Context): Action {
    val intent = Intent(PlaybackService.ACTION_TOGGLE_FAVORITE)
    intent.setComponent(ComponentName(context, PlaybackService::class.java))
    return actionStartService(intent)
}