package com.mardous.booming.ui.component.preferences.dialog

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mardous.booming.extensions.LanguageLabel
import com.mardous.booming.extensions.languageLabelOf
import com.mardous.booming.extensions.withArgs

class LanguageSelectionDialog : SingleSelectionDialog() {

    private var languages: List<LanguageLabel> = emptyList()

    override fun onCreateDialogView(context: Context): View {
        languages = entryValues?.map { context.languageLabelOf(it.toString()) }.orEmpty()
        return super.onCreateDialogView(context)
    }

    override fun subtitleFor(index: Int): String? = languages.getOrNull(index)?.name

    override fun leadingFor(index: Int): (@Composable () -> Unit)? {
        val flag = languages.getOrNull(index)?.flag ?: return null
        return { LanguageFlag(flag) }
    }

    @Composable
    private fun LanguageFlag(flag: String) {
        Text(
            text = flag,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(32.dp)
                .clearAndSetSemantics {}
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
