package com.mardous.booming.core.appwidgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.compose
import com.mardous.booming.core.appwidgets.config.WidgetConfigStore
import com.mardous.booming.core.appwidgets.state.PlaybackState
import com.mardous.booming.core.appwidgets.state.PlaybackStateDefinition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object WidgetUpdater {

    /** Package-internal nudge a stopped service simply has nobody listening. */
    const val ACTION_WIDGETS_CHANGED = "com.mardous.booming.widget.WIDGETS_CHANGED"

    private const val TAG = "WidgetUpdater"

    private val renderLock = Mutex()

    private val widgets = widgetsByReceiver.values

    private class Placed(
        val widget: BoomingWidget,
        val glanceId: GlanceId,
        val needs: Set<WidgetData>
    )

    private suspend fun placed(context: Context): List<Placed> {
        val manager = GlanceAppWidgetManager(context)
        return buildList {
            for (widget in widgets) {
                for (glanceId in manager.getGlanceIds(widget.javaClass)) {
                    val appWidgetId = manager.getAppWidgetId(glanceId)
                    val config = WidgetConfigStore.read(context, appWidgetId, widget.settings)
                    add(Placed(widget, glanceId, config.dataNeeds(widget.settings)))
                }
            }
        }
    }

    suspend fun placedNeeds(context: Context): Set<WidgetData> = withContext(Dispatchers.IO) {
        placed(context).needs()
    }

    private fun List<Placed>.needs() = flatMapTo(mutableSetOf()) { it.needs }

    /** Fills the store without repainting */
    internal suspend fun ensureData(context: Context, ownNeeds: Set<WidgetData>) =
        withContext(Dispatchers.IO) {
            val needs = placed(context).needs() + ownNeeds
            runCatching {
                PlaybackStateDefinition.getDataStore(context, "").updateData {
                    WidgetDataSource.enrich(context, it, needs)
                }
            }.onFailure { Log.e(TAG, "Couldn't prepare widget data", it) }
            Unit
        }

    /** Recomputes everything the placed widgets need and repaints them */
    suspend fun refresh(context: Context) = withContext(Dispatchers.IO) {
        val targets = placed(context)
        if (targets.isEmpty()) return@withContext
        val needs = targets.needs()
        publishTo(context, targets) { WidgetDataSource.enrich(context, it, needs) }
        notifyWidgetsChanged(context)
    }

    /** Nothing receives this unless playback is running */
    fun notifyWidgetsChanged(context: Context) {
        context.sendBroadcast(Intent(ACTION_WIDGETS_CHANGED).setPackage(context.packageName))
    }

    suspend fun push(context: Context, snapshot: suspend (Set<WidgetData>) -> PlaybackState) {
        val targets = withContext(Dispatchers.IO) { placed(context) }
        if (targets.isEmpty()) return
        val state = snapshot(targets.needs())
        withContext(Dispatchers.IO) {
            publishTo(context, targets) { persisted ->
                // Keep the last known timeline until the player reports a duration.
                if (state.durationMs > 0L) state
                else state.copy(positionMs = persisted.positionMs, durationMs = persisted.durationMs)
            }
        }
    }

    suspend fun pushProgress(context: Context, positionMs: Long, durationMs: Long) =
        publish(context, requires = WidgetData.Progress) {
            it.copy(positionMs = positionMs, durationMs = durationMs)
        }

    /** shuffle or repeat button must not wait for a snapshot */
    suspend fun pushModes(context: Context, shuffleMode: Boolean, repeatMode: Int) =
        publish(context) { it.copy(isShuffleMode = shuffleMode, repeatMode = repeatMode) }

    private suspend fun publish(
        context: Context,
        requires: WidgetData? = null,
        transform: suspend (PlaybackState) -> PlaybackState
    ) = withContext(Dispatchers.IO) {
        publishTo(context, placed(context).filter { requires == null || requires in it.needs }, transform)
    }

    private suspend fun publishTo(
        context: Context,
        targets: List<Placed>,
        transform: suspend (PlaybackState) -> PlaybackState
    ) {
        if (targets.isEmpty()) return
        try {
            var changed = false
            PlaybackStateDefinition.getDataStore(context, "").updateData { persisted ->
                transform(persisted).also { changed = it != persisted }
            }
            if (!changed) return
            for (target in targets) {
                target.widget.update(context, target.glanceId)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Couldn't update Glance widgets", e)
        }
    }

    suspend fun render(context: Context, widget: BoomingWidget, glanceId: GlanceId) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        // Concurrent compose calls replace each other's unmanaged Glance sessions
        val views = renderLock.withLock {
            widget.compose(
                context = context,
                id = glanceId,
                // Exact-size widgets need the host's current options
                options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            )
        }
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
