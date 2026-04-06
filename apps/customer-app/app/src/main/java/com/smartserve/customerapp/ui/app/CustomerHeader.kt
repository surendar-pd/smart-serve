package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.smartserve.customerapp.R
import com.smartserve.sharedui.SharedScreenHeader
import com.smartserve.sharedui.SharedStackHeader

/**
 * Tab/root screens: logo + [SharedScreenHeader] with optional trailing action.
 */
@Composable
fun CustomerTabHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = "SmartServe",
            modifier = Modifier.size(38.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.width(10.dp))
        SharedScreenHeader(
            title = title,
            subtitle = subtitle,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/**
 * Pushed/detail screens: back control + [SharedScreenHeader] for consistent title/subtitle typography.
 */
@Composable
fun CustomerStackHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SharedStackHeader(
        title = title,
        subtitle = subtitle,
        onBack = onBack,
        modifier = modifier,
    )
}
