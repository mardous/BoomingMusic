package com.mardous.booming.core.appwidgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.media3.common.Player
import com.mardous.booming.R
import com.mardous.booming.core.appwidgets.state.PlaybackState
import com.mardous.booming.core.appwidgets.state.PlaybackStateDefinition
import com.mardous.booming.ui.component.compose.color.isDark
import com.mardous.booming.ui.screen.MainActivity

class FullWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PlaybackStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playbackState = currentState<PlaybackState>()
            val currentSize = LocalSize.current

            GlanceTheme(
                colors = playbackState.widgetTheme.getColors()
            ) {
                val primaryContainer = GlanceTheme.colors.primaryContainer
                FullWidgetContent(
                    context = context,
                    size = currentSize,
                    playbackState = playbackState,
                    scrimColor = primaryContainer,
                    onScrimColor = if (primaryContainer.getColor(context).isDark()) {
                        androidx.glance.color.ColorProvider(
                            day = androidx.compose.ui.graphics.Color.White,
                            night = androidx.compose.ui.graphics.Color.White
                        )
                    } else {
                        androidx.glance.color.ColorProvider(
                            day = androidx.compose.ui.graphics.Color.Black,
                            night = androidx.compose.ui.graphics.Color.Black
                        )
                    },
                    showAllActions = currentSize.width >= 220.dp
                )
            }
        }
    }

    @Composable
    private fun FullWidgetContent(
        context: Context,
        size: DpSize,
        playbackState: PlaybackState,
        scrimColor: ColorProvider,
        onScrimColor: ColorProvider,
        showAllActions: Boolean
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(playbackState.imageCornerRadius?.dp?.times(2) ?: 16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.BottomCenter
        ) {
            AlbumArtGlance(
                playbackState = playbackState,
                modifier = GlanceModifier.fillMaxSize()
            )

            Column(
                verticalAlignment = Alignment.Bottom,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(
                        imageProvider = ImageProvider(
                            createGradientScrim(
                                context = context,
                                color = scrimColor,
                                size = size
                            )
                        )
                    )
                    .padding(horizontal = 16.dp)
                    .padding(top = 32.dp, bottom = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = GlanceModifier.defaultWeight()
                    ) {
                        Text(
                            text = playbackState.currentTitle.orEmpty(),
                            style = TextStyle(
                                color = onScrimColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            maxLines = 1
                        )

                        Text(
                            text = playbackState.currentArtist.orEmpty(),
                            style = TextStyle(
                                color = onScrimColor,
                                fontSize = 16.sp
                            ),
                            maxLines = 1
                        )

                        if (!playbackState.additionalInfo.isNullOrEmpty()) {
                            Text(
                                text = playbackState.additionalInfo,
                                style = TextStyle(
                                    color = onScrimColor,
                                    fontSize = 14.sp
                                ),
                                maxLines = 2
                            )
                        }
                    }

                    if (playbackState.isForeground) {
                        Spacer(GlanceModifier.width(8.dp))

                        ControlIconGlance(
                            resId = if (playbackState.isFavorite) {
                                R.drawable.ic_favorite_24dp
                            } else {
                                R.drawable.ic_favorite_outline_24dp
                            },
                            tint = onScrimColor,
                            contentDescription = "Toggle favorite",
                            modifier = GlanceModifier
                                .size(28.dp)
                                .clickable(toggleFavoriteAction(context))
                        )
                    }
                }

                Spacer(GlanceModifier.height(16.dp))

                FullWidgetController(context, playbackState, onScrimColor, showAllActions)
            }
        }
    }

    @Composable
    private fun FullWidgetController(
        context: Context,
        playbackState: PlaybackState,
        controlsColor: ColorProvider,
        showAllActions: Boolean
    ) {
        Row(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            if (playbackState.isForeground) {
                if (showAllActions) {
                    // Shuffle
                    ControlIconGlance(
                        resId = if (playbackState.isShuffleMode) {
                            R.drawable.ic_shuffle_on_24dp
                        } else {
                            R.drawable.ic_shuffle_24dp
                        },
                        tint = controlsColor,
                        contentDescription = "Toggle shuffle mode",
                        modifier = GlanceModifier
                            .size(24.dp)
                            .clickable(toggleShuffleAction(context))
                    )

                    Spacer(GlanceModifier.defaultWeight())
                }

                // Previous
                ControlIconGlance(
                    resId = R.drawable.ic_previous_24dp,
                    tint = controlsColor,
                    contentDescription = "Previous",
                    modifier = GlanceModifier
                        .size(32.dp)
                        .clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                )
            }

            Spacer(GlanceModifier.defaultWeight())

            // Play/pause
            ShapeableControlIconGlance(
                resId = if (playbackState.isPlaying) {
                    R.drawable.ic_pause_24dp
                } else {
                    R.drawable.ic_play_24dp
                },
                size = 56.dp,
                contentDescription = "Play/Pause",
                onClick = GlanceModifier
                    .clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            )

            Spacer(GlanceModifier.defaultWeight())

            if (playbackState.isForeground) {
                // Next
                ControlIconGlance(
                    resId = R.drawable.ic_next_24dp,
                    tint = controlsColor,
                    contentDescription = "Next",
                    modifier = GlanceModifier
                        .size(32.dp)
                        .clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_NEXT))
                )

                if (showAllActions) {
                    Spacer(GlanceModifier.defaultWeight())

                    // Repeat
                    ControlIconGlance(
                        resId = when (playbackState.repeatMode) {
                            Player.REPEAT_MODE_ALL -> R.drawable.ic_repeat_on_24dp
                            Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one_on_24dp
                            else -> R.drawable.ic_repeat_24dp
                        },
                        tint = controlsColor,
                        contentDescription = "Cycle repeat mode",
                        modifier = GlanceModifier
                            .size(24.dp)
                            .clickable(cycleRepeatAction(context))
                    )
                }
            }
        }
    }

    fun createGradientScrim(context: Context, color: ColorProvider, size: DpSize): Bitmap {
        val width = size.width.toPx(context)
        val height = (size.height / 2).toPx(context)

        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        val gradient = LinearGradient(
            0f, height.toFloat(), 0f, 0f,
            color.getColor(context).toArgb(),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )

        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }
}