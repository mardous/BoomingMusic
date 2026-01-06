package com.mardous.booming.ui.component.compose

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mardous.booming.ui.theme.CornerRadiusTokens

@Composable
fun TitledSurface(
    title: String,
    @DrawableRes iconRes: Int = 0,
    modifier: Modifier = Modifier,
    collapsible: Boolean = false,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    titleEndContent: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val animatedRadius by animateDpAsState(
        targetValue = if (expanded) CornerRadiusTokens.SurfaceSmall else CornerRadiusTokens.SurfaceMedium,
        tween(500, easing = FastOutSlowInEasing)
    )

    LowestSurface(
        color = color,
        cornerRadius = animatedRadius,
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(animatedRadius))
                .clickable(enabled = collapsible, onClick = { expanded = !expanded })
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp)
        ) {
            if (iconRes != 0) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            titleEndContent()
        }

        AnimatedVisibility(
            visible = expanded,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
        ) {
            content()
        }
    }
}

@Composable
fun LowestSurface(
    color: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    cornerRadius: Dp = CornerRadiusTokens.SurfaceSmall,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(color),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}