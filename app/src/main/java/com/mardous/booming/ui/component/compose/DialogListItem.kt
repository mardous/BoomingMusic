package com.mardous.booming.ui.component.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mardous.booming.ui.theme.BorderStrokeTokens
import com.mardous.booming.ui.theme.CornerRadiusTokens

@Composable
fun ShapeableDialogListItemSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    useBorderStroke: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shapeCornerRadius by animateDpAsState(
        targetValue = if (isSelected) CornerRadiusTokens.SurfaceLargest else CornerRadiusTokens.SurfaceMedium,
        animationSpec = tween(400)
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) colors.primaryContainer.copy(alpha = .75f) else colors.surfaceContainerHighest,
        animationSpec = tween(200)
    )
    val contentColor = if (isSelected) colors.onPrimaryContainer else colors.onSurface
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected && useBorderStroke) BorderStrokeTokens.Medium else BorderStrokeTokens.None,
        animationSpec = tween(200)
    )
    val borderStroke = remember(isSelected, borderWidth) {
        if (isSelected && useBorderStroke) BorderStroke(borderWidth, colors.primary) else null
    }
    Surface(
        onClick = onClick,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(shapeCornerRadius),
        border = borderStroke,
        content = content,
        modifier = modifier
    )
}

@Composable
fun DialogListItemWithRadio(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    subtitle: String? = null,
    contentPadding: PaddingValues? = null
) {
    DialogListItem(
        title = title,
        leadingComposable = {
            RadioButton(
                selected = isSelected,
                onClick = null
            )
        },
        subtitle = subtitle,
        contentPadding = contentPadding,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun DialogListItemWithCheckBox(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    subtitle: String? = null,
    contentPadding: PaddingValues? = null
) {
    DialogListItem(
        title = title,
        leadingComposable = {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null
            )
        },
        subtitle = subtitle,
        contentPadding = contentPadding,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun DialogListItem(
    title: String,
    leadingComposable: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    contentPadding: PaddingValues? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(contentPadding ?: PaddingValues(horizontal = 24.dp, vertical = 10.dp)),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingComposable.invoke()

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}