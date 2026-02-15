package com.mardous.booming.ui.component.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ShapedText(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    color: Color = contentColorFor(containerColor),
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    shape: Shape = MaterialTheme.shapes.small,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .clickable(
                enabled = enabled && onClick != null,
                onClick = { onClick?.invoke() }
            )
            .padding(4.dp)
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TitleShapedText(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    color: Color = contentColorFor(containerColor),
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ShapedText(
        text = text,
        style = MaterialTheme.typography.bodyMediumEmphasized,
        color = color,
        containerColor = containerColor,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    )
}