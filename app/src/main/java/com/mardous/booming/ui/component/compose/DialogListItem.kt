package com.mardous.booming.ui.component.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.mardous.booming.ui.theme.BorderStrokeTokens
import com.mardous.booming.ui.theme.CornerRadiusTokens

@Composable
fun DialogListItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    leadingIcon: Painter? = null,
    subtitle: String? = null
) {
    DialogListItemSurface(
        onClick = onClick,
        isSelected = isSelected,
        modifier = modifier
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    painter = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = LocalContentColor.current
                )
            }

            if (subtitle.isNullOrEmpty()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DialogListItemWithCheckBox(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    subtitle: String? = null
) {
    DialogListItemSurface(
        onClick = onClick,
        isSelected = isSelected,
        modifier = modifier
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null
            )

            if (subtitle.isNullOrEmpty()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DialogListItemSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shapeCornerRadius by animateDpAsState(
        targetValue = if (isSelected) CornerRadiusTokens.SurfaceLargest else CornerRadiusTokens.SurfaceLarge,
        animationSpec = tween(400)
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) colors.primaryContainer.copy(alpha = .75f) else colors.surfaceContainerHighest,
        animationSpec = tween(200)
    )
    val contentColor = if (isSelected) colors.onPrimaryContainer else colors.onSurface
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) BorderStrokeTokens.Medium else BorderStrokeTokens.None,
        animationSpec = tween(200)
    )
    val borderStroke = remember(isSelected, borderWidth) {
        if (isSelected) BorderStroke(borderWidth, colors.primary) else null
    }
    Surface(
        onClick = onClick,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(shapeCornerRadius),
        border = borderStroke,
        content = content
    )
}