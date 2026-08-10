package com.mardous.booming.core.appwidgets

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.mardous.booming.playback.Playback
import com.mardous.booming.playback.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "WidgetActionReceiver"

private const val CONNECT_TIMEOUT_MS = 5_000L

private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** Where widget buttons land */
class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val command = when (intent.action) {
            ACTION_TOGGLE_FAVORITE -> Playback.TOGGLE_FAVORITE
            ACTION_TOGGLE_SHUFFLE -> Playback.TOGGLE_SHUFFLE
            ACTION_CYCLE_REPEAT -> Playback.CYCLE_REPEAT
            else -> return
        }

        val appContext = context.applicationContext
        val pending = goAsync()
        widgetScope.launch {
            try {
                withSessionController(appContext) { controller ->
                    val result = controller
                        .sendCustomCommand(SessionCommand(command, Bundle.EMPTY), Bundle.EMPTY)
                        .await()
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
        private const val PACKAGE_NAME = "com.mardous.booming"

        const val ACTION_TOGGLE_FAVORITE = "$PACKAGE_NAME.widget.TOGGLE_FAVORITE"
        const val ACTION_TOGGLE_SHUFFLE = "$PACKAGE_NAME.widget.TOGGLE_SHUFFLE"
        const val ACTION_CYCLE_REPEAT = "$PACKAGE_NAME.widget.CYCLE_REPEAT"
    }
}
