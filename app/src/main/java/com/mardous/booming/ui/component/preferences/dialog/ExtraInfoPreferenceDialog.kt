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
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.core.os.BundleCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.core.model.player.MetadataField
import com.mardous.booming.databinding.DialogRecyclerViewBinding
import com.mardous.booming.extensions.create
import com.mardous.booming.extensions.withArgs
import com.mardous.booming.ui.adapters.preference.ExtraInfoAdapter
import com.mardous.booming.util.NOW_PLAYING_EXTRA_INFO
import com.mardous.booming.util.Preferences
import com.mardous.booming.util.WIDGET_THIRD_LINE_CONTENT

class ExtraInfoPreferenceDialog : DialogFragment() {

    private lateinit var adapter: ExtraInfoAdapter

    @Suppress("DEPRECATION")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val arguments = requireArguments()

        val title = requireNotNull(arguments.getString(TITLE))
        val preferenceKey = requireNotNull(arguments.getString(PREFERENCE_KEY))
        val defaultContent = requireNotNull(
                BundleCompat.getParcelableArrayList(arguments, DEFAULT_CONTENT, MetadataField::class.java)
        )

        var currentContent = Preferences.getExtraInfoContent(preferenceKey, defaultContent)
        if (savedInstanceState != null && savedInstanceState.containsKey(SAVED_KEY)) {
            currentContent =
                BundleCompat.getParcelableArrayList(savedInstanceState, SAVED_KEY, MetadataField::class.java)!!
        }

        adapter = ExtraInfoAdapter(currentContent.toMutableList())

        val binding = DialogRecyclerViewBinding.inflate(layoutInflater)
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter = adapter
        adapter.attachToRecyclerView(binding.recyclerView)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                Preferences.setExtraInfoContent(preferenceKey, adapter.items)
            }
            .setNeutralButton(R.string.reset_action, null)
            .create { dialog ->
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
                    adapter.items = defaultContent.toMutableList()
                }
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(SAVED_KEY, ArrayList(adapter.items))
    }

    companion object {
        private const val TITLE = "Dialog.title"
        private const val PREFERENCE_KEY = "Preference.key"
        private const val DEFAULT_CONTENT = "Preference.default"
        private const val SAVED_KEY = "SavedKey.list"

        fun nowPlaying(context: Context) = create(
            title = context.getString(R.string.select_extra_info_title),
            preferenceKey = NOW_PLAYING_EXTRA_INFO,
            defaultContent = Preferences.getDefaultNowPlayingInfo()
        )

        fun appWidgets(context: Context) = create(
            title = context.getString(R.string.widget_third_line_title),
            preferenceKey = WIDGET_THIRD_LINE_CONTENT,
            defaultContent = Preferences.getDefaultWidgetInfo()
        )

        private fun create(
            title: String,
            preferenceKey: String,
            defaultContent: List<MetadataField>
        ) = ExtraInfoPreferenceDialog().withArgs {
            putString(TITLE, title)
            putString(PREFERENCE_KEY, preferenceKey)
            putParcelableArrayList(DEFAULT_CONTENT, ArrayList(defaultContent))
        }
    }
}