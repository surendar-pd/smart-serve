package com.smartserve.providerapp.ui.app

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
        ProviderStackHeader(
            title = "Privacy & Data",
            subtitle = "How C-SmartService handles provider data",
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
                title = "Operational data",
                body = "C-SmartService stores service listings, booking details, availability, ratings, and profile information needed to run marketplace operations.",
            )
            PrivacySection(
                title = "Location and routing",
                body = "Serviceable area and job destination data are used for discovery, scheduling, and navigation support while bookings are active.",
            )
            PrivacySection(
                title = "Notifications",
                body = "Request and schedule notifications are used to help you respond quickly and deliver services on time.",
            )
            PrivacySection(
                title = "Security and retention",
                body = "Data is stored with role-based access controls. Records are retained for service quality, dispute handling, and legal compliance.",
            )
            PrivacySection(
                title = "Provider rights",
                body = "You can request profile corrections and account/data deletion review through C-SmartService support channels.",
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
