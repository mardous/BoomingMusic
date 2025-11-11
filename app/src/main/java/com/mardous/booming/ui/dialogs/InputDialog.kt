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
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.mardous.booming.databinding.DialogInputBinding
import com.mardous.booming.extensions.create
import com.mardous.booming.extensions.extraNotNull
import com.mardous.booming.extensions.resources.animateToggle
import com.mardous.booming.extensions.resources.hide
import com.mardous.booming.extensions.resources.show
import com.mardous.booming.extensions.withArgs
import kotlinx.parcelize.Parcelize

typealias InputCallback = (DialogInterface, TextInputLayout, String?, Boolean) -> Boolean

/**
 * @author Christians M. A. (mardous)
 */
open class InputDialog : DialogFragment() {

    private val config: InputConfig by extraNotNull(EXTRA_INPUT_CONFIG)

    private var _binding: DialogInputBinding? = null
    private val binding get() = _binding!!

    private var inputCallback: InputCallback? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogInputBinding.inflate(layoutInflater)
        val actualConfig = inputConfig()
        if (actualConfig.message.isNullOrEmpty()) {
            binding.message.hide()
        } else {
            binding.message.text = actualConfig.message
        }
        if (!actualConfig.checkablePrompt.isNullOrEmpty()) {
            binding.checkboxPrompt.text = actualConfig.checkablePrompt
            binding.checkboxContainer.setOnClickListener {
                binding.checkbox.animateToggle()
            }
            binding.checkboxContainer.show()
        }
        if (actualConfig.inputType != null) {
            binding.editText.inputType = actualConfig.inputType
        }
        if (actualConfig.maxLength != null) {
            binding.inputLayout.isCounterEnabled = true
            binding.inputLayout.counterMaxLength = actualConfig.maxLength
        }
        binding.editText.hint = actualConfig.hint
        binding.editText.setText(actualConfig.prefill)
        return createInputDialog(actualConfig)
    }

    protected open fun createInputDialog(config: InputConfig): AlertDialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(config.title)
            .setView(binding.root)
            .setPositiveButton(config.positiveText, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create { dialog ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val text = binding.editText.text?.toString()
                    val isChecked = binding.checkboxContainer.isVisible && binding.checkbox.isChecked
                    if (processInput(dialog, text, isChecked)) {
                        dialog.dismiss()
                    }
                }
            }
    }

    protected open fun inputLayout() = binding.inputLayout

    protected open fun inputConfig() = config

    protected open fun processInput(dialog: DialogInterface, text: String?, isChecked: Boolean) =
        inputCallback?.invoke(dialog, binding.inputLayout, text, isChecked) == true

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
        inputCallback = null
    }

    @Parcelize
    class InputConfig internal constructor(
        val title: String,
        val positiveText: String,
        val message: String? = null,
        val hint: String? = null,
        val prefill: String? = null,
        val maxLength: Int? = null,
        val inputType: Int? = null,
        val checkablePrompt: String? = null
    ) : Parcelable

    class Builder(private val context: Context) {
        private var title: String? = null
        private var message: String? = null
        private var hint: String? = null
        private var prefill: String? = null
        private var maxLength: Int? = null
        private var inputType: Int? = null
        private var positiveText: String? = null
        private var checkablePrompt: String? = null

        fun title(title: Int) = title(context.getString(title))
        fun title(title: String) = apply { this.title = title }
        fun message(message: Int) = message(context.getString(message))
        fun message(message: String) = apply { this.message = message }
        fun hint(hint: Int) = hint(context.getString(hint))
        fun hint(hint: String) = apply { this.hint = hint }
        fun prefill(prefill: String) = apply { this.prefill = prefill }
        fun maxLength(maxLength: Int) = apply { this.maxLength = maxLength }
        fun inputType(inputType: Int) = apply { this.inputType = inputType }
        fun positiveText(positiveRes: Int) = positiveText(context.getString(positiveRes))
        fun positiveText(positiveText: String) = apply { this.positiveText = positiveText }
        fun checkablePrompt(checkablePrompt: Int) = checkablePrompt(context.getString(checkablePrompt))
        fun checkablePrompt(checkablePrompt: String) = apply { this.checkablePrompt = checkablePrompt }

        fun createConfig(): InputConfig {
            checkNotNull(title)
            if (positiveText.isNullOrEmpty()) {
                positiveText = context.getString(android.R.string.ok)
            }
            return InputConfig(
                title = title!!,
                positiveText = positiveText!!,
                message = message,
                hint = hint,
                maxLength = maxLength,
                prefill = prefill,
                inputType = inputType,
                checkablePrompt = checkablePrompt
            )
        }

        fun createDialog(inputCallback: InputCallback): InputDialog {
            return InputDialog().withArgs {
                putParcelable(EXTRA_INPUT_CONFIG, createConfig())
            }.apply { this.inputCallback = inputCallback }
        }
    }

    companion object {
        private const val EXTRA_INPUT_CONFIG = "extra_input_config"
    }
}