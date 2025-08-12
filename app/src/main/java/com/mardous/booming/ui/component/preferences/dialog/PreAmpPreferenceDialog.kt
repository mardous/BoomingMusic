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

package com.mardous.booming.ui.component.preferences.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.databinding.PreferenceDialogReplaygainPreampBinding
import com.mardous.booming.extensions.create
import com.mardous.booming.util.REPLAYGAIN_PREAMP_WITHOUT_TAG
import com.mardous.booming.util.REPLAYGAIN_PREAMP_WITH_TAG
import org.koin.android.ext.android.inject
import java.util.Locale

class PreAmpPreferenceDialog : DialogFragment(), OnSeekBarChangeListener {

    private var _binding: PreferenceDialogReplaygainPreampBinding? = null
    private val binding get() = _binding!!

    private val sharedPreferences: SharedPreferences by inject()

    private var withRgValue = 0f
    private var withoutRgValue = 0f

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = PreferenceDialogReplaygainPreampBinding.inflate(layoutInflater)

        withRgValue = sharedPreferences.getFloat(REPLAYGAIN_PREAMP_WITH_TAG, 0f)
        withoutRgValue = sharedPreferences.getFloat(REPLAYGAIN_PREAMP_WITHOUT_TAG, 0f)
        updateLabelWithRg()
        updateLabelWithoutRg()

        binding.seekbarWithRg.setOnSeekBarChangeListener(this)
        binding.seekbarWithRg.progress = ((withRgValue + 15) / 0.2f).toInt()
        binding.seekbarWithoutRg.setOnSeekBarChangeListener(this)
        binding.seekbarWithoutRg.progress = ((withoutRgValue + 15) / 0.2f).toInt()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.replaygain_preamp_title)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                sharedPreferences.edit {
                    putFloat(REPLAYGAIN_PREAMP_WITH_TAG, withRgValue)
                    putFloat(REPLAYGAIN_PREAMP_WITHOUT_TAG, withoutRgValue)
                }
            }
            .setNeutralButton(R.string.reset_action, null)
            .create { dialog ->
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
                    binding.seekbarWithRg.progress = binding.seekbarWithRg.max / 2
                    binding.seekbarWithoutRg.progress = binding.seekbarWithoutRg.max / 2
                }
            }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (seekBar === binding.seekbarWithRg) {
            withRgValue = progress * 0.2f - 15.0f
            updateLabelWithRg()
        } else if (seekBar === binding.seekbarWithoutRg) {
            withoutRgValue = progress * 0.2f - 15.0f
            updateLabelWithoutRg()
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {}

    private fun updateLabelWithRg() {
        binding.labelWithRg.text = String.format(Locale.getDefault(), "%+.1f%s", withRgValue, "dB")
    }

    private fun updateLabelWithoutRg() {
        binding.labelWithoutRg.text = String.format(Locale.getDefault(), "%+.1f%s", withoutRgValue, "dB")
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}