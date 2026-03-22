package com.smartserve.sharedui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Scrollable column with outer [paddingValues] (e.g. from [SharedScaffold]) plus horizontal/vertical insets.
 */
@Composable
fun SharedPaddedScrollColumn(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 24.dp,
    verticalPadding: Dp = 16.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(paddingValues)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}
