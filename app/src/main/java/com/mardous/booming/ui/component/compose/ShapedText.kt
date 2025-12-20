package com.mardous.booming.ui.component.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun ShapedText(
    text: String,
    style: TextStyle = LocalTextStyle.current,
    textColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
    shape: Shape = RoundedCornerShape(50),
    shapeColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = textColor,
        style = style,
        modifier = modifier
            .clip(shape)
            .background(shapeColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun TitleShapedText(text: String, onClick: (() -> Unit)? = null) {
    ShapedText(
        text = text,
        textColor = MaterialTheme.colorScheme.onPrimaryContainer,
        style = MaterialTheme.typography.bodySmall,
        shapeColor = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.clickable(
            enabled = onClick != null,
            onClick = { onClick?.invoke() }
        )
    )
}