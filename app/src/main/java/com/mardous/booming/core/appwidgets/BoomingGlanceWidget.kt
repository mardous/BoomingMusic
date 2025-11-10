package com.mardous.booming.core.appwidgets

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
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
import com.mardous.booming.playback.PlaybackService
import com.mardous.booming.ui.screen.MainActivity

class BoomingGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PlaybackStateDefinition()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playbackState = currentState<PlaybackState>()

            GlanceTheme {
                LargeWidget(playbackState)
            }
        }
    }
}

@Composable
fun LargeWidget(playbackState: PlaybackState) {
    val surfaceColor = GlanceTheme.colors.surface
    val onSurfaceColor = GlanceTheme.colors.onSurface
    val onSurfaceVariantColor = GlanceTheme.colors.onSurfaceVariant

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity<MainActivity>())
            .background(surfaceColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        playbackState.artworkUri?.let {
            Image(
                provider = it,
                contentDescription = "Album Art",
                modifier = GlanceModifier
                    .size(64.dp)
                    .cornerRadius(8.dp)
            )
        } ?: Box(
            modifier = GlanceModifier
                .size(64.dp)
                .cornerRadius(8.dp)
                .background(ColorProvider(day = Color(0xFF888888), night = Color(0xFF444444))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No Art",
                style = TextStyle(
                    color = ColorProvider(day = Color.White, night = Color.White) ,
                    fontSize = 10.sp
                )
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        Column(
            modifier = GlanceModifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = playbackState.currentTitle,
                style = TextStyle(color = onSurfaceColor, fontWeight = FontWeight.Bold),
                maxLines = 1
            )

            Text(
                text = playbackState.currentArtist,
                style = TextStyle(color = onSurfaceVariantColor, fontSize = 12.sp),
                maxLines = 1
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            Row(
                modifier = GlanceModifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous
                ControlIconGlance(
                    resId = R.drawable.ic_previous_24dp,
                    tint = onSurfaceColor,
                    contentDescription = "Previous",
                    action = PrevAction::class.java
                )

                Spacer(GlanceModifier.width(16.dp))

                // Play/pause
                ControlIconGlance(
                    resId = if (playbackState.isPlaying) R.drawable.ic_pause_24dp else R.drawable.ic_play_24dp,
                    tint = onSurfaceColor,
                    contentDescription = "Play/Pause",
                    action = PlayPauseAction::class.java
                )

                Spacer(GlanceModifier.width(16.dp))

                // Next
                ControlIconGlance(
                    resId = android.R.drawable.ic_media_next,
                    tint = onSurfaceColor,
                    contentDescription = "Next",
                    action = NextAction::class.java
                )
            }
        }
    }
}

@Composable
private fun ControlIconGlance(
    resId: Int,
    tint: ColorProvider,
    contentDescription: String,
    action: Class<out ActionCallback>
) {
    Image(
        provider = ImageProvider(resId),
        contentDescription = contentDescription,
        modifier = GlanceModifier
            .wrapContentSize()
            .clickable(actionRunCallback(action)),
        colorFilter = ColorFilter.tint(tint)
    )
}

// --- Control Actions ---
class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withController(context) { controller ->
            if (controller.isPlaying) controller.pause() else controller.play()
        }
    }
}

class PrevAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withController(context) { it.seekToPrevious() }
    }
}

class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withController(context) { it.seekToNext() }
    }
}

// --- Controller Helper ---

private fun withController(
    context: Context,
    action: (MediaController) -> Unit,
) {
    val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
    val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
    controllerFuture.addListener(
        {
            val controller = controllerFuture.get()
            action(controller)
            controller.release()
        },
        MoreExecutors.directExecutor()
    )
}
