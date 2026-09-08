package com.mardous.booming.playback

import android.os.SystemClock
import com.mardous.booming.extensions.media.asReadableDuration
import com.mardous.booming.ui.screen.sleeptimer.SleepTimerWaitingFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SleepTimer {

    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null

    private var nextElapsedTimeRealTime: Long = -1
    private var shouldConsumePendingQuit: Boolean = false

    private var sleepParams = SleepParams()
    private val listeners = LinkedHashSet<(SleepParams) -> Unit>()

    private val _waitingFor = MutableStateFlow<SleepTimerWaitingFor?>(null)
    val waitingFor = _waitingFor.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning get() = _isRunning.asStateFlow()

    private fun onAlarm() {
        val params: SleepParams
        val currentListeners: List<(SleepParams) -> Unit>
        synchronized(lock) {
            params = sleepParams
            currentListeners = listeners.toList()
            nextElapsedTimeRealTime = -1
            shouldConsumePendingQuit = params.pendingQuit
            setRunning(shouldConsumePendingQuit)
            if (shouldConsumePendingQuit) {
                setWaitingFor(SleepTimerWaitingFor.PendingQuit)
            } else {
                setWaitingFor(null)
            }
        }
        currentListeners.forEach { it(params) }
    }

    fun set(millisInFuture: Long, allowPendingQuit: Boolean, fadeOut: Boolean, fadeDuration: Long) {
        synchronized(lock) {
            timerJob?.cancel()

            this.sleepParams = SleepParams(
                pendingQuit = allowPendingQuit,
                fadeOut = fadeOut,
                fadeDuration = fadeDuration
            )
            this.nextElapsedTimeRealTime = SystemClock.elapsedRealtime() + millisInFuture
            setRunning(true)

            timerJob = scope.launch {
                while (SystemClock.elapsedRealtime() < nextElapsedTimeRealTime) {
                    val remaining = nextElapsedTimeRealTime - SystemClock.elapsedRealtime()
                    setWaitingFor(
                        SleepTimerWaitingFor.Countdown(remaining.coerceAtLeast(0).asReadableDuration())
                    )
                    delay(1000.milliseconds)
                }
                onAlarm()
            }
        }
    }

    fun consumePendingQuit() {
        synchronized(lock) {
            if (nextElapsedTimeRealTime == -1L && shouldConsumePendingQuit) {
                sleepParams = sleepParams.copy(pendingQuit = false)
                shouldConsumePendingQuit = false
                setWaitingFor(null)
                setRunning(false)
            }
        }
    }

    fun cancel(): Boolean = synchronized(lock) {
        val active = nextElapsedTimeRealTime > -1 || sleepParams.pendingQuit
        if (active) {
            nextElapsedTimeRealTime = -1
            sleepParams = sleepParams.copy(pendingQuit = false)
            timerJob?.cancel()
            timerJob = null
            setWaitingFor(null)
            setRunning(false)
        }
        active
    }

    fun release() {
        cancel()
        synchronized(lock) {
            setRunning(false)
            listeners.clear()
            scope.cancel()
        }
    }

    fun addFinishListener(listener: (SleepParams) -> Unit) {
        synchronized(lock) {
            listeners.add(listener)
        }
    }

    private fun setRunning(isRunning: Boolean) {
        _isRunning.value = isRunning
    }

    private fun setWaitingFor(waitingFor: SleepTimerWaitingFor?) {
        _waitingFor.value = waitingFor
    }

    data class SleepParams(
        val pendingQuit: Boolean = false,
        val fadeOut: Boolean = false,
        val fadeDuration: Long = 5000
    )
}
