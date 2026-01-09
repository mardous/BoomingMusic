package com.mardous.booming.ui.screen.sleeptimer

import android.content.Context
import androidx.lifecycle.ViewModel
import com.mardous.booming.R
import com.mardous.booming.extensions.showToast
import com.mardous.booming.playback.SleepTimer
import com.mardous.booming.util.Preferences

class SleepTimerViewModel: ViewModel() {

    fun startTimer(context: Context, sleepTimer: SleepTimer) {
    if (sleepTimer.canScheduleExactAlarm()) {
        val minutes = Preferences.lastSleepTimerValue.toLong()
        sleepTimer.set(
            millisInFuture = minutes * 60 * 1000,
            allowPendingQuit = Preferences.isSleepTimerFinishMusic
        )
        context.showToast(context.resources.getString(R.string.sleep_timer_set, minutes))
    } else {
        sleepTimer.launchExactAlarmPermissionRequest()
    }
}

    fun cancelTimer(context: Context, sleepTimer: SleepTimer) {
        if (sleepTimer.cancel()) {
            context.showToast(R.string.sleep_timer_canceled)
        }
    }
}