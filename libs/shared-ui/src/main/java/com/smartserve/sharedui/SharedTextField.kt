package com.smartserve.sharedui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.VisualTransformation

enum class SharedTextFieldVariant {
    Filled,
    Outlined,
}

@Composable
fun SharedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    variant: SharedTextFieldVariant = SharedTextFieldVariant.Outlined,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val labelComposable: (@Composable () -> Unit)? = label?.let { { Text(it) } }
    val placeholderComposable: (@Composable () -> Unit)? = placeholder?.let { { Text(it) } }
    val supportingComposable: (@Composable () -> Unit)? = supportingText?.let {
        { Text(text = it, style = MaterialTheme.typography.bodySmall) }
    }

    when (variant) {
        SharedTextFieldVariant.Filled -> TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            label = labelComposable,
            placeholder = placeholderComposable,
            supportingText = supportingComposable,
            isError = isError,
            singleLine = singleLine,
            enabled = enabled,
            readOnly = readOnly,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
        )

        SharedTextFieldVariant.Outlined -> OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            label = labelComposable,
            placeholder = placeholderComposable,
            supportingText = supportingComposable,
            isError = isError,
            singleLine = singleLine,
            enabled = enabled,
            readOnly = readOnly,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
        )
    }
}

