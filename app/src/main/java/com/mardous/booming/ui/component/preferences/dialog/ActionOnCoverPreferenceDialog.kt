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
import com.mardous.booming.util.*
import org.koin.android.ext.android.get

/**
 * @author Christians M. A. (mardous)
 */
class ActionOnCoverPreferenceDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val prefKey = requireArguments().getString(EXTRA_KEY)
        checkNotNull(prefKey)

        val currentAction = getCurrentAction(prefKey)
        val allActions = NowPlayingAction.entries.toMutableList()
        removeActionsForPrefKey(prefKey, allActions)

        val dialogTitle = arguments?.getCharSequence(EXTRA_TITLE)
        val actionNames = allActions.map { getString(it.titleRes) }
        var selectedIndex = allActions.indexOf(currentAction).coerceAtLeast(0)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(dialogTitle)
            .setSingleChoiceItems(actionNames.toTypedArray(), selectedIndex) { _: DialogInterface, selected: Int ->
                selectedIndex = selected
            }
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                get<SharedPreferences>().edit {
                    putString(prefKey, allActions[selectedIndex].name)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun removeActionsForPrefKey(prefKey: String, actions: MutableList<NowPlayingAction>) {
        val exclusivityMap = mapOf(
            COVER_SINGLE_TAP_ACTION to listOf(
                Preferences.coverDoubleTapAction,
                Preferences.coverLongPressAction,
                Preferences.coverLeftDoubleTapAction,
                Preferences.coverRightDoubleTapAction
            ),
            COVER_LONG_PRESS_ACTION to listOf(
                Preferences.coverDoubleTapAction,
                Preferences.coverSingleTapAction,
                Preferences.coverLeftDoubleTapAction,
                Preferences.coverRightDoubleTapAction
            ),
            COVER_LEFT_DOUBLE_TAP_ACTION to listOf(
                Preferences.coverDoubleTapAction,
                Preferences.coverLongPressAction,
                Preferences.coverSingleTapAction,
                Preferences.coverRightDoubleTapAction
            ),
            COVER_RIGHT_DOUBLE_TAP_ACTION to listOf(
                Preferences.coverDoubleTapAction,
                Preferences.coverLongPressAction,
                Preferences.coverSingleTapAction,
                Preferences.coverLeftDoubleTapAction
            )
        )

        exclusivityMap[prefKey]?.forEach {
            if (it != NowPlayingAction.Nothing) {
                actions.remove(it)
            }
        }
    }

    private fun getCurrentAction(prefKey: String) = when (prefKey) {
        COVER_DOUBLE_TAP_ACTION -> Preferences.coverDoubleTapAction
        COVER_LONG_PRESS_ACTION -> Preferences.coverLongPressAction
        COVER_LEFT_DOUBLE_TAP_ACTION -> Preferences.coverLeftDoubleTapAction
        COVER_RIGHT_DOUBLE_TAP_ACTION -> Preferences.coverRightDoubleTapAction
        else -> Preferences.coverSingleTapAction
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