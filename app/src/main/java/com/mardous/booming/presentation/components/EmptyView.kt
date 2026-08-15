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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object EmptyViewDefaults {
    val IconSize = 48.dp

    @Composable
    fun defaultColors(
        iconColor: Color = MaterialTheme.colorScheme.secondary,
        iconContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleColor: Color = MaterialTheme.colorScheme.onSurface,
        textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
    ): EmptyViewColors {
        return EmptyViewColors(
            iconColor = iconColor,
            iconContainerColor = iconContainerColor,
            titleColor = titleColor,
            textColor = textColor
        )
    }
}

data class EmptyViewColors(
    val iconColor: Color,
    val iconContainerColor: Color,
    val titleColor: Color,
    val textColor: Color
)

@Composable
fun EmptyView(
    icon: Painter,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    colors: EmptyViewColors = EmptyViewDefaults.defaultColors(),
    iconSize: Dp = EmptyViewDefaults.IconSize,
    button: @Composable () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.iconContainerColor)
                .padding(32.dp)
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = colors.iconColor,
                modifier = Modifier.size(iconSize)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = title,
            textAlign = TextAlign.Center,
            color = colors.titleColor,
            style = MaterialTheme.typography.titleLarge
        )

        if (!subtitle.isNullOrEmpty()) {
            Text(
                text = subtitle,
                textAlign = TextAlign.Center,
                color = colors.textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(16.dp))

        button()
    }
}