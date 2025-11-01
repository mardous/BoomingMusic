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
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.core.model.action.NowPlayingAction
import com.mardous.booming.util.COVER_DOUBLE_TAP_ACTION
import com.mardous.booming.util.COVER_LONG_PRESS_ACTION
import com.mardous.booming.util.COVER_SINGLE_TAP_ACTION
import com.mardous.booming.util.Preferences
import org.koin.android.ext.android.get

/**
 * @author Christians M. A. (mardous)
 */
class ActionOnCoverPreferenceDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val prefKey = requireArguments().getString(EXTRA_KEY)
        val current = getCurrentAction(prefKey)

        val actions = NowPlayingAction.entries.toMutableList()
        makeCleanActions(prefKey, actions)

        val dialogTitle = arguments?.getCharSequence(EXTRA_TITLE)
        val actionNames = actions.map { getString(it.titleRes) }
        var selectedIndex = actions.indexOf(current).coerceAtLeast(0)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(dialogTitle)
            .setSingleChoiceItems(actionNames.toTypedArray(), selectedIndex) { _: DialogInterface, selected: Int ->
                selectedIndex = selected
            }
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                get<SharedPreferences>().edit {
                    putString(prefKey, actions[selectedIndex].name)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun makeCleanActions(prefKey: String?, actions: MutableList<NowPlayingAction>) {
        if (COVER_DOUBLE_TAP_ACTION == prefKey) {
            actions.remove(Preferences.coverLongPressAction)
            actions.remove(Preferences.coverSingleTapAction)
        }

        if (COVER_LONG_PRESS_ACTION == prefKey) {
            actions.remove(Preferences.coverDoubleTapAction)
            actions.remove(Preferences.coverSingleTapAction)
        }

        if (COVER_SINGLE_TAP_ACTION == prefKey) {
            actions.remove(Preferences.coverDoubleTapAction)
            actions.remove(Preferences.coverLongPressAction)
        }

        if (!actions.contains(NowPlayingAction.Nothing)) {
            // "Nothing" must be always available, so if we
            // removed it previously, add it again.
            actions.add(NowPlayingAction.Nothing)
        }
    }

    private fun getCurrentAction(prefKey: String?): NowPlayingAction {
        return if (COVER_DOUBLE_TAP_ACTION == prefKey) {
            Preferences.coverDoubleTapAction
        }else if (COVER_LONG_PRESS_ACTION == prefKey) {
            Preferences.coverLongPressAction
        }else {
            Preferences.coverSingleTapAction
        }
    }

    companion object {
        private const val EXTRA_KEY = "extra_key"
        private const val EXTRA_TITLE = "extra_title"

        fun newInstance(preference: String, title: CharSequence): ActionOnCoverPreferenceDialog {
            return ActionOnCoverPreferenceDialog().apply {
                arguments = bundleOf(EXTRA_KEY to preference, EXTRA_TITLE to title)
            }
        }
    }
}