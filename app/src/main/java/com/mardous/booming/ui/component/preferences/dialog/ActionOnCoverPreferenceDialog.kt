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
import android.content.SharedPreferences
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.core.model.action.NowPlayingAction
import com.mardous.booming.ui.component.compose.DialogListItemWithRadio
import com.mardous.booming.ui.theme.BoomingMusicTheme
import com.mardous.booming.util.COVER_DOUBLE_TAP_ACTION
import com.mardous.booming.util.COVER_LEFT_DOUBLE_TAP_ACTION
import com.mardous.booming.util.COVER_LONG_PRESS_ACTION
import com.mardous.booming.util.COVER_RIGHT_DOUBLE_TAP_ACTION
import com.mardous.booming.util.COVER_SINGLE_TAP_ACTION
import com.mardous.booming.util.Preferences
import org.koin.android.ext.android.get

/**
 * @author Christians M. A. (mardous)
 */
class ActionOnCoverPreferenceDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val prefKey = requireArguments().getString(EXTRA_KEY)
        checkNotNull(prefKey)

        val allActions = NowPlayingAction.entries.toMutableList()
        removeActionsForPrefKey(prefKey, allActions)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(
                ComposeView(requireContext()).apply {
                    setViewCompositionStrategy(
                        ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                    )
                    setContent {
                        BoomingMusicTheme {
                            DialogScreen(
                                actions = allActions,
                                selected = getCurrentAction(prefKey),
                                onActionClick = { action ->
                                    get<SharedPreferences>().edit {
                                        putString(prefKey, action.name)
                                    }
                                    dialog?.dismiss()
                                }
                            )
                        }
                    }
                }
            )
            .create()
    }

    @Composable
    private fun DialogScreen(
        actions: List<NowPlayingAction>,
        selected: NowPlayingAction,
        onActionClick: (NowPlayingAction) -> Unit
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.wrapContentHeight()
        ) {
            val firstVisibleIndex = actions.indexOfFirst { it.ordinal == selected.ordinal }
                .coerceAtLeast(0)

            LazyColumn(
                state = rememberLazyListState(firstVisibleIndex),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(vertical = 24.dp)
            ) {
                items(actions) { action ->
                    DialogListItemWithRadio(
                        title = stringResource(action.titleRes),
                        isSelected = action == selected,
                        onClick = { onActionClick(action) }
                    )
                }
            }
        }
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
            COVER_DOUBLE_TAP_ACTION to listOf(
                Preferences.coverLongPressAction,
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

        fun newInstance(preference: String): ActionOnCoverPreferenceDialog {
            return ActionOnCoverPreferenceDialog().apply {
                arguments = bundleOf(EXTRA_KEY to preference)
            }
        }
    }
}