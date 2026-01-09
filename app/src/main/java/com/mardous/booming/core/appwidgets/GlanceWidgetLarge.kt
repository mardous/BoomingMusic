package com.mardous.booming.core.appwidgets

import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.mardous.booming.playback.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext

data class TrackInfo(val title: String, val artist: String)

class GlanceWidgetLarge : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (track, isPlaying, albumArt) = withContext(Dispatchers.IO) {
            val controller = getMediaController(context)
            val track = getCurrentTrack(controller)
            val isPlaying = isPlaybackActive(controller)
            val albumArt = getAlbumArt(controller)
            controller.release()
            Triple(track, isPlaying, albumArt)
        }

        provideContent {
            GlanceWidgetLargeContent(track, isPlaying, albumArt)
        }
    }
}

@Composable
fun GlanceWidgetLargeContent(
    track: TrackInfo,
    isPlaying: Boolean,
    albumArt: ImageProvider?
) {
    val backgroundColor = ColorProvider(
        day = Color(0xFFECECEC),
        night = Color(0xFF1E1E1E)
    )

    val iconTint = ColorProvider(
        day = Color.Black,
        night = Color.White
    )
    val textColor = iconTint

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        albumArt?.let {
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
                .height(64.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = track.title,
                style = TextStyle(color = textColor, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Text(
                text = track.artist,
                style = TextStyle(color = textColor, fontSize = 12.sp),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ControlIcon(android.R.drawable.ic_media_previous, "Previous", PrevAction::class.java, iconTint)
                Spacer(GlanceModifier.width(16.dp))
                ControlIcon(
                    if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    "Play/Pause", PlayPauseAction::class.java, iconTint
                )
                Spacer(GlanceModifier.width(16.dp))
                ControlIcon(android.R.drawable.ic_media_next, "Next", NextAction::class.java, iconTint)
            }
        }
    }
}

@Composable
private fun ControlIcon(resId: Int, desc: String, action: Class<out ActionCallback>, tint: ColorProvider) {
    Image(
        provider = ImageProvider(resId),
        contentDescription = desc,
        modifier = GlanceModifier
            .size(32.dp)
            .clickable(actionRunCallback(action)),
        colorFilter = ColorFilter.tint(tint)
    )
}

// --- Media3 Helpers ---

private suspend fun getMediaController(context: Context): MediaController {
    val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
    return MediaController.Builder(context, token).buildAsync().await()
}

private fun getCurrentTrack(controller: MediaController): TrackInfo {
    val metadata = controller.mediaMetadata
    return TrackInfo(
        title = metadata.title?.toString() ?: "Unknown",
        artist = metadata.artist?.toString() ?: "Unknown"
    )
}

private fun getAlbumArt(controller: MediaController): ImageProvider? {
    val artworkData = controller.mediaMetadata.artworkData ?: return null
    val bitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)
    return bitmap?.let { ImageProvider(it) }
}

private fun isPlaybackActive(controller: MediaController): Boolean {
    return controller.playWhenReady && controller.playbackState == Player.STATE_READY
}

// --- Control Actions ---

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withController(context) { controller ->
            if (controller.isPlaying) controller.pause() else controller.play()
            controller.release()
        }
    }
}

class PrevAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withController(context) { it.seekToPrevious(); it.release() }
    }
}

class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withController(context) { it.seekToNext(); it.release() }
    }
}

// --- Controller Helper ---

private suspend fun withController(
    context: Context,
    action: (MediaController) -> Unit,
) {
    val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
    val controller = MediaController.Builder(context, sessionToken).buildAsync().await()
    action(controller)
}
