package com.mardous.booming.playback

import android.app.AlarmManager
import android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.SystemClock
import android.provider.Settings
import androidx.annotation.RequiresApi

class SleepTimer(private val context: Context) : AlarmManager.OnAlarmListener {

    private val lock = Any()
    private val am: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private var countDownTimer: TimerUpdater? = null
    private var nextElapsedTimeRealTime: Long = -1
    private var allowPendingQuit: Boolean = false
    private var shouldConsumePendingQuit: Boolean = false

    private var running: Boolean = false
        set(value) {
            field = value
            if (value) createTimerUpdater() else finishTimerUpdater()
        }

    private val tickListeners = LinkedHashSet<TickListener>()
    private val listeners = LinkedHashSet<(Boolean) -> Unit>()

    val isRunning get() = running
    val isPendingQuitMode get() = allowPendingQuit

    override fun onAlarm() {
        finishTimerUpdater()
        synchronized(lock) {
            listeners.forEach { it(allowPendingQuit) }
            nextElapsedTimeRealTime = -1
            shouldConsumePendingQuit = allowPendingQuit
            running = shouldConsumePendingQuit
        }
    }

    fun set(millisInFuture: Long, allowPendingQuit: Boolean) {
        synchronized(lock) {
            if (nextElapsedTimeRealTime > -1) {
                am.cancel(this)
            }
            this.allowPendingQuit = allowPendingQuit
            this.nextElapsedTimeRealTime = SystemClock.elapsedRealtime() + millisInFuture
            am.setExact(ELAPSED_REALTIME_WAKEUP, nextElapsedTimeRealTime, TAG, this, null)
            running = true
        }
    }

    fun consumePendingQuit() {
        synchronized(lock) {
            if (nextElapsedTimeRealTime == -1L && shouldConsumePendingQuit) {
                allowPendingQuit = false
                shouldConsumePendingQuit = false
                running = false
            }
        }
    }

    fun cancel(): Boolean = synchronized(lock) {
        val active = nextElapsedTimeRealTime > -1 || allowPendingQuit
        if (active) {
            nextElapsedTimeRealTime = -1
            allowPendingQuit = false
            am.cancel(this)
            running = false
        }
        active
    }

    fun release() {
        cancel()
        synchronized(lock) {
            running = false
            listeners.clear()
            tickListeners.clear()
        }
    }

    fun addFinishListener(listener: (Boolean) -> Unit) {
        synchronized(lock) {
            listeners.add(listener)
        }
    }

    fun addTickListener(listener: TickListener) {
        synchronized(lock) {
            tickListeners.add(listener)
            if (running) createTimerUpdater()
        }
    }

    fun removeTickListener(listener: TickListener) {
        synchronized(lock) {
            tickListeners.remove(listener)
            if (tickListeners.isEmpty()) cancelTimerUpdater()
        }
    }

    fun canScheduleExactAlarm(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

    @RequiresApi(Build.VERSION_CODES.S)
    fun launchExactAlarmPermissionRequest() {
        try {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: ActivityNotFoundException) {}
    }

    private fun createTimerUpdater() {
        if (countDownTimer == null && nextElapsedTimeRealTime > -1 && tickListeners.isNotEmpty()) {
            countDownTimer = TimerUpdater().apply { start() }
        }
    }

    private fun finishTimerUpdater() {
        synchronized(lock) {
            tickListeners.forEach { it.onTickerFinished() }
            cancelTimerUpdater()
        }
    }

    private fun cancelTimerUpdater() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    private inner class TimerUpdater :
        CountDownTimer(nextElapsedTimeRealTime - SystemClock.elapsedRealtime(), 1000) {

        override fun onTick(millisUntilFinished: Long) {
            synchronized(lock) {
                tickListeners.forEach { it.onTick(millisUntilFinished) }
            }
        }

        override fun onFinish() = finishTimerUpdater()
    }

    interface TickListener {
        fun onTick(millisUntilFinished: Long)
        fun onTickerFinished()
    }

    companion object {
        private const val TAG = "SleepTimer"
    }
}
