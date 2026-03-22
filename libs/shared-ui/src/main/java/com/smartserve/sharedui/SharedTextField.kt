package com.smartserve.sharedui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    /** When true, shows a password visibility toggle ([SharedIconButton]) and applies [PasswordVisualTransformation]. Ignores [trailingIcon]. */
    passwordToggleEnabled: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val effectiveVisualTransformation = when {
        passwordToggleEnabled -> if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
        else -> visualTransformation
    }
    val effectiveKeyboardOptions = if (passwordToggleEnabled) {
        keyboardOptions.copy(keyboardType = KeyboardType.Password)
    } else {
        keyboardOptions
    }
    val effectiveTrailingIcon: (@Composable () -> Unit)? = when {
        passwordToggleEnabled -> {
            {
                SharedIconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                )
            }
        }
        else -> trailingIcon
    }

    val labelComposable: (@Composable () -> Unit)? = label?.let { { Text(it) } }
    val placeholderComposable: (@Composable () -> Unit)? = placeholder?.let { { Text(it) } }
    val supportingComposable: (@Composable () -> Unit)? = supportingText?.let {
        { SharedText(text = it, variant = SharedTextVariant.Caption) }
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
            keyboardOptions = effectiveKeyboardOptions,
            visualTransformation = effectiveVisualTransformation,
            leadingIcon = leadingIcon,
            trailingIcon = effectiveTrailingIcon,
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
            keyboardOptions = effectiveKeyboardOptions,
            visualTransformation = effectiveVisualTransformation,
            leadingIcon = leadingIcon,
            trailingIcon = effectiveTrailingIcon,
        )
    }
}

