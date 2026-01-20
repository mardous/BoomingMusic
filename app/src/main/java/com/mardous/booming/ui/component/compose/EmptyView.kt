package com.mardous.booming.ui.component.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object EmptyViewDefaults {
    val IconSize = 72.dp
    val TextSize = 20.sp

    @Composable
    fun defaultColors(
        iconColor: Color = MaterialTheme.colorScheme.secondary,
        iconContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
        textColor: Color = MaterialTheme.colorScheme.onSurface
    ): EmptyViewColors {
        return EmptyViewColors(
            iconColor = iconColor,
            iconContainerColor = iconContainerColor,
            textColor = textColor
        )
    }
}

class EmptyViewColors(
    val iconColor: Color,
    val iconContainerColor: Color,
    val textColor: Color,
)

@Composable
fun EmptyView(
    icon: Painter,
    text: String,
    modifier: Modifier = Modifier,
    colors: EmptyViewColors = EmptyViewDefaults.defaultColors(),
    iconSize: Dp = EmptyViewDefaults.IconSize,
    textSize: TextUnit = EmptyViewDefaults.TextSize,
    button: @Composable () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(32.dp)
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = colors.iconColor,
            modifier = Modifier.size(iconSize)
        )

        Text(
            text = text,
            textAlign = TextAlign.Center,
            color = colors.textColor,
            style = MaterialTheme.typography.headlineSmall,
            fontSize = textSize
        )

        button()
    }
}