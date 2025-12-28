package com.mardous.booming.ui.component.compose.preferences

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mardous.booming.R

@Composable
fun PreferenceScreenItem(
    title: String,
    summary: String,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    HorizontalPreferenceItem(
        title = title,
        summary = summary,
        leadingIcon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
fun ProgressIndicatorPreferenceItem(
    showIndicator: Boolean,
    title: String,
    summary: String? = null,
    iconRes: Int? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    HorizontalPreferenceItem(
        title = title,
        summary = summary,
        leadingIcon = {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        },
        trailingContent = {
            if (showIndicator) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp)
                )
            }
        },
        modifier = modifier,
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
fun SwitchPreferenceItem(
    checked: Boolean,
    title: String,
    summary: String? = null,
    iconRes: Int? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCheckedChanged: (Boolean) -> Unit
) {
    HorizontalPreferenceItem(
        title = title,
        summary = summary,
        leadingIcon = {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                thumbContent = {
                    if (checked) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                },
                onCheckedChange = onCheckedChanged
            )
        },
        modifier = modifier,
        enabled = enabled,
        onClick = {
            onCheckedChanged(!enabled)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderPreferenceItem(
    title: String,
    value: Float,
    valueTo: Float,
    valueFrom: Float = 0f,
    summary: String? = null,
    iconRes: Int? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChanged: (Float) -> Unit
) {
    ExpandableVerticalPreferenceItem(
        title = title,
        summary = summary,
        leadingIcon = {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        },
        bottomContent = {
            Slider(
                enabled = enabled,
                value = value,
                valueRange = valueFrom..valueTo,
                onValueChange = onValueChanged,
                modifier = Modifier.fillMaxWidth()
            )
        },
        modifier = modifier,
        enabled = enabled
    )
}

@Composable
fun HorizontalPreferenceItem(
    title: String,
    summary: String? = null,
    leadingIcon: @Composable BoxScope.() -> Unit = {},
    trailingContent: (@Composable BoxScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    PreferenceItemSurface(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PreferenceItemContent(
                title = title,
                summary = summary,
                leadingIcon = leadingIcon,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            )
            trailingContent?.let { content ->
                Box(
                    modifier = Modifier.wrapContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    content.invoke(this)
                }
            }
        }
    }
}

@Composable
fun VerticalPreferenceItem(
    title: String,
    summary: String? = null,
    leadingIcon: @Composable BoxScope.() -> Unit = {},
    bottomContent: (@Composable BoxScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    PreferenceItemSurface(
        enabled = enabled,
        onClick = { onClick?.invoke() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PreferenceItemContent(
                title = title,
                summary = summary,
                leadingIcon = leadingIcon,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            )
            bottomContent?.let { content ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 56.dp, end = 12.dp)
                ) {
                    content.invoke(this)
                }
            }
        }
    }
}

@Composable
fun ExpandableVerticalPreferenceItem(
    title: String,
    summary: String? = null,
    leadingIcon: @Composable BoxScope.() -> Unit = {},
    bottomContent: (@Composable BoxScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val trailingIconRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    PreferenceItemSurface(
        enabled = enabled,
        onClick = { expanded = !expanded },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PreferenceItemContent(
                title = title,
                summary = summary,
                leadingIcon = leadingIcon,
                trailingIcon = {
                    if (bottomContent != null) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_drop_down_24dp),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.rotate(trailingIconRotation)
                        )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            )
            bottomContent?.let { content ->
                AnimatedVisibility(
                    visible = expanded,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 48.dp, end = 12.dp)
                    ) {
                        content.invoke(this)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceItemContent(
    title: String,
    summary: String? = null,
    leadingIcon: @Composable BoxScope.() -> Unit = {},
    trailingIcon: (@Composable BoxScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            leadingIcon()
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp
            )

            if (!summary.isNullOrEmpty()) {
                Text(
                    text = summary,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        trailingIcon?.let { icon ->
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                icon.invoke(this)
            }
        }
    }
}

@Composable
private fun PreferenceItemSurface(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        content()
    }
}