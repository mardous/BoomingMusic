package com.mardous.booming.core.appwidgets

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.mardous.booming.core.appwidgets.config.SongSource
import com.mardous.booming.extensions.utilities.toEnum
import com.mardous.booming.playback.Playback
import com.mardous.booming.playback.Playback.PACKAGE_NAME
import com.mardous.booming.playback.PlaybackService
import com.mardous.booming.playback.buildPlayableMediaItem
import com.mardous.booming.playback.library.MediaIDs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "WidgetActionReceiver"

private const val CONNECT_TIMEOUT_MS = 5_000L

/** Where widget buttons land */
class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        var command: String? = null
        when (val action = intent.action) {
            ACTION_TOGGLE_FAVORITE -> command = Playback.TOGGLE_FAVORITE
            ACTION_TOGGLE_SHUFFLE -> command = Playback.TOGGLE_SHUFFLE
            ACTION_CYCLE_REPEAT -> command = Playback.CYCLE_REPEAT
            else -> if (action != ACTION_PLAY_SONG) return
        }

        val appContext = context.applicationContext
        val pending = goAsync()
        widgetScope.launch {
            try {
                withSessionController(appContext) { controller ->
                    when (command) {
                        Playback.TOGGLE_FAVORITE,
                        Playback.TOGGLE_SHUFFLE,
                        Playback.CYCLE_REPEAT -> {
                            val result = controller
                                .sendCustomCommand(SessionCommand(command, Bundle.EMPTY), Bundle.EMPTY)
                                .await()
                            if (result.resultCode == SessionResult.RESULT_SUCCESS) {
                                if (command == Playback.TOGGLE_FAVORITE) {
                                    WidgetUpdater.refresh(appContext)
                                } else {
                                    WidgetUpdater.pushModes(
                                        context = appContext,
                                        shuffleMode = result.extras.getBoolean(Playback.EXTRA_SHUFFLE_MODE),
                                        repeatMode = result.extras.getInt(Playback.EXTRA_REPEAT_MODE)
                                    )
                                }
                            }
                        }
                        else -> {
                            val songId = intent.getLongExtra(EXTRA_SONG_ID, -1L)
                            if (songId != -1L) {
                                val source = intent.getStringExtra(EXTRA_SONG_SOURCE)
                                    ?.toEnum<SongSource>() ?: SongSource.Recent

                                controller.setMediaItem(
                                    buildPlayableMediaItem(MediaIDs.getPathId(source.mediaId, songId))
                                )
                                controller.prepare()
                                controller.play()
                            }
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun withSessionController(
        context: Context,
        block: suspend (MediaController) -> Unit
    ) = withContext(Dispatchers.Main) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controller = try {
            val future = MediaController.Builder(context, token).buildAsync()
            try {
                withTimeout(CONNECT_TIMEOUT_MS.milliseconds) { future.await() }
            } catch (e: Exception) {
                future.cancel(true)
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't connect to the MediaSession", e)
            return@withContext
        }
        try {
            block(controller)
        } catch (e: Exception) {
            Log.e(TAG, "Widget command failed", e)
        } finally {
            controller.release()
        }
    }

    companion object {
        const val ACTION_PLAY_SONG = "$PACKAGE_NAME.widget.PLAY_SONG"
        const val EXTRA_SONG_ID = "${PACKAGE_NAME}.extra.SONG_ID"
        const val EXTRA_SONG_SOURCE = "${PACKAGE_NAME}.extra.SONG_SOURCE"

        const val ACTION_TOGGLE_FAVORITE = "$PACKAGE_NAME.widget.TOGGLE_FAVORITE"
        const val ACTION_TOGGLE_SHUFFLE = "$PACKAGE_NAME.widget.TOGGLE_SHUFFLE"
        const val ACTION_CYCLE_REPEAT = "$PACKAGE_NAME.widget.CYCLE_REPEAT"
    }
}
