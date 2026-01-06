package com.mardous.booming.ui.component.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

@Composable
fun ConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    message: String,
    confirmButton: String,
    dismissButton: String = stringResource(android.R.string.cancel),
    title: String? = null,
    icon: Painter? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            icon?.let {
                Icon(
                    painter = it,
                    contentDescription = null
                )
            }
        },
        title = {
            title?.let { Text(it) }
        },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmButton)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissButton)
            }
        }
    )
}

@Composable
fun InputDialog(
    onConfirm: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
    message: String,
    confirmButton: String,
    dismissButton: String = stringResource(android.R.string.cancel),
    title: String? = null,
    icon: Painter? = null,
    inputHint: String? = null,
    inputPrefill: String = "",
    inputMaxLength: Int = Int.MAX_VALUE,
    initialChecked: Boolean = false,
    checkBoxPrompt: String? = null
) {
    val inputState = rememberTextFieldState(initialText = inputPrefill)
    var isChecked by remember { mutableStateOf(initialChecked) }

    val inputLength = inputState.text.length
    val isOverMaxLength = inputLength > inputMaxLength
    val isInputValid = !isOverMaxLength && inputState.text.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            icon?.let {
                Icon(
                    painter = it,
                    contentDescription = null
                )
            }
        },
        title = {
            title?.let { Text(it) }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    state = inputState,
                    label = inputHint?.let { { Text(it) } },
                    isError = isOverMaxLength,
                    suffix = {
                        if (inputMaxLength < Int.MAX_VALUE) {
                            Text("$inputLength/$inputMaxLength")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (checkBoxPrompt != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = isChecked,
                                onValueChange = {
                                    isChecked = it
                                }
                            )
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = null
                        )

                        Text(
                            text = checkBoxPrompt,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(inputState.text.toString(), isChecked) },
                enabled = isInputValid
            ) {
                Text(confirmButton)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissButton)
            }
        }
    )
}