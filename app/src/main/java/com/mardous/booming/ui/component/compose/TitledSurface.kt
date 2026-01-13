package com.mardous.booming.ui.component.compose

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SwitchCard(
    onCheckedChange: (Boolean) -> Unit,
    checked: Boolean,
    title: String,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    @DrawableRes iconRes: Int = 0,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
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
        iconRes = iconRes,
        modifier = modifier,
        content = content
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TitledCard(
    title: String,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    @DrawableRes iconRes: Int = 0,
    modifier: Modifier = Modifier,
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
        iconRes = iconRes,
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
    style: TextStyle = MaterialTheme.typography.titleMedium,
    @DrawableRes iconRes: Int? = null,
    modifier: Modifier = Modifier,
    titleEndContent: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = collapsible, onClick = onTitleClick)
                .padding(16.dp)
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = title,
                style = style,
                modifier = Modifier.weight(1f)
            )

            titleEndContent()
        }

        AnimatedVisibility(visible = expanded) {
            content(PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp))
        }
    }
}