package com.mardous.booming.ui.component.compose

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mardous.booming.R
import com.mardous.booming.extensions.languageEndonym
import com.mardous.booming.extensions.languageLabelOf
import com.mardous.booming.util.AUTO_LANGUAGE

class LanguageEntry(val tag: String, val title: String, val subtitle: String, val flag: String)

fun Context.languageTitle(tag: String): String =
    if (tag == AUTO_LANGUAGE) getString(R.string.system_default) else tag.languageEndonym()

/** Every entry costs several ICU lookups, so resolve the whole list only when it is shown. */
fun Context.languageEntries(): List<LanguageEntry> =
    resources.getStringArray(R.array.pref_language_codes).map { tag ->
        val label = languageLabelOf(tag)
        LanguageEntry(
            tag = tag,
            title = languageTitle(tag),
            subtitle = label.name,
            flag = label.flag
        )
    }

@Composable
fun rememberLanguageEntries(): List<LanguageEntry> {
    val context = LocalContext.current
    return remember(context) { context.languageEntries() }
}

@Composable
fun LanguageList(
    entries: List<LanguageEntry>,
    selectedTag: String?,
    onSelected: (LanguageEntry) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues? = null
) {
    val selectedIndex = entries.indexOfFirst { it.tag == selectedTag }
    LazyColumn(
        state = rememberLazyListState(selectedIndex.coerceAtLeast(0)),
        modifier = modifier.fillMaxWidth()
    ) {
        items(entries, key = { it.tag }) { entry ->
            DialogListItemWithRadio(
                title = entry.title,
                subtitle = entry.subtitle,
                isSelected = entry.tag == selectedTag,
                leadingIcon = { LanguageFlag(entry.flag) },
                contentPadding = contentPadding,
                onClick = { onSelected(entry) }
            )
        }
    }
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
