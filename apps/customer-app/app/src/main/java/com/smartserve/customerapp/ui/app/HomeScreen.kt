package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
) {
    val greetingName = greetingDisplayName()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        SharedText(
            text = "Hello $greetingName",
            variant = SharedTextVariant.Title,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Home",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun greetingDisplayName(): String {
    val user = FirebaseAuth.getInstance().currentUser ?: return "User"
    return user.displayName?.takeIf { it.isNotBlank() }
        ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
        ?: "User"
}
