package com.mardous.booming.core.appwidgets

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
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
    }

    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PlaybackStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playbackState = currentState<PlaybackState>()
            val currentSize = LocalSize.current

            GlanceTheme {
                when {
                    currentSize.height >= MEDIUM_LAYOUT_SIZE.height -> {
                        MediumWidget(context, playbackState)
                    }

                    currentSize.height >= LARGE_LAYOUT_SIZE.height -> {
                        LargeWidget(context, playbackState)
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

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>())
            .background(surfaceColor)
    ) {
        AlbumArtGlance(
            playbackState = playbackState,
            modifier = GlanceModifier.fillMaxSize()
        )

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {


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
private fun AlbumArtGlance(
    playbackState: PlaybackState,
    placeholderColor: ColorProvider = GlanceTheme.colors.tertiaryContainer,
    placeholderIconColor: ColorProvider = GlanceTheme.colors.onTertiaryContainer,
    modifier: GlanceModifier = GlanceModifier
) {
    playbackState.artworkUri?.let {
        Image(
            provider = ImageProvider(it.toUri()),
            contentDescription = "Album Art",
            modifier = modifier
        )
    } ?: Box(
        modifier = modifier.background(placeholderColor),
        contentAlignment = Alignment.Center
    ) {
        val size = LocalSize.current
        Image(
            provider = ImageProvider(R.drawable.ic_music_note_24dp),
            colorFilter = ColorFilter.tint(placeholderIconColor),
            contentDescription = "Album Art",
            modifier = GlanceModifier.size(width = size.width, height = size.height)
        )
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
        // Previous
        ControlIconGlance(
            resId = R.drawable.ic_previous_24dp,
            tint = GlanceTheme.colors.onSurface,
            contentDescription = "Previous",
            onClick = GlanceModifier.clickable {
                withController(context) { controller ->
                    controller.seekToPrevious()
                    controller.play()
                }
            }
        )

        Spacer(GlanceModifier.defaultWeight())

        // Play/pause
        ControlIconGlance(
            resId = if (playbackState.isPlaying) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp,
            tint = GlanceTheme.colors.onSurface,
            contentDescription = "Play/Pause",
            onClick = GlanceModifier.clickable {
                withController(context) { controller ->
                    if (controller.isPlaying) controller.pause() else controller.play()
                }
            }
        )

        Spacer(GlanceModifier.defaultWeight())

        // Next
        ControlIconGlance(
            resId = R.drawable.ic_next_24dp,
            tint = GlanceTheme.colors.onSurface,
            contentDescription = "Next",
            onClick = GlanceModifier.clickable {
                withController(context) { controller ->
                    controller.seekToNext()
                    controller.play()
                }
            }
        )
    }
}

@Composable
private fun MediumControllerGlance(
    context: Context,
    playbackState: PlaybackState,
    modifier: GlanceModifier = GlanceModifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
        ControlIconGlance(
            resId = if (playbackState.isShuffleMode) R.drawable.ic_shuffle_on_24dp else R.drawable.ic_shuffle_24dp,
            tint = GlanceTheme.colors.onSurface,
            contentDescription = "Toggle shuffle mode",
            onClick = GlanceModifier.clickable {

            }
        )

        Spacer(GlanceModifier.defaultWeight())

        // Previous
        ControlIconGlance(
            resId = R.drawable.ic_previous_24dp,
            tint = GlanceTheme.colors.onSurface,
            contentDescription = "Previous",
            onClick = GlanceModifier
                .size(28.dp)
                .clickable {
                    withController(context) { controller ->
                        controller.seekToPrevious()
                        controller.play()
                    }
            }
        )

        Spacer(GlanceModifier.defaultWeight())

        // Play/pause
        CircularControlIconGlance(
            resId = if (playbackState.isPlaying) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp,
            iconTint = GlanceTheme.colors.onPrimaryContainer,
            backgroundTint = GlanceTheme.colors.primaryContainer,
            contentDescription = "Play/Pause",
            onClick = GlanceModifier.clickable {
                withController(context) { controller ->
                    if (controller.isPlaying) controller.pause() else controller.play()
                }
            }
        )

        Spacer(GlanceModifier.defaultWeight())

        // Next
        ControlIconGlance(
            resId = R.drawable.ic_next_24dp,
            tint = GlanceTheme.colors.onSurface,
            contentDescription = "Next",
            onClick = GlanceModifier
                .size(28.dp)
                .clickable {
                    withController(context) { controller ->
                        controller.seekToNext()
                        controller.play()
                    }
            }
        )

        Spacer(GlanceModifier.defaultWeight())

        // Favorite
        ControlIconGlance(
            resId = if (playbackState.isFavorite) R.drawable.ic_favorite_24dp else R.drawable.ic_favorite_outline_24dp,
            tint = GlanceTheme.colors.onSurface,
            contentDescription = "Toggle favorite",
            onClick = GlanceModifier.clickable {

            }
        )
    }
}

@Composable
private fun ControlIconGlance(
    resId: Int,
    tint: ColorProvider,
    contentDescription: String,
    onClick: GlanceModifier
) {
    Image(
        provider = ImageProvider(resId),
        contentDescription = contentDescription,
        modifier = GlanceModifier
            .size(24.dp)
            .then(onClick),
        colorFilter = ColorFilter.tint(tint)
    )
}

@Composable
private fun CircularControlIconGlance(
    resId: Int,
    iconTint: ColorProvider,
    backgroundTint: ColorProvider,
    contentDescription: String,
    onClick: GlanceModifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .size(48.dp)
            .cornerRadius(24.dp)
            .padding(8.dp)
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

// --- Controller Helper ---

private fun withController(
    context: Context,
    action: (MediaController) -> Unit,
) {
    val sessionToken = SessionToken(context.applicationContext, ComponentName(context, PlaybackService::class.java))
    val controllerFuture = MediaController.Builder(context.applicationContext, sessionToken).buildAsync()
    controllerFuture.addListener(
        {
            val controller = controllerFuture.get()
            action(controller)
            controller.release()
        },
        MoreExecutors.directExecutor()
    )
}
