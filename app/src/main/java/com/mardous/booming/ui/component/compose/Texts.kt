package com.mardous.booming.ui.component.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun TitleText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.titleLarge
) {
    Text(
        text = text,
        maxLines = maxLines,
        overflow = overflow,
        color = color,
        style = style,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@Composable
fun TitledSection(
    text: String,
    modifier: Modifier = Modifier,
    titleEndContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TitleText(
                text = text,
                modifier = Modifier.weight(1f)
            )

            if (titleEndContent != null) {
                titleEndContent()
            }
        }
        content()
    }
}

@Composable
fun TitleEndText(text: String, onClick: (() -> Unit)? = null) {
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