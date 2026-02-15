package com.mardous.booming.ui.component.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TonalToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ButtonGroup(
    onSelected: (T) -> Unit,
    buttonItems: List<T>,
    buttonStateResolver: (T) -> Boolean,
    buttonIconResolver: @Composable (T, Boolean) -> Painter? = { _, _ -> null },
    buttonTextResolver: @Composable (T) -> String = { it.toString() },
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        buttonItems.forEachIndexed { index, item ->
            val isChecked = buttonStateResolver(item)
            val buttonWeight = if (isChecked) 1.5f else 1f
            TonalToggleButton(
                checked = isChecked,
                onCheckedChange = { onSelected(item) },
                modifier = Modifier
                    .weight(buttonWeight)
                    .semantics { role = Role.RadioButton },
                shapes =
                    when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        buttonItems.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
            ) {
                buttonIconResolver(item, isChecked)?.let {
                    Icon(
                        painter = it,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                }
                Text(
                    text = buttonTextResolver(item),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}