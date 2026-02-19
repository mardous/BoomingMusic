package com.mardous.booming.ui.component.preferences.dialog

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.preference.ListPreference
import androidx.preference.PreferenceDialogFragmentCompat
import com.mardous.booming.extensions.withArgs
import com.mardous.booming.ui.component.compose.DialogListItemWithRadio
import com.mardous.booming.ui.theme.BoomingMusicTheme

class SingleSelectionDialog : PreferenceDialogFragmentCompat() {

    private val listPreference: ListPreference
        get() = getPreference() as ListPreference

    private var entries: Array<CharSequence>? = null
    private var entryValues: Array<CharSequence>? = null

    private var clickedDialogEntryIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val preference: ListPreference = this.listPreference

            check(!(preference.entries == null || preference.entryValues == null)) {
                "ListPreference requires an entries array and an entryValues array."
            }

            clickedDialogEntryIndex = preference.findIndexOfValue(preference.value)
            entries = preference.entries
            entryValues = preference.entryValues
        } else {
            clickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, 0)
            entries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES)
            entryValues = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVE_STATE_INDEX, clickedDialogEntryIndex)
        outState.putCharSequenceArray(SAVE_STATE_ENTRIES, entries)
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, entryValues)
    }

    override fun onCreateDialogView(context: Context): View {
        return ComposeView(context).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                BoomingMusicTheme {
                    DialogView(
                        titles = entries?.map { it.toString() }.orEmpty(),
                        selectedIndex = clickedDialogEntryIndex,
                        onSelection = {
                            clickedDialogEntryIndex = it
                            dialog?.let { dialog ->
                                onClick(dialog, DialogInterface.BUTTON_POSITIVE)
                                dialog.dismiss()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onBindDialogView(view: View) {}

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)
        builder.setTitle(null)
        builder.setPositiveButton(null, null)
        builder.setNegativeButton(null, null)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && clickedDialogEntryIndex >= 0) {
            val value = entryValues!![clickedDialogEntryIndex].toString()
            val preference: ListPreference = this.listPreference
            if (preference.callChangeListener(value)) {
                preference.setValue(value)
            }
        }
    }

    @Composable
    private fun DialogView(
        titles: List<String>,
        selectedIndex: Int,
        onSelection: (Int) -> Unit
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.wrapContentHeight()
        ) {
            LazyColumn(
                state = rememberLazyListState(selectedIndex.coerceAtLeast(0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(vertical = 24.dp)
            ) {
                itemsIndexed(titles) { index, action ->
                    DialogListItemWithRadio(
                        title = action,
                        isSelected = selectedIndex == index,
                        onClick = {
                            onSelection(index)
                        }
                    )
                }
            }
        }
    }

    companion object {
        private const val SAVE_STATE_INDEX = "ListPreferenceDialogFragment.index"
        private const val SAVE_STATE_ENTRIES = "ListPreferenceDialogFragment.entries"
        private const val SAVE_STATE_ENTRY_VALUES = "ListPreferenceDialogFragment.entryValues"

        fun newInstance(key: String?): SingleSelectionDialog {
            return SingleSelectionDialog().withArgs {
                putString(ARG_KEY, key)
            }
        }
    }
}