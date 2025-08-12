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
import android.view.View
import android.widget.CheckBox
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.ui.adapters.SimpleItemAdapter
import com.mardous.booming.databinding.DialogMultiCheckBinding
import com.mardous.booming.extensions.create
import com.mardous.booming.extensions.extraNotNull
import com.mardous.booming.extensions.resources.hide
import com.mardous.booming.extensions.withArgs
import kotlinx.parcelize.Parcelize
import kotlin.properties.Delegates

typealias MultiCheckCallback2 = (DialogInterface, List<Int>, List<String>) -> Boolean

/**
 * @author Christians M. A. (mardous)
 */
open class MultiCheckDialog : DialogFragment(), SimpleItemAdapter.Callback<String> {

    private val extraConfig: MultiCheckConfig by extraNotNull(EXTRA_MULTI_CHECK_CONFIG)

    private var _binding: DialogMultiCheckBinding? = null
    private val binding get() = _binding!!

    private var checkedPositions = arrayListOf<Int>()
    private var adapter: SimpleItemAdapter<String>? = null
    private var callback: MultiCheckCallback2? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogMultiCheckBinding.inflate(layoutInflater)
        val actualConfig = multiCheckConfig()
        if (actualConfig.message.isNullOrBlank()) {
            binding.message.hide()
        } else {
            binding.message.text = actualConfig.message
        }
        adapter = SimpleItemAdapter(
            R.layout.item_checkable,
            items = actualConfig.items,
            callback = this
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(actualConfig.title)
            .setView(binding.root)
            .setPositiveButton(actualConfig.positiveText, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create { dialog ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val checkedItems = adapter!!.items.filterIndexed { index, _ ->
                        checkedPositions.contains(index)
                    }
                    if (processMultiCheck(dialog, checkedPositions, checkedItems)) {
                        dialog.dismiss()
                    }
                }
            }
    }

    override fun itemClick(itemView: View, position: Int, item: String) {
        val checkBox = itemView.findViewById<CheckBox>(android.R.id.checkbox)
        checkBox.toggle()
        if (checkBox.isChecked) {
            checkedPositions.add(position)
        } else {
            checkedPositions.remove(position)
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    protected open fun processMultiCheck(
        dialog: DialogInterface,
        whichPos: List<Int>,
        whichItems: List<String>
    ) = callback?.invoke(dialog, whichPos, whichItems) == true

    protected open fun multiCheckConfig() = extraConfig

    @Parcelize
    class MultiCheckConfig internal constructor(
        val title: String,
        val positiveText: String,
        val message: String? = null,
        val items: List<String>
    ) : Parcelable

    class Builder(private val context: Context) {
        private var title: String? = null
        private var message: String? = null
        private var positiveText: String? = null
        private var items: List<String> by Delegates.notNull()

        fun title(title: Int) = title(context.getString(title))
        fun title(title: String) = apply { this.title = title }
        fun message(message: Int) = message(context.getString(message))
        fun message(message: String) = apply { this.message = message }
        fun positiveText(positiveRes: Int) = positiveText(context.getString(positiveRes))
        fun positiveText(positiveText: String) = apply { this.positiveText = positiveText }
        fun items(items: List<String>) = apply { this.items = items }
        fun items(items: Array<String>) = apply { this.items = items.toList() }

        fun createConfig(): MultiCheckConfig {
            checkNotNull(title)
            if (positiveText.isNullOrEmpty()) {
                positiveText = context.getString(android.R.string.ok)
            }
            if (items.isEmpty()) {
                throw IllegalArgumentException("Items cannot be empty")
            }
            return MultiCheckConfig(
                title = title!!,
                positiveText = positiveText!!,
                message = message,
                items = items
            )
        }

        fun createDialog(callback: MultiCheckCallback2): MultiCheckDialog {
            return MultiCheckDialog().withArgs {
                putParcelable(EXTRA_MULTI_CHECK_CONFIG, createConfig())
            }.apply {
                this.callback = callback
            }
        }
    }

    companion object {
        private const val EXTRA_MULTI_CHECK_CONFIG = "extra_multi_check_config"
    }
}