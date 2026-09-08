package com.mardous.booming.ui.component.preferences.dialog

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mardous.booming.extensions.withArgs
import com.mardous.booming.ui.component.compose.LanguageList
import com.mardous.booming.ui.component.compose.rememberLanguageEntries

class LanguageSelectionDialog : SingleSelectionDialog() {

    override fun onCreateDialogView(context: Context): View = composeDialogView(context) {
        LanguageList(
            entries = rememberLanguageEntries(),
            selectedTag = listPreference.value,
            onSelected = { selectValue(it.tag) },
            modifier = Modifier
                .wrapContentHeight()
                .padding(vertical = 24.dp)
        )
    }

    companion object {
        fun newInstance(key: String?): LanguageSelectionDialog {
            return LanguageSelectionDialog().withArgs {
                putString(ARG_KEY, key)
            }
        }
    }
}
