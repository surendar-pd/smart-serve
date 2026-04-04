package com.smartserve.customerapp.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartserve.sharedui.SharedText
import com.smartserve.sharedui.SharedTextVariant

@Composable
fun PrivacyDataScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        CustomerStackHeader(
            title = "Privacy & Data",
            subtitle = "How C-SmartService protects your information",
            onBack = onBack,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PrivacySection(
                title = "What we collect",
                body = "C-SmartService collects account details, service requests, booking times, and optional location data used for booking fulfillment.",
            )
            PrivacySection(
                title = "How we use it",
                body = "Data is used to match customers and providers, manage active bookings, support in-app chat, and improve reliability and safety.",
            )
            PrivacySection(
                title = "How long we keep data",
                body = "Booking records and service history are retained for operations, quality review, and legal obligations. Non-essential data can be removed upon request when eligible.",
            )
            PrivacySection(
                title = "Your controls",
                body = "You can update profile fields, change notification preferences, and request data export or account deletion through support.",
            )
            PrivacySection(
                title = "Contact",
                body = "For privacy requests, contact C-SmartService support and include your account email for faster verification.",
            )
            Spacer(modifier = Modifier.height(8.dp))
            SharedText(
                text = "Template version: C-SmartService Standard Privacy v1",
                variant = SharedTextVariant.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SharedText(text = title, variant = SharedTextVariant.Subtitle)
        SharedText(
            text = body,
            variant = SharedTextVariant.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
