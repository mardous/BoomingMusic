package com.mardous.booming.core.appwidgets

import android.content.Context
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import com.mardous.booming.R
import com.mardous.booming.core.appwidgets.state.PlaybackState
import com.mardous.booming.core.appwidgets.state.PlaybackStateDefinition
import com.mardous.booming.ui.screen.MainActivity

class CardWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PlaybackStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playbackState = currentState<PlaybackState>()

            GlanceTheme(
                colors = playbackState.widgetTheme.getColors()
            ) {
                CardWidgetContent(context, playbackState)
            }
        }
    }

    @Composable
    private fun CardWidgetContent(context: Context, playbackState: PlaybackState) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(top = 8.dp, bottom = 32.dp, start = 8.dp, end = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                AlbumArtGlance(
                    playbackState = playbackState,
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(playbackState.imageCornerRadius?.dp?.times(2) ?: 8.dp)
                )
            }

            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp)
                    .cornerRadius(16.dp)
                    .background(GlanceTheme.colors.surface),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Prev
                    ControlIconGlance(
                        resId = R.drawable.ic_previous_24dp,
                        tint = GlanceTheme.colors.onSurface,
                        contentDescription = "Previous",
                        modifier = GlanceModifier
                            .size(24.dp)
                            .clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                    )

                    Spacer(GlanceModifier.defaultWeight())

                    // Play/Pause circular
                    CircularControlIconGlance(
                        resId = if (playbackState.isPlaying)
                            R.drawable.ic_pause_24dp
                        else
                            R.drawable.ic_play_24dp,
                        size = 48.dp,
                        iconTint = GlanceTheme.colors.onPrimary,
                        backgroundTint = GlanceTheme.colors.primary,
                        contentDescription = "Play/Pause",
                        onClick = GlanceModifier
                            .clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                    )

                    Spacer(GlanceModifier.defaultWeight())

                    // Next
                    ControlIconGlance(
                        resId = R.drawable.ic_next_24dp,
                        tint = GlanceTheme.colors.onSurface,
                        contentDescription = "Next",
                        modifier = GlanceModifier
                            .size(24.dp)
                            .clickable(playbackAction(context, KeyEvent.KEYCODE_MEDIA_NEXT))
                    )
                }
            }
        }
    }
}