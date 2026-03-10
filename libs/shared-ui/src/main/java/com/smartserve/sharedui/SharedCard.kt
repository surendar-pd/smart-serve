package com.smartserve.sharedui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CardColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SharedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    content: @Composable () -> Unit,
) {
    if (onClick != null) {
        Card(
            modifier = modifier,
            onClick = onClick,
            colors = colors,
            elevation = elevation,
        ) {
            Column(Modifier.padding(contentPadding)) { content() }
        }
    } else {
        Card(
            modifier = modifier,
            colors = colors,
            elevation = elevation,
        ) {
            Column(Modifier.padding(contentPadding)) { content() }
        }
    }
}

