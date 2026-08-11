package com.mardous.booming.core.appwidgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.mardous.booming.core.appwidgets.component.progressTickInterval
import com.mardous.booming.core.appwidgets.state.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/** Decides when widgets are pushed */
class WidgetPresenter(
    host: Context,
    private val scope: CoroutineScope,
    private val playback: Playback
) {

    interface Playback {
        val isPlaying: Boolean
        val positionMs: Long

        /** 0 while unknown */
        val durationMs: Long

        suspend fun snapshot(needs: Set<WidgetData>): PlaybackState
    }

    private val context by lazy(LazyThreadSafetyMode.NONE) { host.applicationContext }

    private var refreshJob: Job? = null
    private var tickerJob: Job? = null

    private var screenOn = true

    private var listening = false

    private val filter = IntentFilter().apply {
        addAction(Intent.ACTION_SCREEN_ON)
        addAction(Intent.ACTION_SCREEN_OFF)
        addAction(WidgetUpdater.ACTION_WIDGETS_CHANGED)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // A widget was placed or reconfigured
            if (intent?.action == WidgetUpdater.ACTION_WIDGETS_CHANGED) {
                refresh()
                return
            }
            screenOn = intent?.action == Intent.ACTION_SCREEN_ON
            if (screenOn && playback.isPlaying) {
                refreshPosition()
            }
            syncTicker()
        }
    }

    fun start() {
        if (listening) return
        screenOn = context.getSystemService<PowerManager>()?.isInteractive != false
        ContextCompat.registerReceiver(
            context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
        listening = true
    }

    fun stop() {
        if (!listening) return
        context.unregisterReceiver(receiver)
        refreshJob?.cancel()
        tickerJob?.cancel()
        listening = false
    }

    fun refresh() {
        syncTicker()
        refreshJob?.cancel()
        // Push immediately: service teardown can cancel a debounced stop update.
        if (!playback.isPlaying) {
            widgetScope.launch(Dispatchers.Main) {
                WidgetUpdater.push(context) { needs -> playback.snapshot(needs) }
            }
            return
        }
        refreshJob = scope.launch {
            delay(REFRESH_DEBOUNCE)
            WidgetUpdater.push(context) { needs -> playback.snapshot(needs) }
        }
    }

    fun refreshModes(shuffleMode: Boolean, repeatMode: Int) {
        scope.launch { WidgetUpdater.pushModes(context, shuffleMode, repeatMode) }
    }

    /** Only the timeline moved */
    fun refreshPosition() {
        if (!screenOn) return
        val duration = playback.durationMs.takeIf { it > 0 } ?: return
        val position = playback.positionMs
        scope.launch { WidgetUpdater.pushProgress(context, position, duration) }
    }

    /** ticker only runs with the screen on */
    private fun syncTicker() {
        tickerJob?.cancel()
        if (!playback.isPlaying || !screenOn) return
        tickerJob = scope.launch {
            if (WidgetData.Progress !in WidgetUpdater.placedNeeds(context)) return@launch
            while (isActive) {
                delay(progressTickInterval(playback.durationMs).milliseconds)
                refreshPosition()
            }
        }
    }

    private companion object {
        val REFRESH_DEBOUNCE = 300.milliseconds
    }
}
