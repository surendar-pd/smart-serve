package com.smartserve.providerapp.ui.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun greetingDisplayName(): String {
    val user = FirebaseAuth.getInstance().currentUser ?: return "User"
    return user.displayName?.takeIf { it.isNotBlank() }
        ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
        ?: "User"
}
