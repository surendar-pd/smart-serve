package com.smartserve.customerapp.ui.layouts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AuthLayout(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(24.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .padding(contentPadding),
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(24.dp),
                content = content,
            )
        }
    }
}

