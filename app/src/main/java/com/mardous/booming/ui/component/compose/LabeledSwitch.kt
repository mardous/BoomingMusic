package com.mardous.booming.ui.component.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.mardous.booming.R

@Composable
fun LabeledSwitch(
    checked: Boolean,
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
    textStyle: TextStyle = LocalTextStyle.current,
    icon: @Composable (() -> Unit)? = null,
    onStateChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .clickable(
                enabled = enabled,
                role = Role.Switch,
                onClick = {
                    onStateChange(!checked)
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                maxLines = maxLines,
                style = textStyle
            )
            icon?.invoke()
        }

        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = {
                onStateChange(it)
            },
            thumbContent = {
                if (checked) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                } else null
            }
        )
    }
}