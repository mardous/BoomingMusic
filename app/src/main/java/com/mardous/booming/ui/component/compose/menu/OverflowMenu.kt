package com.mardous.booming.ui.component.compose.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mardous.booming.R

@Composable
fun TopAppBarMenu(
    items: List<MenuItem>,
    modifier: Modifier = Modifier,
    showItemIcons: Boolean = false,
    state: MutableState<Boolean> = remember { mutableStateOf(false) }
) {
    var expanded by state

    val actionItems = items.filterIsInstance<MenuItem.Button.Action>().toSet()
    val dropDownItems = (items - actionItems).toSet()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        actionItems.forEach { action ->
            if (action.visible) {
                if (action.icon != null) {
                    IconButton(onClick = action.onClick) {
                        Icon(
                            painter = action.icon,
                            contentDescription = action.text
                        )
                    }
                } else {
                    TextButton(onClick = action.onClick) {
                        Text(
                            text = action.text,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        if (dropDownItems.isNotEmpty()) {
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert_24dp),
                    contentDescription = stringResource(R.string.action_more)
                )
            }

            DropDownMenuInternal(
                items = dropDownItems,
                expanded = expanded,
                showIcons = showItemIcons
            ) { expanded = false }
        }
    }
}

@Composable
fun OverflowMenu(
    items: List<MenuItem>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showItemIcons: Boolean = true,
    icon: Painter = painterResource(R.drawable.ic_more_vert_24dp),
    contentDescription: String? = stringResource(R.string.action_more),
    state: MutableState<Boolean> = remember { mutableStateOf(false) },
) {
    var expanded by state

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = !expanded },
            enabled = enabled
        ) {
            Icon(
                painter = icon,
                contentDescription = contentDescription
            )
        }

        DropDownMenuInternal(
            items = items,
            expanded = expanded,
            showIcons = showItemIcons
        ) { expanded = false }
    }
}

@Composable
private fun DropDownMenuInternal(
    items: Collection<MenuItem>,
    expanded: Boolean,
    showIcons: Boolean,
    onDismissRequest: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        items.forEach {
            if (it.visible) when (it) {
                is MenuItem.Button -> {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = it.text,
                                color = if (it.dangerous) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    Color.Unspecified
                                },
                            )
                        },
                        leadingIcon = if (showIcons) {
                            {
                                if (it.icon != null) {
                                    Icon(
                                        painter = it.icon,
                                        contentDescription = null,
                                        tint = if (it.dangerous) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            LocalContentColor.current
                                        }
                                    )
                                }
                            }
                        } else {
                            null
                        },
                        trailingIcon = {
                            if (it is MenuItem.Button.Checkable) {
                                if (it.isSingleSelection) {
                                    RadioButton(
                                        selected = it.isChecked,
                                        enabled = it.enabled,
                                        onClick = null
                                    )
                                } else {
                                    Checkbox(
                                        checked = it.isChecked,
                                        enabled = it.enabled,
                                        onCheckedChange = null
                                    )
                                }
                            }
                        },
                        onClick = {
                            it.onClick()
                            onDismissRequest()
                        },
                        enabled = it.enabled
                    )
                }
                is MenuItem.Divider -> {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}