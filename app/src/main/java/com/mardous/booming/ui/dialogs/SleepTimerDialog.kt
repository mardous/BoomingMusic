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

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.mardous.booming.R
import com.mardous.booming.databinding.DialogSleepTimerBinding
import com.mardous.booming.extensions.create
import com.mardous.booming.extensions.media.asReadableDuration
import com.mardous.booming.extensions.requireAlertDialog
import com.mardous.booming.extensions.showToast
import com.mardous.booming.playback.SleepTimer
import com.mardous.booming.util.Preferences
import org.koin.android.ext.android.inject

class SleepTimerDialog : DialogFragment(), SleepTimer.TickListener {

    private var _binding: DialogSleepTimerBinding? = null
    private val binding get() = _binding!!

    private val sleepTimer: SleepTimer by inject()

    private var seekBarProgress = 0f

    override fun onDismiss(dialog: DialogInterface) {
        binding.slider.clearOnChangeListeners()
        binding.slider.clearOnSliderTouchListeners()
        super.onDismiss(dialog)
        sleepTimer.removeTickListener(this)
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
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_sleep_timer)
            .setView(binding.root)
            .setPositiveButton(R.string.sleep_timer_set_action) { _: DialogInterface, _: Int ->
                Preferences.isSleepTimerFinishMusic = binding.shouldFinishLastSong.isChecked
                if (sleepTimer.canScheduleExactAlarm()) {
                    val minutes = seekBarProgress.toLong()
                    sleepTimer.set(
                        millisInFuture = minutes * 60 * 1000,
                        allowPendingQuit = binding.shouldFinishLastSong.isChecked
                    )
                    showToast(resources.getString(R.string.sleep_timer_set, minutes))
                } else {
                    sleepTimer.launchExactAlarmPermissionRequest()
                }
            }
            .setNeutralButton(R.string.sleep_timer_cancel_current_timer) { _: DialogInterface, _: Int ->
                if (sleepTimer.cancel()) {
                    showToast(R.string.sleep_timer_canceled)
                }
            }
            .create { dialog ->
                sleepTimer.addTickListener(this)
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL).isVisible = sleepTimer.isRunning
            }
    }

    private fun updateTimeDisplayTime() {
        binding.timerDisplay.text = "${seekBarProgress.toInt()} min"
    }

    override fun onTick(millisUntilFinished: Long) {
        requireAlertDialog().getButton(DialogInterface.BUTTON_NEUTRAL).text =
            getString(R.string.sleep_timer_cancel_current_timer_x, millisUntilFinished.asReadableDuration())
    }

    override fun onTickerFinished() {
        if (sleepTimer.isPendingQuitMode) {
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

    companion object {
        private const val SCHEDULE_EXACT_ALARM_REQUEST = 200
    }
}