package com.mardous.booming.ui.component.preferences.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.core.model.action.QueueClearingBehavior
import com.mardous.booming.ui.component.compose.DialogListItem
import com.mardous.booming.ui.theme.BoomingMusicTheme
import com.mardous.booming.util.Preferences

/**
 * @author Christians M. A. (mardous)
 */
class ClearQueueActionPreferenceDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.on_clear_queue_title)
            .setView(
                ComposeView(requireContext()).apply {
                    setViewCompositionStrategy(
                        ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                    )
                    setContent {
                        BoomingMusicTheme {
                            var selectedAction by remember {
                                mutableStateOf(Preferences.clearQueueAction)
                            }
                            QueueDialogScreen(
                                selected = selectedAction,
                                onActionClick = { action ->
                                    selectedAction = action
                                    Preferences.clearQueueAction = action
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
    private fun QueueDialogScreen(
        selected: QueueClearingBehavior,
        onActionClick: (QueueClearingBehavior) -> Unit
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.wrapContentHeight()
        ) {
            val actions = remember { QueueClearingBehavior.entries.toTypedArray() }
            val firstVisibleIndex = actions.indexOfFirst { it.ordinal == selected.ordinal }
                .coerceAtLeast(0)

            val listState = rememberLazyListState(firstVisibleIndex)
            var maxItemHeight by remember { mutableIntStateOf(0) }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 24.dp)
            ) {
                items(actions) { action ->
                    DialogListItem(
                        title = stringResource(action.titleRes),
                        subtitle = stringResource(action.summaryRes),
                        leadingIcon = painterResource(action.iconRes),
                        isSelected = action == selected,
                        onClick = { onActionClick(action) },
                        modifier = Modifier
                            .then(
                                if (maxItemHeight > 0)
                                    Modifier.height(with(LocalDensity.current) { maxItemHeight.toDp() })
                                else Modifier
                            )
                            .onGloballyPositioned {
                                val height = it.size.height
                                if (height > maxItemHeight) {
                                    maxItemHeight = height
                                }
                            }
                    )
                }
            }
        }
    }
}