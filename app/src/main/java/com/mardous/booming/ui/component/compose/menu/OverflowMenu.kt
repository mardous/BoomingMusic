package com.mardous.booming.ui.component.compose.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mardous.booming.R

object MenuDefaults {
    val OverflowIcon: Painter
        @Composable get() = painterResource(R.drawable.ic_more_vert_24dp)

    val OverflowDescription: String
        @Composable get() = stringResource(R.string.action_more)

    @Composable
    fun topAppBarMenuColors(
        containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
        actionContainerColor: Color = Color.Unspecified,
        actionContentColor: Color = contentColorFor(actionContainerColor),
        overflowContainerColor: Color = Color.Unspecified,
        overflowIconColor: Color = contentColorFor(overflowContainerColor)
    ) = MenuColors(
        containerColor = containerColor,
        actionContainerColor = actionContainerColor,
        actionContentColor = actionContentColor,
        overflowContainerColor = overflowContainerColor,
        overflowIconColor = overflowIconColor
    )

    @Composable
    fun dropDownMenuColors(
        containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
        overflowContainerColor: Color = Color.Unspecified,
        overflowIconColor: Color = contentColorFor(overflowContainerColor)
    ) = MenuColors(
        containerColor = containerColor,
        actionContainerColor = Color.Unspecified,
        actionContentColor = Color.Unspecified,
        overflowContainerColor = overflowContainerColor,
        overflowIconColor = overflowIconColor
    )
}

class MenuColors(
    val containerColor: Color,
    val actionContainerColor: Color,
    val actionContentColor: Color,
    val overflowContainerColor: Color,
    val overflowIconColor: Color
)

@Composable
fun TopAppBarMenu(
    items: Collection<MenuItem>,
    modifier: Modifier = Modifier,
    showItemIcons: Boolean = false,
    overflowIcon: Painter = MenuDefaults.OverflowIcon,
    overflowDescription: String = MenuDefaults.OverflowDescription,
    colors: MenuColors = MenuDefaults.topAppBarMenuColors(),
    state: MutableState<Boolean> = remember { mutableStateOf(false) }
) {
    val actionItems = items.filterIsInstance<MenuItem.Button.Action>().toSet()
    val dropDownItems = (items - actionItems).toSet()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        actionItems.forEach { action ->
            if (action.visible) {
                if (action.icon != null) {
                    if (colors.actionContainerColor.isSpecified) {
                        FilledIconButton(
                            onClick = action.onClick,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colors.actionContainerColor,
                                contentColor = colors.actionContentColor
                            )
                        ) {
                            Icon(
                                painter = action.icon,
                                contentDescription = action.text
                            )
                        }
                    } else {
                        IconButton(onClick = action.onClick) {
                            Icon(
                                painter = action.icon,
                                contentDescription = action.text
                            )
                        }
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
            OverflowMenu(
                items = dropDownItems,
                showItemIcons = showItemIcons,
                colors = colors,
                icon = overflowIcon,
                contentDescription = overflowDescription,
                state = state
            )
        }
    }
}

@Composable
fun OverflowMenu(
    items: Collection<MenuItem>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showItemIcons: Boolean = true,
    icon: Painter = MenuDefaults.OverflowIcon,
    contentDescription: String? = MenuDefaults.OverflowDescription,
    colors: MenuColors = MenuDefaults.dropDownMenuColors(),
    state: MutableState<Boolean> = remember { mutableStateOf(false) }
) {
    var expanded by state

    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = modifier
    ) {
        if (colors.overflowContainerColor.isSpecified) {
            FilledIconButton(
                onClick = { expanded = !expanded },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = colors.overflowContainerColor,
                    contentColor = colors.overflowIconColor
                ),
                enabled = enabled
            ) {
                Icon(
                    painter = icon,
                    contentDescription = contentDescription
                )
            }
        } else {
            IconButton(
                onClick = { expanded = !expanded },
                enabled = enabled
            ) {
                Icon(
                    painter = icon,
                    contentDescription = contentDescription
                )
            }
        }

        DropDownMenuInternal(
            items = items,
            expanded = expanded,
            showIcons = showItemIcons,
            onDismissRequest = { expanded = false },
            colors = colors
        )
    }
}

@Composable
private fun DropDownMenuInternal(
    items: Collection<MenuItem>,
    expanded: Boolean,
    showIcons: Boolean,
    onDismissRequest: () -> Unit,
    colors: MenuColors
) {
    DropdownMenu(
        shape = RoundedCornerShape(16.dp),
        containerColor = colors.containerColor,
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
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