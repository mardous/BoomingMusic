/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SwitchCard(
    onCheckedChange: (Boolean) -> Unit,
    checked: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    style: TextStyle? = null,
    color: Color? = null,
    enabled: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    TitledCard(
        onTitleClick = { onCheckedChange(!checked) },
        expanded = checked,
        collapsible = enabled,
        title = title,
        style = style,
        titleEndContent = {
            Switch(
                checked = checked,
                onCheckedChange = null
            )
        },
        icon = icon,
        color = color,
        modifier = modifier,
        content = content
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TitledCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    style: TextStyle? = null,
    color: Color? = null,
    collapsible: Boolean = false,
    titleEndContent: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    TitledCard(
        onTitleClick = { expanded = !expanded },
        expanded = expanded,
        collapsible = collapsible,
        title = title,
        titleEndContent = titleEndContent,
        style = style,
        icon = icon,
        color = color,
        modifier = modifier,
        content = content
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TitledCard(
    onTitleClick: () -> Unit,
    expanded: Boolean,
    collapsible: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    style: TextStyle? = null,
    color: Color? = null,
    titleEndContent: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = color ?: MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = collapsible, onClick = onTitleClick)
                .padding(16.dp)
        ) {
            if (icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(Modifier.width(8.dp))
            }

            Text(
                text = title,
                style = style ?: MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(16.dp))

            titleEndContent()
        }

        AnimatedVisibility(visible = expanded) {
            content(PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp))
        }
    }
}