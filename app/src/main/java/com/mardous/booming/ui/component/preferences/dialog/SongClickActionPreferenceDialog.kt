package com.mardous.booming.ui.component.preferences.dialog

import android.app.Dialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.core.model.action.SongClickBehavior
import com.mardous.booming.ui.component.compose.DialogListItemWithRadio
import com.mardous.booming.ui.theme.BoomingMusicTheme
import com.mardous.booming.util.Preferences

class SongClickActionPreferenceDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.on_song_click_title)
            .setView(
                ComposeView(requireContext()).apply {
                    setViewCompositionStrategy(
                        ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                    )
                    setContent {
                        BoomingMusicTheme {
                            var selectedAction by remember {
                                mutableStateOf(Preferences.songClickAction)
                            }
                            SongDialogScreen(
                                selected = selectedAction,
                                onActionClick = { action ->
                                    selectedAction = action
                                    Preferences.songClickAction = action
                                }
                            )
                        }
                    }
                }
            )
            .setNegativeButton(R.string.close_action, null)
            .create()
    }

    @Composable
    private fun SongDialogScreen(
        selected: SongClickBehavior,
        onActionClick: (SongClickBehavior) -> Unit
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.wrapContentHeight()
        ) {
            val actions = remember { SongClickBehavior.entries.toTypedArray() }
            val firstVisibleIndex = actions.indexOfFirst { it.ordinal == selected.ordinal }
                .coerceAtLeast(0)

            LazyColumn(
                state = rememberLazyListState(firstVisibleIndex),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 24.dp)
            ) {
                items(actions) { action ->
                    DialogListItemWithRadio(
                        title = stringResource(action.titleRes),
                        subtitle = stringResource(action.summaryRes),
                        isSelected = action == selected,
                        onClick = { onActionClick(action) }
                    )
                }
            }
        }
    }
}