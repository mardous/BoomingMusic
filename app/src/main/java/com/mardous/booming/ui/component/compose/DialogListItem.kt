package com.mardous.booming.ui.component.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DialogListItemWithRadio(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    subtitle: String? = null,
    contentPadding: PaddingValues? = null
) {
    DialogListItem(
        title = title,
        leadingComposable = {
            RadioButton(
                selected = isSelected,
                onClick = null
            )
        },
        subtitle = subtitle,
        contentPadding = contentPadding,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun DialogListItemWithCheckBox(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    subtitle: String? = null,
    contentPadding: PaddingValues? = null
) {
    DialogListItem(
        title = title,
        leadingComposable = {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null
            )
        },
        subtitle = subtitle,
        contentPadding = contentPadding,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun DialogListItem(
    title: String,
    leadingComposable: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    contentPadding: PaddingValues? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(contentPadding ?: PaddingValues(horizontal = 24.dp, vertical = 10.dp)),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingComposable.invoke()

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}