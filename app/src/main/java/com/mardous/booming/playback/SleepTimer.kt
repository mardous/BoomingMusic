package com.mardous.booming.playback

import android.app.AlarmManager
import android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.mardous.booming.extensions.media.asReadableDuration
import com.mardous.booming.ui.screen.sleeptimer.SleepTimerWaitingFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SleepTimer(private val context: Context) : AlarmManager.OnAlarmListener {

    private val lock = Any()
    private val uiHandler = Handler(Looper.getMainLooper())
    private val am: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private var countDownTimer: TimerUpdater? = null
    private var nextElapsedTimeRealTime: Long = -1
    private var allowPendingQuit: Boolean = false
    private var shouldConsumePendingQuit: Boolean = false

    private val listeners = LinkedHashSet<(Boolean) -> Unit>()

    private val _waitingFor = MutableStateFlow<SleepTimerWaitingFor?>(null)
    val waitingFor = _waitingFor.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning get() = _isRunning.asStateFlow()

    override fun onAlarm() {
        synchronized(lock) {
            listeners.forEach { it(allowPendingQuit) }
            nextElapsedTimeRealTime = -1
            shouldConsumePendingQuit = allowPendingQuit
            setRunning(shouldConsumePendingQuit)
            if (shouldConsumePendingQuit) {
                setWaitingFor(SleepTimerWaitingFor.PendingQuit)
            } else {
                setWaitingFor(null)
            }
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
            setRunning(true)
        }
    }

    fun consumePendingQuit() {
        synchronized(lock) {
            if (nextElapsedTimeRealTime == -1L && shouldConsumePendingQuit) {
                allowPendingQuit = false
                shouldConsumePendingQuit = false
                setWaitingFor(null)
                setRunning(false)
            }
        }
    }

    fun cancel(): Boolean = synchronized(lock) {
        val active = nextElapsedTimeRealTime > -1 || allowPendingQuit
        if (active) {
            nextElapsedTimeRealTime = -1
            allowPendingQuit = false
            am.cancel(this)
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
        }
    }

    fun createTimerUpdater() = synchronized(lock) {
        uiHandler.post {
            if (countDownTimer == null && nextElapsedTimeRealTime > -1) {
                countDownTimer = TimerUpdater().apply { start() }
            }
        }
    }

    fun cancelTimerUpdater() = synchronized(lock) {
        uiHandler.post {
            countDownTimer?.cancel()
            countDownTimer = null
        }
    }

    fun addFinishListener(listener: (Boolean) -> Unit) {
        synchronized(lock) {
            listeners.add(listener)
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

    private fun setRunning(isRunning: Boolean) {
        _isRunning.value = isRunning
        if (isRunning) {
            createTimerUpdater()
        } else {
            cancelTimerUpdater()
        }
    }

    private fun setWaitingFor(waitingFor: SleepTimerWaitingFor?) {
        _waitingFor.value = waitingFor
    }

    private inner class TimerUpdater :
        CountDownTimer(nextElapsedTimeRealTime - SystemClock.elapsedRealtime(), 1000) {

        override fun onTick(millisUntilFinished: Long) {
            setWaitingFor(
                SleepTimerWaitingFor.Countdown(millisUntilFinished.asReadableDuration())
            )
        }

        override fun onFinish() {}
    }

    companion object {
        private const val TAG = "SleepTimer"
    }
}
