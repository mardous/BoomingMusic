/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.ui.dialogs

import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.getSystemService
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.mardous.booming.R
import com.mardous.booming.databinding.DialogSleepTimerBinding
import com.mardous.booming.extensions.create
import com.mardous.booming.extensions.hasS
import com.mardous.booming.extensions.media.durationStr
import com.mardous.booming.extensions.requireAlertDialog
import com.mardous.booming.extensions.showToast
import com.mardous.booming.service.MusicService
import com.mardous.booming.service.constants.ServiceAction
import com.mardous.booming.ui.screen.player.PlayerViewModel
import com.mardous.booming.util.Preferences
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class SleepTimerDialog : DialogFragment() {

    private val playerViewModel: PlayerViewModel by activityViewModel()

    private val am: AlarmManager by lazy { requireContext().getSystemService()!! }
    private var _binding: DialogSleepTimerBinding? = null
    private val binding get() = _binding!!

    private lateinit var timerUpdater: TimerUpdater

    private var seekBarProgress = 0f

    override fun onDismiss(dialog: DialogInterface) {
        binding.slider.clearOnChangeListeners()
        binding.slider.clearOnSliderTouchListeners()
        super.onDismiss(dialog)
        timerUpdater.cancel()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        seekBarProgress = Preferences.lastSleepTimerValue.toFloat()

        _binding = DialogSleepTimerBinding.inflate(layoutInflater).apply {
            shouldFinishLastSong.isChecked = Preferences.isSleepTimerFinishMusic
            slider.apply {
                value = seekBarProgress
                setLabelFormatter { value ->
                    value.toInt().toString()
                }
                addOnChangeListener { slider, value, _ ->
                    if (value < 1) {
                        slider.value = 1f
                    } else {
                        seekBarProgress = value
                        updateTimeDisplayTime()
                    }
                }
                addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                    override fun onStartTrackingTouch(slider: Slider) {
                        timerDisplay.animate()
                            .alpha(0f)
                            .setDuration(100)
                            .withEndAction { timerDisplay.isInvisible = true }
                    }

                    override fun onStopTrackingTouch(slider: Slider) {
                        timerDisplay.animate()
                            .alpha(1f)
                            .setDuration(150)
                            .withStartAction { timerDisplay.isInvisible = false }

                        Preferences.lastSleepTimerValue = slider.value.toInt()
                    }
                })
            }
        }

        updateTimeDisplayTime()
        timerUpdater = TimerUpdater()
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_sleep_timer)
            .setView(binding.root)
            .setPositiveButton(R.string.sleep_timer_set_action) { _: DialogInterface, _: Int ->
                Preferences.isSleepTimerFinishMusic = binding.shouldFinishLastSong.isChecked
                if (hasS() && !am.canScheduleExactAlarms()) {
                    try {
                        startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                .setData(Uri.fromParts("package", requireContext().packageName, null))
                        )
                    } catch (_: ActivityNotFoundException) {}
                } else {
                    scheduleExactAlarm()
                }
            }
            .setNeutralButton(R.string.sleep_timer_cancel_current_timer) { _: DialogInterface, _: Int ->
                val previous = makeTimerPendingIntent(PendingIntent.FLAG_NO_CREATE)
                if (previous != null) {
                    val am = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    am.cancel(previous)
                    previous.cancel()

                    showToast(R.string.sleep_timer_canceled)
                }

                if (playerViewModel.pendingQuit) {
                    playerViewModel.pendingQuit = false
                    showToast(R.string.sleep_timer_canceled)
                }
            }
            .create { dialog ->
                if (makeTimerPendingIntent(PendingIntent.FLAG_NO_CREATE) != null) {
                    dialog.getButton(DialogInterface.BUTTON_NEUTRAL).isVisible = true
                    timerUpdater.start()
                } else {
                    dialog.getButton(DialogInterface.BUTTON_NEUTRAL).isVisible = false
                }
            }
    }

    private fun scheduleExactAlarm() {
        val minutes = seekBarProgress.toInt()
        val pi = makeTimerPendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
        if (pi != null) {
            val nextSleepTimerElapsedTime = (SystemClock.elapsedRealtime() + minutes * 60 * 1000)
                .also { elapsed -> Preferences.nextSleepTimerElapsedRealTime = elapsed }

            am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextSleepTimerElapsedTime, pi)
            showToast(resources.getString(R.string.sleep_timer_set, minutes))
        }
    }

    private fun updateTimeDisplayTime() {
        binding.timerDisplay.text = "${seekBarProgress.toInt()} min"
    }

    private fun makeTimerPendingIntent(flag: Int): PendingIntent? {
        return PendingIntent.getService(requireContext(), 0, makeTimerIntent(), flag or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun makeTimerIntent(): Intent {
        return Intent(activity, MusicService::class.java).apply {
            action = if (binding.shouldFinishLastSong.isChecked)
                ServiceAction.ACTION_PENDING_QUIT
            else ServiceAction.ACTION_QUIT
        }
    }

    private fun updateCancelButton() {
        if (playerViewModel.pendingQuit) {
            requireAlertDialog().getButton(DialogInterface.BUTTON_NEUTRAL)
                .setText(R.string.sleep_timer_cancel_current_timer)
        } else {
            requireAlertDialog().getButton(DialogInterface.BUTTON_NEUTRAL).isVisible = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private inner class TimerUpdater :
        CountDownTimer(Preferences.nextSleepTimerElapsedRealTime - SystemClock.elapsedRealtime(), 1000) {

        override fun onTick(millisUntilFinished: Long) {
            requireAlertDialog().getButton(DialogInterface.BUTTON_NEUTRAL).text =
                getString(R.string.sleep_timer_cancel_current_timer_x, millisUntilFinished.durationStr())
        }

        override fun onFinish() {
            updateCancelButton()
        }
    }

    companion object {
        private const val SCHEDULE_EXACT_ALARM_REQUEST = 200
    }
}