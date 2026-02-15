package com.mardous.booming.ui.component.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    checkBoxPrompt: String,
    confirmButton: String,
    dismissButton: String = stringResource(android.R.string.cancel),
    title: String? = null,
    icon: Painter? = null,
    inputHint: String? = null,
    inputPrefill: String = "",
    inputMaxLength: Int = Int.MAX_VALUE,
    initialChecked: Boolean = false,
) {
    var isChecked by remember { mutableStateOf(initialChecked) }

    InputDialog(
        onConfirm = { onConfirm(it, isChecked) },
        onDismiss = onDismiss,
        message = message,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        title = title,
        icon = icon,
        inputHint = inputHint,
        inputPrefill = inputPrefill,
        inputMaxLength = inputMaxLength,
        additionalContent = {
            DialogCheckBox(
                text = checkBoxPrompt,
                isChecked = isChecked,
                onValueChange = { isChecked = it }
            )
        }
    )
}

@Composable
fun InputDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    message: String,
    confirmButton: String,
    dismissButton: String = stringResource(android.R.string.cancel),
    title: String? = null,
    icon: Painter? = null,
    inputHint: String? = null,
    inputPrefill: String = "",
    inputMaxLength: Int = Int.MAX_VALUE,
    additionalContent: @Composable (ColumnScope.() -> Unit)? = null
) {
    val inputState = rememberTextFieldState(initialText = inputPrefill)

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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = message,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

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

                Spacer(Modifier.height(8.dp))

                additionalContent?.invoke(this)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(inputState.text.toString()) },
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