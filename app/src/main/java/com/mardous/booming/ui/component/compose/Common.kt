/*
 * Copyright (c) 2025 Christians Martínez Alvarado
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

package com.mardous.booming.ui.component.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mardous.booming.R

@Composable
fun LabeledSwitch(
    checked: Boolean,
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
    textStyle: TextStyle = LocalTextStyle.current,
    icon: @Composable (() -> Unit)? = null,
    onStateChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onClick = {
                    onStateChange(!checked)
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                maxLines = maxLines,
                style = textStyle
            )
            icon?.invoke()
        }

        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = {
                onStateChange(it)
            },
            thumbContent = {
                if (checked) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_24dp),
                        contentDescription = null
                    )
                } else null
            }
        )
    }
}

@Composable
fun ActionButton(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}